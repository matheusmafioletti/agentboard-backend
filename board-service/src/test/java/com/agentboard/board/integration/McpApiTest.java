package com.agentboard.board.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.agentboard.board.domain.Board;
import com.agentboard.board.domain.TenantApiKey;
import com.agentboard.board.repository.BoardRepository;
import com.agentboard.board.repository.TenantApiKeyRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for MCP query endpoints: POST /api/features (API key), GET /api/features/{id},
 * GET /api/features with stage filter, and cross-tenant access (403).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class McpApiTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("agentboard")
          .withUsername("agentboard")
          .withPassword("agentboard");

  @LocalServerPort
  int port;

  @Autowired
  BoardRepository boardRepository;

  @Autowired
  TenantApiKeyRepository tenantApiKeyRepository;

  private UUID tenantId;
  private String rawApiKey;

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
    rawApiKey = "test-key-" + UUID.randomUUID();
    seedBoardForTenant(tenantId);
    storeApiKey(tenantId, rawApiKey);
  }

  @Test
  void createFeature_withApiKey_returns201() {
    given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "title": "MCP Feature", "description": "Created by MCP" }
            """)
    .when()
        .post("/api/features")
    .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("title", equalTo("MCP Feature"));
  }

  @Test
  void getFeature_withApiKey_returns200() {
    String cardId = given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "title": "To Fetch" }
            """)
        .post("/api/features")
        .then().statusCode(201)
        .extract().path("id");

    given()
        .header("X-API-Key", rawApiKey)
    .when()
        .get("/api/features/{id}", cardId)
    .then()
        .statusCode(200)
        .body("title", equalTo("To Fetch"));
  }

  @Test
  void listFeatures_withStageFilter_returns200WithMatchingCards() {
    given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "title": "Backlog Card" }
            """)
        .post("/api/features")
        .then().statusCode(201);

    given()
        .header("X-API-Key", rawApiKey)
        .queryParam("stage", "BACKLOG")
    .when()
        .get("/api/features")
    .then()
        .statusCode(200)
        .body("features.size()", equalTo(1))
        .body("features[0].title", equalTo("Backlog Card"));
  }

  @Test
  void listFeatures_withUnknownApiKey_returns401() {
    given()
        .header("X-API-Key", "invalid-key")
        .queryParam("stage", "BACKLOG")
    .when()
        .get("/api/features")
    .then()
        .statusCode(401);
  }

  @Test
  void getFeature_crossTenantApiKey_returns403() {
    String cardId = given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "title": "Tenant A Card" }
            """)
        .post("/api/features")
        .then().statusCode(201)
        .extract().path("id");

    UUID otherTenantId = UUID.randomUUID();
    String otherKey = "other-key-" + UUID.randomUUID();
    seedBoardForTenant(otherTenantId);
    storeApiKey(otherTenantId, otherKey);

    given()
        .header("X-API-Key", otherKey)
    .when()
        .get("/api/features/{id}", cardId)
    .then()
        .statusCode(403);
  }

  private void seedBoardForTenant(UUID tenant) {
    Board board = new Board(tenant, "Test Board");
    boardRepository.save(board);
  }

  private void storeApiKey(UUID tenant, String rawKey) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(rawKey.getBytes(StandardCharsets.UTF_8));
      String hash = HexFormat.of().formatHex(digest);
      tenantApiKeyRepository.save(new TenantApiKey(tenant, hash));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
