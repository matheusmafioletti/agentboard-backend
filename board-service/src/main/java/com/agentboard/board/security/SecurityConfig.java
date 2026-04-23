package com.agentboard.board.security;

import com.agentboard.board.repository.TenantApiKeyRepository;
import com.agentboard.commons.security.JwtValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for board-service supporting both JWT and API key authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final String jwtSecret;
  private final TenantApiKeyRepository tenantApiKeyRepository;

  /**
   * Creates the config with the JWT signing secret and API key repository.
   */
  public SecurityConfig(
      @Value("${jwt.secret}") String jwtSecret,
      TenantApiKeyRepository tenantApiKeyRepository) {
    this.jwtSecret = jwtSecret;
    this.tenantApiKeyRepository = tenantApiKeyRepository;
  }

  /**
   * Configures the security filter chain.
   *
   * <ul>
   *   <li>{@code /internal/**} — no auth required (internal service-to-service calls)</li>
   *   <li>{@code /ws/**} — no HTTP authentication (STOMP handles auth on connect)</li>
   *   <li>{@code /api/**} — JWT or API key required</li>
   * </ul>
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    JwtValidator jwtValidator = new JwtValidator(jwtSecret);
    ApiKeyFilter apiKeyFilter = new ApiKeyFilter(tenantApiKeyRepository);
    JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtValidator);

    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
}
