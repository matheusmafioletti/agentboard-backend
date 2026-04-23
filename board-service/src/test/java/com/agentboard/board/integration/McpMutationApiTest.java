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
 * Integration tests for MCP mutation endpoints: stage, artifacts, tasks, complete, fail.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class McpMutationApiTest {

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
    rawApiKey = "mcp-key-" + UUID.randomUUID();
    seedBoardForTenant(tenantId);
    storeApiKey(tenantId, rawApiKey);
  }

  @Test
  void moveFeature_returns200WithNewStage() {
    String cardId = createFeatureCard("Stage Test Card");

    given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "stage": "SPECIFY" }
            """)
    .when()
        .patch("/api/features/{id}/stage", cardId)
    .then()
        .statusCode(200)
        .body("columnId", notNullValue());
  }

  @Test
  void moveFeature_invalidStage_returns400() {
    String cardId = createFeatureCard("Invalid Stage Card");

    given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "stage": "INVALID_STAGE" }
            """)
    .when()
        .patch("/api/features/{id}/stage", cardId)
    .then()
        .statusCode(400);
  }

  @Test
  void addArtifact_returns201WithArtifact() {
    String cardId = createFeatureCard("Artifact Card");

    given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "command": "specify", "content": "# Spec content", "agentIdentifier": "test-agent" }
            """)
    .when()
        .post("/api/features/{id}/artifacts", cardId)
    .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("command", equalTo("specify"))
        .body("agentIdentifier", equalTo("test-agent"));
  }

  @Test
  void createTasks_returns201AndMovesCardToInProgress() {
    String cardId = createFeatureCard("Tasks Card");

    given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            {
              "tasks": [
                { "title": "Task A", "priority": "P1" },
                { "title": "Task B", "priority": "P2" }
              ]
            }
            """)
    .when()
        .post("/api/features/{id}/tasks", cardId)
    .then()
        .statusCode(201)
        .body("tasks.size()", equalTo(2))
        .body("tasks[0].title", equalTo("Task A"))
        .body("tasks[0].completed", equalTo(false));
  }

  @Test
  void completeTask_lastTask_returns200WithAutoMovedToReview() {
    String cardId = createFeatureCard("Review Card");

    String taskId = given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "tasks": [{ "title": "Only Task", "priority": "P1" }] }
            """)
        .post("/api/features/{id}/tasks", cardId)
        .then().statusCode(201)
        .extract().path("tasks[0].id");

    given()
        .header("X-API-Key", rawApiKey)
    .when()
        .patch("/api/features/{featureId}/tasks/{taskId}/complete", cardId, taskId)
    .then()
        .statusCode(200)
        .body("autoMovedToReview", equalTo(true))
        .body("task.completed", equalTo(true));
  }

  @Test
  void completeTask_alreadyCompleted_returns409() {
    String cardId = createFeatureCard("Conflict Card");

    String taskId = given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "tasks": [{ "title": "Once Task", "priority": "P1" }] }
            """)
        .post("/api/features/{id}/tasks", cardId)
        .then().statusCode(201)
        .extract().path("tasks[0].id");

    given()
        .header("X-API-Key", rawApiKey)
        .patch("/api/features/{fid}/tasks/{tid}/complete", cardId, taskId)
        .then().statusCode(200);

    given()
        .header("X-API-Key", rawApiKey)
    .when()
        .patch("/api/features/{fid}/tasks/{tid}/complete", cardId, taskId)
    .then()
        .statusCode(409);
  }

  @Test
  void failTask_returns200WithBlockedTask() {
    String cardId = createFeatureCard("Fail Task Card");

    String taskId = given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "tasks": [{ "title": "Blocked Task", "priority": "P2" }] }
            """)
        .post("/api/features/{id}/tasks", cardId)
        .then().statusCode(201)
        .extract().path("tasks[0].id");

    given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("""
            { "reason": "Missing dependency" }
            """)
    .when()
        .patch("/api/features/{fid}/tasks/{tid}/fail", cardId, taskId)
    .then()
        .statusCode(200)
        .body("blocked", equalTo(true))
        .body("blockedReason", equalTo("Missing dependency"));
  }

  private String createFeatureCard(String title) {
    return given()
        .contentType(ContentType.JSON)
        .header("X-API-Key", rawApiKey)
        .body("{ \"title\": \"" + title + "\" }")
        .post("/api/features")
        .then().statusCode(201)
        .extract().path("id");
  }

  private void seedBoardForTenant(UUID tenant) {
    boardRepository.save(new Board(tenant, "Test Board"));
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
