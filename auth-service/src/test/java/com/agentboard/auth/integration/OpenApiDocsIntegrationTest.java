package com.agentboard.auth.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies the auth-service OpenAPI document is publicly accessible. */
class OpenApiDocsIntegrationTest extends AbstractAuthIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void openApiDocs_isPublicAndListsAuthEndpoints() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("/auth/login")))
        .andExpect(content().string(containsString("Auth Service")));
  }
}
