package com.agentboard.docs.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.agentboard.docs.config.OpenApiAggregatorProperties;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OpenApiProxyControllerTest {

  private static final String SERVICE = "auth-service";
  private static final String UPSTREAM_URL = "http://auth-service:8080/v3/api-docs";

  @Mock
  private OpenApiAggregatorProperties properties;
  @Mock
  private RestTemplate restTemplate;

  private OpenApiProxyController controller;

  @BeforeEach
  void setUp() {
    controller = new OpenApiProxyController(properties, restTemplate);
  }

  @Test
  void shouldReturnOk_whenUpstreamReturnsValidBody() {
    String body = "{\"openapi\":\"3.0.1\"}";
    when(properties.getUpstream()).thenReturn(Map.of(SERVICE, UPSTREAM_URL));
    when(restTemplate.getForObject(UPSTREAM_URL, String.class)).thenReturn(body);

    ResponseEntity<String> response = controller.proxyOpenApi(SERVICE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(body);
  }

  @Test
  void shouldThrowNotFound_whenServiceIsUnknown() {
    when(properties.getUpstream()).thenReturn(Map.of("board-service", UPSTREAM_URL));

    assertThatThrownBy(() -> controller.proxyOpenApi("unknown-service"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void shouldThrowNotFound_whenServiceUrlIsBlank() {
    when(properties.getUpstream()).thenReturn(Map.of(SERVICE, "  "));

    assertThatThrownBy(() -> controller.proxyOpenApi(SERVICE))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void shouldThrowBadGateway_whenUpstreamBodyIsNull() {
    when(properties.getUpstream()).thenReturn(Map.of(SERVICE, UPSTREAM_URL));
    when(restTemplate.getForObject(UPSTREAM_URL, String.class)).thenReturn(null);

    assertThatThrownBy(() -> controller.proxyOpenApi(SERVICE))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.BAD_GATEWAY));
  }

  @Test
  void shouldThrowBadGateway_whenUpstreamBodyIsBlank() {
    when(properties.getUpstream()).thenReturn(Map.of(SERVICE, UPSTREAM_URL));
    when(restTemplate.getForObject(UPSTREAM_URL, String.class)).thenReturn("   ");

    assertThatThrownBy(() -> controller.proxyOpenApi(SERVICE))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.BAD_GATEWAY));
  }

  @Test
  void shouldThrowBadGateway_whenRestClientExceptionThrown() {
    when(properties.getUpstream()).thenReturn(Map.of(SERVICE, UPSTREAM_URL));
    when(restTemplate.getForObject(UPSTREAM_URL, String.class))
        .thenThrow(new ResourceAccessException("Connection refused"));

    assertThatThrownBy(() -> controller.proxyOpenApi(SERVICE))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.BAD_GATEWAY));
  }
}
