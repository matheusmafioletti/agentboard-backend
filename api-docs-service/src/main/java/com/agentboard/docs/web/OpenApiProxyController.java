package com.agentboard.docs.web;

import com.agentboard.docs.config.OpenApiAggregatorProperties;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Proxies upstream OpenAPI JSON documents through a same-origin path for Swagger UI. */
@RestController
public class OpenApiProxyController {

  private final OpenApiAggregatorProperties properties;
  private final RestTemplate restTemplate;

  /** Creates the controller with upstream URLs and an HTTP client. */
  public OpenApiProxyController(
      OpenApiAggregatorProperties properties,
      RestTemplate restTemplate) {
    this.properties = properties;
    this.restTemplate = restTemplate;
  }

  /**
   * Fetches the OpenAPI document for the given service slug.
   *
   * @param service upstream key such as {@code auth-service} or {@code board-service}
   */
  @GetMapping(value = "/openapi/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> proxyOpenApi(@PathVariable String service) {
    String upstreamUrl = resolveUpstreamUrl(service);
    try {
      String body = restTemplate.getForObject(upstreamUrl, String.class);
      if (body == null || body.isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "Upstream OpenAPI document for '" + service + "' was empty");
      }
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(body);
    } catch (RestClientException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Failed to fetch OpenAPI document for '" + service
              + "' from " + upstreamUrl + ": " + ex.getMessage(),
          ex);
    }
  }

  private String resolveUpstreamUrl(String service) {
    Map<String, String> upstream = properties.getUpstream();
    String url = upstream.get(service);
    if (url == null || url.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Unknown OpenAPI service '" + service + "'. Known services: " + upstream.keySet());
    }
    return url;
  }
}
