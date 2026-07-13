package com.agentboard.board.config;

import com.agentboard.commons.policy.DataSourcePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Application-level bean definitions for board-service. */
@Configuration
public class AppConfig {

  /** Shared data-source validation rules for board write flows. */
  @Bean
  public DataSourcePolicy dataSourcePolicy() {
    return new DataSourcePolicy();
  }
}
