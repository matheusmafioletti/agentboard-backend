package com.agentboard.board.security;

import com.agentboard.board.repository.ProjectRepository;
import com.agentboard.board.repository.TenantApiKeyRepository;
import com.agentboard.commons.security.JwtValidator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration for board-service.
 *
 * <p>Supports three authentication paths:
 * <ol>
 *   <li>Project API key ({@code Authorization: Bearer agb_...}) — MCP server clients</li>
 *   <li>Tenant API key ({@code X-API-Key}) — legacy MCP tool integration</li>
 *   <li>JWT ({@code Authorization: Bearer <jwt>}) — browser UI clients</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final String jwtSecret;
  private final TenantApiKeyRepository tenantApiKeyRepository;
  private final ProjectRepository projectRepository;
  private final String allowedOrigins;

  /**
   * Creates the config with the JWT secret, tenant API key repo, and project repo.
   */
  public SecurityConfig(
      @Value("${jwt.secret}") String jwtSecret,
      TenantApiKeyRepository tenantApiKeyRepository,
      ProjectRepository projectRepository,
      @Value("${cors.allowed-origins:http://localhost:3010}") String allowedOrigins) {
    this.jwtSecret = jwtSecret;
    this.tenantApiKeyRepository = tenantApiKeyRepository;
    this.projectRepository = projectRepository;
    this.allowedOrigins = allowedOrigins;
  }

  /**
   * Configures the security filter chain.
   *
   * <ul>
   *   <li>{@code /internal/**} — no auth required (internal service-to-service calls)</li>
   *   <li>{@code /ws/**} — no HTTP authentication (STOMP handles auth on connect)</li>
   *   <li>{@code /api/**} — JWT, tenant API key, or project API key required</li>
   * </ul>
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    JwtValidator jwtValidator = new JwtValidator(jwtSecret);
    ProjectApiKeyFilter projectApiKeyFilter = new ProjectApiKeyFilter(projectRepository);
    ApiKeyFilter apiKeyFilter = new ApiKeyFilter(tenantApiKeyRepository);
    JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtValidator);

    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(projectApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/internal/**").permitAll()
            .requestMatchers("/ws/**").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().authenticated()
        )
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
