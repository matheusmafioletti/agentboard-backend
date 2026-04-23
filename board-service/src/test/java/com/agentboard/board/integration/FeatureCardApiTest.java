package com.agentboard.board.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.agentboard.board.repository.BoardRepository;
import com.agentboard.board.repository.ColumnDefRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * Integration tests for Feature Card CRUD endpoints (POST, GET, PATCH, DELETE /api/features).
 * Verifies tenant isolation by asserting cross-tenant GET returns 403.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FeatureCardApiTest {

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
  BoardRepository boardRepository;

  @Autowired
  ColumnDefRepository columnDefRepository;

  private UUID tenantId;
  private String jwt;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    tenantId = UUID.randomUUID();
    jwt = buildJwt(tenantId, UUID.randomUUID());
    seedBoardForTenant(tenantId);
  }

  @Test
  void createFeature_returns201WithCard() {
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "title": "My First Feature", "description": "A test feature" }
            """)
    .when()
        .post("/api/features")
    .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("title", equalTo("My First Feature"))
        .body("description", equalTo("A test feature"))
        .body("reExecutionPending", equalTo(false))
        .body("displayOrder", equalTo(0));
  }

  @Test
  void getFeature_returns200ForOwnCard() {
    String cardId = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "title": "Fetch Me" }
            """)
        .post("/api/features")
        .then().statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
    .when()
        .get("/api/features/{id}", cardId)
    .then()
        .statusCode(200)
        .body("title", equalTo("Fetch Me"));
  }

  @Test
  void patchFeature_returns200WithUpdatedFields() {
    String cardId = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "title": "Original Title" }
            """)
        .post("/api/features")
        .then().statusCode(201)
        .extract().path("id");

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "title": "Updated Title", "description": "Updated desc" }
            """)
    .when()
        .patch("/api/features/{id}", cardId)
    .then()
        .statusCode(200)
        .body("title", equalTo("Updated Title"))
        .body("description", equalTo("Updated desc"));
  }

  @Test
  void deleteFeature_returns204AndCardGone() {
    String cardId = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "title": "To Delete" }
            """)
        .post("/api/features")
        .then().statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
    .when()
        .delete("/api/features/{id}", cardId)
    .then()
        .statusCode(204);

    given()
        .header("Authorization", "Bearer " + jwt)
    .when()
        .get("/api/features/{id}", cardId)
    .then()
        .statusCode(404);
  }

  @Test
  void getFeature_crossTenantAccess_returns403() {
    String cardId = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "title": "Tenant A Card" }
            """)
        .post("/api/features")
        .then().statusCode(201)
        .extract().path("id");

    UUID otherTenant = UUID.randomUUID();
    String otherJwt = buildJwt(otherTenant, UUID.randomUUID());

    given()
        .header("Authorization", "Bearer " + otherJwt)
    .when()
        .get("/api/features/{id}", cardId)
    .then()
        .statusCode(403);
  }

  private String buildJwt(UUID tenant, UUID userId) {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    return Jwts.builder()
        .subject(userId.toString())
        .claim("tenantId", tenant.toString())
        .claim("roles", new String[]{"USER"})
        .expiration(new Date(System.currentTimeMillis() + 3_600_000))
        .signWith(Keys.hmacShaKeyFor(keyBytes))
        .compact();
  }

  private void seedBoardForTenant(UUID tenant) {
    com.agentboard.board.domain.Board board =
        new com.agentboard.board.domain.Board(tenant, "Test Board");
    boardRepository.save(board);
  }
}
