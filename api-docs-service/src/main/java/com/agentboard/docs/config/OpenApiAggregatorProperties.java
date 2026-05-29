package com.agentboard.docs.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Upstream OpenAPI document URLs keyed by service slug. */
@ConfigurationProperties(prefix = "agentboard.openapi")
public class OpenApiAggregatorProperties {

  private Map<String, String> upstream = new LinkedHashMap<>();

  /** Returns upstream OpenAPI URLs keyed by service slug. */
  public Map<String, String> getUpstream() {
    return upstream;
  }

  /** Sets upstream OpenAPI URLs keyed by service slug. */
  public void setUpstream(Map<String, String> upstream) {
    this.upstream = upstream;
  }
}
