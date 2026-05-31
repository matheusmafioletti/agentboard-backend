package com.agentboard.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Integration tests for login and tenant selection. */
class LoginTenantSelectionIntegrationTest extends AbstractAuthIntegrationTest {

  @Autowired
  private org.springframework.test.web.servlet.MockMvc mockMvc;

  @Test
  void login_singleTenant_returnsSession() throws Exception {
    registerUser("solo@example.com", "Solo WS");
    mockMvc.perform(post("/auth/login")
            .contentType("application/json")
            .content("""
                {"email":"solo@example.com","password":"secret123"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.tenantName").value("Solo WS"))
        .andExpect(jsonPath("$.requiresTenantSelection").doesNotExist());
  }

  @Test
  void login_multiTenant_requiresSelection() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    registerUser("multi-" + suffix + "@example.com", "First WS " + suffix);
    String email = "multi-" + suffix + "@example.com";
    String token = loginAndGetToken(email);
    mockMvc.perform(post("/auth/tenants")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content("{\"tenantName\":\"Second WS " + suffix + "\"}"))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/auth/login")
            .contentType("application/json")
            .content("""
                {"email":"%s","password":"secret123"}
                """.formatted(email)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresTenantSelection").value(true))
        .andExpect(jsonPath("$.memberships.length()").value(2));
  }

  @Test
  void switchTenant_issuesNewJwtForMember() throws Exception {
    var firstReg = mockMvc.perform(post("/auth/register")
            .contentType("application/json")
            .content("""
                {
                  "name": "Switcher",
                  "email": "switch@example.com",
                  "password": "secret123",
                  "tenantName": "Alpha Switch"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn();
    String alphaTenantId = firstReg.getResponse().getContentAsString()
        .split("\"tenantId\":\"")[1].split("\"")[0];

    String token = loginAndGetToken("switch@example.com");
    var secondTenant = mockMvc.perform(post("/auth/tenants")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content("{\"tenantName\":\"Beta Switch\"}"))
        .andExpect(status().isCreated())
        .andReturn();
    String betaTenantId = secondTenant.getResponse().getContentAsString()
        .split("\"tenantId\":\"")[1].split("\"")[0];

    mockMvc.perform(post("/auth/switch-tenant")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content("{\"tenantId\":\"" + alphaTenantId + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantName").value("Alpha Switch"))
        .andExpect(jsonPath("$.token").isNotEmpty());

    String newToken = mockMvc.perform(post("/auth/switch-tenant")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content("{\"tenantId\":\"" + betaTenantId + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantName").value("Beta Switch"))
        .andReturn()
        .getResponse()
        .getContentAsString()
        .split("\"token\":\"")[1]
        .split("\"")[0];

    mockMvc.perform(get("/auth/me/memberships")
            .header("Authorization", "Bearer " + newToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.memberships.length()").value(2));
  }

  @Test
  void selectTenant_issuesJwtForChosenWorkspace() throws Exception {
    var firstReg = mockMvc.perform(post("/auth/register")
            .contentType("application/json")
            .content("""
                {
                  "name": "Picker",
                  "email": "pick@example.com",
                  "password": "secret123",
                  "tenantName": "Alpha"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn();
    String alphaTenantId = firstReg.getResponse().getContentAsString()
        .split("\"tenantId\":\"")[1].split("\"")[0];

    String token = loginAndGetToken("pick@example.com");
    mockMvc.perform(post("/auth/tenants")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content("{\"tenantName\":\"Beta\"}"))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/auth/select-tenant")
            .contentType("application/json")
            .content("""
                {
                  "email":"pick@example.com",
                  "password":"secret123",
                  "tenantId":"%s"
                }
                """.formatted(alphaTenantId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  private void registerUser(String email, String tenantName) throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType("application/json")
            .content("""
                {
                  "name": "User",
                  "email": "%s",
                  "password": "secret123",
                  "tenantName": "%s"
                }
                """.formatted(email, tenantName)))
        .andExpect(status().isCreated());
  }

  private String loginAndGetToken(String email) throws Exception {
    var result = mockMvc.perform(post("/auth/login")
            .contentType("application/json")
            .content("""
                {"email":"%s","password":"secret123"}
                """.formatted(email)))
        .andExpect(status().isOk())
        .andReturn();
    String body = result.getResponse().getContentAsString();
    return body.split("\"token\":\"")[1].split("\"")[0];
  }
}
