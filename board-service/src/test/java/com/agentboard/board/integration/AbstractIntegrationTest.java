package com.agentboard.board.integration;

import com.agentboard.board.domain.Project;
import com.agentboard.board.repository.ProjectRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.restassured.RestAssured;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all board-service integration tests.
 *
 * <p>Starts a shared PostgreSQL 16 TestContainer and applies Flyway migrations on first boot.
 * Subclasses inherit {@link #buildJwt} and RestAssured port configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("agentboard")
          .withUsername("agentboard")
          .withPassword("agentboard");

  @LocalServerPort
  int port;

  @Value("${jwt.secret}")
  String jwtSecret;

  @Autowired
  ProjectRepository projectRepository;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @BeforeEach
  void configureRestAssured() {
    RestAssured.port = port;
  }

  /**
   * Persists a Project row for isolated integration tests ({@code POST /api/v1/projects} relies on the
   * same HTTP/security stack exercised by assertions; persisting avoids duplicate bootstrapping when
   * multiple IT classes share one Spring context).
   *
   * @param tenantId owning tenant
   * @return persisted project id
   */
  protected UUID createProject(UUID tenantId) {
    UUID suffix = UUID.randomUUID();
    Project project = new Project(
        tenantId,
        "Integration Project " + suffix,
        null,
        "agb_it_" + suffix.toString().replace("-", ""));
    return projectRepository.save(project).getId();
  }

  /**
   * Builds a signed JWT for the given tenant and user.
   *
   * @param tenantId the tenant UUID encoded in the {@code tenantId} claim
   * @param userId   the subject claim
   * @return a compact JWT string valid for 1 hour
   */
  protected String buildJwt(UUID tenantId, UUID userId) {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    return Jwts.builder()
        .subject(userId.toString())
        .claim("tenantId", tenantId.toString())
        .claim("roles", new String[]{"USER"})
        .expiration(new Date(System.currentTimeMillis() + 3_600_000))
        .signWith(Keys.hmacShaKeyFor(keyBytes))
        .compact();
  }
}
