package com.agentboard.docs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Placeholder OpenAPI bean so springdoc registers Swagger UI routes. */
@Configuration
public class OpenApiConfig {

  /** Minimal metadata; actual specs are proxied from upstream services. */
  @Bean
  public OpenAPI aggregatorOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("AgentBoard API Documentation")
            .version("0.0.1-SNAPSHOT")
            .description("Unified Swagger UI for auth-service and board-service."));
  }
}
