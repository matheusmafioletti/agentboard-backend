package com.agentboard.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Integration tests for POST /auth/register. */
class RegisterIntegrationTest extends AbstractAuthIntegrationTest {

  @Autowired
  private org.springframework.test.web.servlet.MockMvc mockMvc;

  @Test
  void register_createsAdminMembership() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType("application/json")
            .content("""
                {
                  "name": "Alice",
                  "email": "alice@example.com",
                  "password": "secret123",
                  "tenantName": "Alice Workspace"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.tenantName").value("Alice Workspace"))
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.board").doesNotExist());
  }

  @Test
  void register_duplicateEmail_returns409() throws Exception {
    String body = """
        {
          "name": "Bob",
          "email": "dup@example.com",
          "password": "secret123",
          "tenantName": "Workspace A"
        }
        """;
    mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_REGISTERED"));
  }
}
