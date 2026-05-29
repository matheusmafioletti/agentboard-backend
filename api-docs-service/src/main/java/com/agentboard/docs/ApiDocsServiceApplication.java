package com.agentboard.docs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Aggregated Swagger UI entry point for all AgentBoard backend services. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiDocsServiceApplication {

  /** Starts the api-docs-service Spring Boot application. */
  public static void main(String[] args) {
    SpringApplication.run(ApiDocsServiceApplication.class, args);
  }
}
