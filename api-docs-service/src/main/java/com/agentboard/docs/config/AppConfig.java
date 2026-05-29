package com.agentboard.docs.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** Shared HTTP client beans for upstream OpenAPI proxying. */
@Configuration
public class AppConfig {

  /** Creates a RestTemplate with a short timeout for upstream OpenAPI fetches. */
  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(5))
        .setReadTimeout(Duration.ofSeconds(5))
        .build();
  }
}
