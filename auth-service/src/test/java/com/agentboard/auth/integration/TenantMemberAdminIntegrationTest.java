package com.agentboard.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Integration tests for tenant member admin endpoints. */
class TenantMemberAdminIntegrationTest extends AbstractAuthIntegrationTest {

  @Autowired
  private org.springframework.test.web.servlet.MockMvc mockMvc;

  @Test
  void adminListsMembers() throws Exception {
    var reg = mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Admin",
                  "email": "admin-members@example.com",
                  "password": "secret123",
                  "tenantName": "Members WS"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn();

    String body = reg.getResponse().getContentAsString();
    String token = body.split("\"token\":\"")[1].split("\"")[0];
    String tenantId = body.split("\"tenantId\":\"")[1].split("\"")[0];

    mockMvc.perform(get("/auth/tenants/" + tenantId + "/members")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members.length()").value(1));
  }

  @Test
  void nonAdminCannotListMembers() throws Exception {
    var adminReg = mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Admin2",
                  "email": "admin2-members@example.com",
                  "password": "secret123",
                  "tenantName": "Members WS2"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn();

    String adminBody = adminReg.getResponse().getContentAsString();
    String adminToken = adminBody.split("\"token\":\"")[1].split("\"")[0];
    String tenantId = adminBody.split("\"tenantId\":\"")[1].split("\"")[0];

    var invite = mockMvc.perform(post("/auth/tenants/" + tenantId + "/invites")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"user-members@example.com\"}"))
        .andExpect(status().isCreated())
        .andReturn();

    String inviteUrl = invite.getResponse().getContentAsString()
        .split("\"inviteUrl\":\"")[1].split("\"")[0];
    String rawToken = inviteUrl.substring(inviteUrl.lastIndexOf('/') + 1);

    var accept = mockMvc.perform(post("/auth/invites/" + rawToken + "/accept")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"User","password":"secret456"}
                """))
        .andExpect(status().isOk())
        .andReturn();

    String userToken = accept.getResponse().getContentAsString()
        .split("\"token\":\"")[1].split("\"")[0];

    mockMvc.perform(get("/auth/tenants/" + tenantId + "/members")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());
  }
}
