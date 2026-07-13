package com.agentboard.auth.integration;

import static io.restassured.RestAssured.given;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for all auth-service integration tests.
 *
 * <p>Starts a shared PostgreSQL 16 TestContainer and applies Flyway migrations on first boot.
 * Datasource coordinates are injected via {@link DynamicPropertySource}; all remaining
 * configuration (including {@code jwt.secret}) comes from the main {@code application.yml}
 * defaults, mirroring the board-service test setup.
 *
 * <p>NOTE: the container is started once per JVM (singleton pattern) instead of using
 * {@code @Testcontainers}, because the cached Spring context is shared by several IT classes
 * and a per-class container restart would leave it pointing at a dead database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractAuthIntegrationTest {

  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("agentboard")
          .withUsername("agentboard")
          .withPassword("agentboard");

  static {
    postgres.start();
  }

  static final String DEFAULT_PASSWORD = "S3cret-password";

  @LocalServerPort
  int port;

  @Value("${jwt.secret}")
  String jwtSecret;

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
   * Identity created through {@code POST /auth/register}, retained with the raw password so
   * follow-up login and tenant-switch calls can re-authenticate.
   */
  protected record RegisteredUser(
      UUID userId,
      UUID tenantId,
      String tenantName,
      String email,
      String password,
      String token) {}

  /**
   * Registers a fresh user with a random email and tenant name via the public HTTP API.
   *
   * @return the registered identity including the admin JWT for the new tenant
   */
  protected RegisteredUser registerUser() {
    return registerUser(uniqueEmail(), DEFAULT_PASSWORD, uniqueTenantName());
  }

  /**
   * Registers a user via {@code POST /auth/register} and asserts the 201 response.
   *
   * @param email      unique global email
   * @param password   raw password (min 8 chars)
   * @param tenantName unique workspace name
   * @return the registered identity including the admin JWT for the new tenant
   */
  protected RegisteredUser registerUser(String email, String password, String tenantName) {
    var extracted = given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "Test User",
            "email", email,
            "password", password,
            "tenantName", tenantName))
        .when()
        .post("/auth/register")
        .then()
        .statusCode(201)
        .extract();
    return new RegisteredUser(
        UUID.fromString(extracted.path("userId")),
        UUID.fromString(extracted.path("tenantId")),
        tenantName,
        email,
        password,
        extracted.path("token"));
  }

  /**
   * Generates a globally unique email so tests sharing one database never collide.
   *
   * @return a random {@code @example.com} address
   */
  protected String uniqueEmail() {
    return "user-" + UUID.randomUUID() + "@example.com";
  }

  /**
   * Generates a globally unique tenant name.
   *
   * @return a random workspace name
   */
  protected String uniqueTenantName() {
    return "Tenant " + UUID.randomUUID();
  }

  /**
   * Verifies the JWT signature against the configured secret and extracts the tenant claim.
   *
   * @param jwt compact JWT issued by the service under test
   * @return the {@code tenantId} claim
   */
  protected UUID tenantIdClaim(String jwt) {
    Claims claims = Jwts.parser()
        .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
        .build()
        .parseSignedClaims(jwt)
        .getPayload();
    return UUID.fromString(claims.get("tenantId", String.class));
  }
}
