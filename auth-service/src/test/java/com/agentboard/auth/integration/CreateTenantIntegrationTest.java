package com.agentboard.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Integration tests for POST /auth/tenants. */
class CreateTenantIntegrationTest extends AbstractAuthIntegrationTest {

  @Autowired
  private org.springframework.test.web.servlet.MockMvc mockMvc;

  @Test
  void createTenant_returnsSessionForNewWorkspace() throws Exception {
    var reg = mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Creator",
                  "email": "creator@example.com",
                  "password": "secret123",
                  "tenantName": "First WS"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn();

    String token = reg.getResponse().getContentAsString()
        .split("\"token\":\"")[1].split("\"")[0];

    mockMvc.perform(post("/auth/tenants")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tenantName\":\"Second WS\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.session.tenantName").value("Second WS"))
        .andExpect(jsonPath("$.session.role").value("ADMIN"))
        .andExpect(jsonPath("$.board").doesNotExist());
  }
}
