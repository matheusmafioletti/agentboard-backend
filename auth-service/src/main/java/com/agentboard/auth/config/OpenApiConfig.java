package com.agentboard.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata and server URL for auth-service API documentation. */
@Configuration
public class OpenApiConfig {

  private final String serverUrl;
  private final String appVersion;

  /** Creates the config with the public server URL and application version. */
  public OpenApiConfig(
      @Value("${openapi.server-url:http://localhost:8080}") String serverUrl,
      @Value("${project.version:0.0.1-SNAPSHOT}") String appVersion) {
    this.serverUrl = serverUrl;
    this.appVersion = appVersion;
  }

  /** Defines service metadata exposed at {@code /v3/api-docs}. */
  @Bean
  public OpenAPI authOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("AgentBoard — Auth Service")
            .version(appVersion)
            .description("Tenant registration, login, and password management."))
        .servers(List.of(new Server().url(serverUrl).description("Auth Service")));
  }

  /** Keeps Try it out requests targeting the auth-service base URL. */
  @Bean
  public OpenApiCustomizer authServerCustomizer() {
    return openApi -> openApi.setServers(
        List.of(new Server().url(serverUrl).description("Auth Service")));
  }
}
