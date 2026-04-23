package com.agentboard.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

/** Application-level bean definitions for auth-service. */
@Configuration
public class AppConfig {

  /**
   * Creates a BCrypt password encoder for hashing user passwords.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Creates a default {@link RestTemplate} for outbound HTTP calls.
   */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
