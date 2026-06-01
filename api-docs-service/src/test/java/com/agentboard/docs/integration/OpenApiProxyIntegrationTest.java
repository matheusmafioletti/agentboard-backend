package com.agentboard.docs.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies proxied OpenAPI documents and Swagger UI availability. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiProxyIntegrationTest {

  private static final String AUTH_SPEC =
      "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Auth\"},"
          + "\"paths\":{\"/auth/login\":{\"post\":{}}}}";

  private static final String BOARD_SPEC =
      "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Board\"},"
          + "\"paths\":{\"/api/v1/work-items\":{\"get\":{}}}}";

  private static final WireMockServer WIRE_MOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    WIRE_MOCK.start();
    configureFor(WIRE_MOCK.port());
  }

  @Autowired
  private MockMvc mockMvc;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "agentboard.openapi.upstream.auth-service",
        () -> WIRE_MOCK.baseUrl() + "/auth/v3/api-docs");
    registry.add(
        "agentboard.openapi.upstream.board-service",
        () -> WIRE_MOCK.baseUrl() + "/board/v3/api-docs");
  }

  @BeforeEach
  void stubUpstreamSpecs() {
    WIRE_MOCK.resetAll();
    configureFor(WIRE_MOCK.port());
    stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/auth/v3/api-docs"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(AUTH_SPEC)));
    stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/board/v3/api-docs"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(BOARD_SPEC)));
  }

  @Test
  void proxyAuthService_returnsUpstreamSpec() throws Exception {
    mockMvc.perform(get("/api/openapi/auth-service"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("/auth/login")));
  }

  @Test
  void proxyBoardService_returnsUpstreamSpec() throws Exception {
    mockMvc.perform(get("/api/openapi/board-service"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("/api/v1/work-items")));
  }

  @Test
  void swaggerUi_indexPageIsAccessible() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk());
  }
}
