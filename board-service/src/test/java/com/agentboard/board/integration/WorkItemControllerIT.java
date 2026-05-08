package com.agentboard.board.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link com.agentboard.board.api.WorkItemController}.
 *
 * <p>Runs against a Flyway-migrated PostgreSQL 16 TestContainer.
 */
class WorkItemControllerIT extends AbstractIntegrationTest {

  @Test
  void listWorkItems_byTypeFeature_returns200() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);

    UUID projectId = createProject(tenantId);

    given()
        .header("Authorization", "Bearer " + jwt)
        .queryParam("projectId", projectId)
        .queryParam("type", "FEATURE")
        .when()
        .get("/api/v1/work-items")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  @Test
  void createFeature_returnsCreated() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);
    UUID projectId = createProject(tenantId);

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "FEATURE", "title", "My Feature", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .body("type", equalTo("FEATURE"))
        .body("status", equalTo("BACKLOG"))
        .body("title", equalTo("My Feature"))
        .body("displayKey", matchesPattern("F[1-9][0-9]*"));
  }

  @Test
  void createUserStory_withValidFeatureParent_returns201WithCorrectParentId() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);
    UUID projectId = createProject(tenantId);

    String featureId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "FEATURE", "title", "Feature A", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "USER_STORY", "title", "US 1", "parentId", featureId, "priority", 1))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .body("type", equalTo("USER_STORY"))
        .body("status", equalTo("READY"))
        .body("parentId", equalTo(featureId));
  }

  @Test
  void createWorkItem_withInvalidParentType_returns400() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);
    UUID projectId = createProject(tenantId);

    String featureId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "FEATURE", "title", "Feature", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "TASK", "title", "Task", "parentId", featureId, "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(400)
        .body("message", org.hamcrest.Matchers.containsString("INVALID_PARENT_TYPE"));
  }

  @Test
  void moveStatus_returns200WithUpdatedStatus() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);
    UUID projectId = createProject(tenantId);

    String featureId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "FEATURE", "title", "Feature", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .body(Map.of("status", "SPECIFY"))
        .when()
        .patch("/api/v1/work-items/" + featureId + "/status")
        .then()
        .statusCode(200)
        .body("status", equalTo("SPECIFY"));
  }

  @Test
  void getWorkItem_byId_returns200WithDetail() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);
    UUID projectId = createProject(tenantId);

    String featureId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "FEATURE", "title", "Feature", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
        .when()
        .get("/api/v1/work-items/" + featureId)
        .then()
        .statusCode(200)
        .body("id", equalTo(featureId))
        .body("type", equalTo("FEATURE"))
        .body("displayKey", matchesPattern("F[1-9][0-9]*"));
  }

  @Test
  void listByParentId_onlyReturnsMathcingChildren() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);
    UUID projectId = createProject(tenantId);

    String featureId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "FEATURE", "title", "Feature", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract().path("id");

    String usId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "USER_STORY", "title", "US 1", "parentId", featureId, "priority", 1))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
        .queryParam("projectId", projectId)
        .queryParam("type", "USER_STORY")
        .queryParam("parentId", featureId)
        .when()
        .get("/api/v1/work-items")
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].id", equalTo(usId));
  }

  @Test
  void moveTaskToClosed_autoTransitionsParentUserStoryToDone_whenLastTask() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);
    UUID projectId = createProject(tenantId);

    String featureId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "FEATURE", "title", "Feature", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract().path("id");

    String usId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "USER_STORY", "title", "US 1", "parentId", featureId, "priority", 1))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract().path("id");

    String taskId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "TASK", "title", "Task 1", "parentId", usId, "priority", 1))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .body(Map.of("status", "CLOSED"))
        .when()
        .patch("/api/v1/work-items/" + taskId + "/status")
        .then()
        .statusCode(200)
        .body("status", equalTo("CLOSED"));

    // Allow time for @TransactionalEventListener AFTER_COMMIT to fire
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    given()
        .header("Authorization", "Bearer " + jwt)
        .when()
        .get("/api/v1/work-items/" + usId)
        .then()
        .statusCode(200)
        .body("status", equalTo("DONE"));
  }

  @Test
  void batchCreateUserStories_returnsCreatedItemsWithParentStatus() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);
    UUID projectId = createProject(tenantId);

    String featureId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "FEATURE", "title", "Feature", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of(
            "type", "USER_STORY",
            "parentId", featureId,
            "items", java.util.List.of(
                Map.of("title", "US 1", "priority", 1),
                Map.of("title", "US 2", "priority", 2)
            )
        ))
        .when()
        .post("/api/v1/work-items/batch")
        .then()
        .statusCode(201)
        .body("workItems", hasSize(2))
        .body("workItems[0].type", equalTo("USER_STORY"))
        .body("workItems[0].parentId", equalTo(featureId));
  }

  @Test
  void patchWorkItem_preservesMarkdownDescriptionRoundTripThroughGetDetail() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);
    UUID projectId = createProject(tenantId);

    String markdown =
        """
        # Heading

        ```java
        int x = 1;
        ```

        Symbols *_[] testing 测试中
        """;

    String featureId =
        given()
            .header("Authorization", "Bearer " + jwt)
            .contentType(ContentType.JSON)
            .queryParam("projectId", projectId)
            .body(Map.of("type", "FEATURE", "title", "Markdown Item", "priority", 5))
            .when()
            .post("/api/v1/work-items")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .body(Map.of("description", markdown))
        .when()
        .patch("/api/v1/work-items/" + featureId)
        .then()
        .statusCode(200);

    given()
        .header("Authorization", "Bearer " + jwt)
        .when()
        .get("/api/v1/work-items/" + featureId)
        .then()
        .statusCode(200)
        .body("description", equalTo(markdown));
  }

  @Test
  void listWorkItems_withIncludeParent_returnsParentPreview() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, userId);
    UUID projectId = createProject(tenantId);

    String featureId =
        given()
            .header("Authorization", "Bearer " + jwt)
            .contentType(ContentType.JSON)
            .queryParam("projectId", projectId)
            .body(Map.of("type", "FEATURE", "title", "Feat", "priority", 5))
            .when()
            .post("/api/v1/work-items")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "USER_STORY", "title", "US", "parentId", featureId, "priority", 1))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201);

    given()
        .header("Authorization", "Bearer " + jwt)
        .queryParam("projectId", projectId)
        .queryParam("type", "USER_STORY")
        .queryParam("parentId", featureId)
        .queryParam("includeParent", true)
        .when()
        .get("/api/v1/work-items")
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].parentPreview.id", equalTo(featureId))
        .body("[0].parentPreview.displayKey", notNullValue());
  }
}
