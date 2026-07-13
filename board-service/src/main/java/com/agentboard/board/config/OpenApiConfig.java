package com.agentboard.board.config;

import com.agentboard.commons.security.DataSourceHeaderFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata, security schemes, and server URL for board-service API documentation. */
@Configuration
public class OpenApiConfig {

  public static final String BEARER_JWT = "bearerJwt";
  public static final String TENANT_API_KEY = "tenantApiKey";
  public static final String PROJECT_API_KEY = "projectApiKey";

  private final String serverUrl;
  private final String appVersion;

  /** Creates the config with the public server URL and application version. */
  public OpenApiConfig(
      @Value("${openapi.server-url:http://localhost:8081}") String serverUrl,
      @Value("${project.version:0.0.1-SNAPSHOT}") String appVersion) {
    this.serverUrl = serverUrl;
    this.appVersion = appVersion;
  }

  /** Defines service metadata and authentication schemes exposed at {@code /v3/api-docs}. */
  @Bean
  public OpenAPI boardOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("AgentBoard — Board Service")
            .version(appVersion)
            .description("Projects, work items, and board state management."))
        .servers(List.of(new Server().url(serverUrl).description("Board Service")))
        .components(new Components()
            .addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Browser UI clients — Authorization: Bearer <jwt>"))
            .addSecuritySchemes(TENANT_API_KEY, new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("Legacy MCP tenant-scoped integration"))
            .addSecuritySchemes(PROJECT_API_KEY, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .description("MCP project scope — Authorization: Bearer agb_<key>")));
  }

  /** Keeps Try it out requests targeting the board-service base URL. */
  @Bean
  public OpenApiCustomizer boardServerCustomizer() {
    return openApi -> openApi.setServers(
        List.of(new Server().url(serverUrl).description("Board Service")));
  }

  /** Documents optional {@code X-Data-Source} on all operations. */
  @Bean
  public OpenApiCustomizer dataSourceHeaderCustomizer() {
    Parameter header = new Parameter()
        .in("header")
        .name(DataSourceHeaderFilter.HEADER_NAME)
        .required(false)
        .description("Optional data provenance tag: manual (default), automation, or seed")
        .schema(new StringSchema()._enum(List.of("manual", "automation", "seed")));
    return openApi -> openApi.getPaths().values().forEach(path ->
        path.readOperations().forEach(operation -> operation.addParametersItem(header)));
  }
}
