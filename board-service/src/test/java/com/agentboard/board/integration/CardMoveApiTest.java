package com.agentboard.board.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.agentboard.board.domain.Stage;
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
 * Integration tests for {@code PATCH /api/features/{id}/move}.
 * Verifies happy-path move and cross-tenant rejection.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CardMoveApiTest {

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
  void moveCard_toSpecifyColumn_returns200WithUpdatedColumn() {
    String cardId = createCard("Feature to Move");

    String specifyColumnId = columnDefRepository
        .findByTenantIdAndStage(tenantId, Stage.SPECIFY)
        .orElseThrow()
        .getId()
        .toString();

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "targetColumnId": "%s", "displayOrder": 0 }
            """.formatted(specifyColumnId))
    .when()
        .patch("/api/features/{id}/move", cardId)
    .then()
        .statusCode(200)
        .body("id", notNullValue())
        .body("columnId", equalTo(specifyColumnId))
        .body("displayOrder", equalTo(0));
  }

  @Test
  void moveCard_crossTenantTargetColumn_returns403() {
    String cardId = createCard("Another Feature");

    UUID otherTenant = UUID.randomUUID();
    seedBoardForTenant(otherTenant);
    String otherColumnId = columnDefRepository
        .findByTenantIdAndStage(otherTenant, Stage.SPECIFY)
        .orElseThrow()
        .getId()
        .toString();

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "targetColumnId": "%s", "displayOrder": 0 }
            """.formatted(otherColumnId))
    .when()
        .patch("/api/features/{id}/move", cardId)
    .then()
        .statusCode(403);
  }

  @Test
  void moveCard_nonExistentCard_returns404() {
    String specifyColumnId = columnDefRepository
        .findByTenantIdAndStage(tenantId, Stage.SPECIFY)
        .orElseThrow()
        .getId()
        .toString();

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "targetColumnId": "%s", "displayOrder": 0 }
            """.formatted(specifyColumnId))
    .when()
        .patch("/api/features/{id}/move", UUID.randomUUID())
    .then()
        .statusCode(404);
  }

  private String createCard(String title) {
    return given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "title": "%s" }
            """.formatted(title))
        .post("/api/features")
        .then().statusCode(201)
        .extract().path("id");
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
