package com.agentboard.board.integration.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.agentboard.board.integration.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

/** Integration tests for Project CRUD endpoints. */
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class ProjectControllerIT extends AbstractIntegrationTest {

  @Test
  void listProjects_newTenant_returnsEmptyArray() {
    UUID tenantId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, UUID.randomUUID());

    given()
        .header("Authorization", "Bearer " + jwt)
    .when()
        .get("/api/v1/projects")
    .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  @Test
  void createProject_validPayload_returns201WithBody() {
    UUID tenantId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, UUID.randomUUID());

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "My Feature Project",
              "constitutionContent": "# Constitution"
            }
            """)
    .when()
        .post("/api/v1/projects")
    .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("name", equalTo("My Feature Project"))
        .body("constitutionContent", equalTo("# Constitution"));
  }

  @Test
  void createProject_blankName_returns400() {
    UUID tenantId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, UUID.randomUUID());

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "",
              "constitutionContent": "# Constitution"
            }
            """)
    .when()
        .post("/api/v1/projects")
    .then()
        .statusCode(400);
  }

  @Test
  void getProject_existingId_returnsSavedData() {
    UUID tenantId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, UUID.randomUUID());

    String projectId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .body("""
            {"name": "Detail Test Project", "constitutionContent": "# Rules"}
            """)
        .post("/api/v1/projects")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
    .when()
        .get("/api/v1/projects/" + projectId)
    .then()
        .statusCode(200)
        .body("id", equalTo(projectId))
        .body("name", equalTo("Detail Test Project"));
  }

  @Test
  void updateProject_nameAndConstitution_returns200WithUpdatedData() {
    UUID tenantId = UUID.randomUUID();
    String jwt = buildJwt(tenantId, UUID.randomUUID());

    String projectId = given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .body("""
            {"name": "Original Name", "constitutionContent": "# Old"}
            """)
        .post("/api/v1/projects")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .body("""
            {"name": "Updated Name", "constitutionContent": "# New Constitution"}
            """)
    .when()
        .put("/api/v1/projects/" + projectId)
    .then()
        .statusCode(200)
        .body("name", equalTo("Updated Name"))
        .body("constitutionContent", equalTo("# New Constitution"));
  }

  @Test
  void getProject_differentTenant_returns404() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    String jwtA = buildJwt(tenantA, UUID.randomUUID());
    String jwtB = buildJwt(tenantB, UUID.randomUUID());

    String projectId = given()
        .header("Authorization", "Bearer " + jwtA)
        .contentType(ContentType.JSON)
        .body("""
            {"name": "Tenant A Project", "constitutionContent": "# A"}
            """)
        .post("/api/v1/projects")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .header("Authorization", "Bearer " + jwtB)
    .when()
        .get("/api/v1/projects/" + projectId)
    .then()
        .statusCode(404);
  }
}
