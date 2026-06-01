package com.agentboard.docs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Permits public access to Swagger UI and proxied OpenAPI documents only. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** Configures a stateless filter chain that exposes documentation endpoints only. */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api/openapi/**",
                "/v3/api-docs/**",
                "/webjars/**"
            ).permitAll()
            .anyRequest().denyAll()
        )
        .build();
  }
}
