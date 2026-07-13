package com.agentboard.auth.config;

import com.agentboard.commons.policy.DataSourcePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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

  /** Shared data-source validation rules for auth flows. */
  @Bean
  public DataSourcePolicy dataSourcePolicy() {
    return new DataSourcePolicy();
  }
}
