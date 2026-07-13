package com.agentboard.auth.config;

import com.agentboard.auth.security.JwtAuthFilter;
import com.agentboard.commons.security.DataSourceHeaderFilter;
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

/** Spring Security configuration for auth-service. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final String allowedOrigins;
  private final String jwtSecret;

  /** Creates the config with CORS and JWT settings. */
  public SecurityConfig(
      @Value("${cors.allowed-origins:http://localhost:3010}") String allowedOrigins,
      @Value("${jwt.secret}") String jwtSecret) {
    this.allowedOrigins = allowedOrigins;
    this.jwtSecret = jwtSecret;
  }

  /** Configures public auth routes, JWT-protected routes, and the JWT filter. */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    DataSourceHeaderFilter dataSourceHeaderFilter = new DataSourceHeaderFilter();
    JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(new JwtValidator(jwtSecret));

    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/v3/api-docs/**").permitAll()
            .requestMatchers(
                "/auth/register",
                "/auth/login",
                "/auth/select-tenant",
                "/auth/change-password",
                "/auth/invites/**"
            ).permitAll()
            .requestMatchers("/auth/**").authenticated()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(dataSourceHeaderFilter, JwtAuthFilter.class)
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
