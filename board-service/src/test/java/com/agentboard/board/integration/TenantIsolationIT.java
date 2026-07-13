package com.agentboard.board.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Cross-tenant isolation tests for board-service REST endpoints.
 *
 * <p>Every scenario provisions data for tenant A and asserts that a JWT scoped to tenant B can
 * neither read nor mutate it, while identical display keys remain legal across tenants.
 */
class TenantIsolationIT extends AbstractIntegrationTest {

  @Test
  void listProjects_otherTenant_returnsEmpty() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    String jwtA = buildJwt(tenantA, UUID.randomUUID());
    String jwtB = buildJwt(tenantB, UUID.randomUUID());
    createProject(tenantA);

    given()
        .header("Authorization", "Bearer " + jwtA)
        .when()
        .get("/api/v1/projects")
        .then()
        .statusCode(200)
        .body("$", hasSize(1));

    given()
        .header("Authorization", "Bearer " + jwtB)
        .when()
        .get("/api/v1/projects")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  @Test
  void getWorkItem_ofOtherTenant_returns404() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    String jwtA = buildJwt(tenantA, UUID.randomUUID());
    String jwtB = buildJwt(tenantB, UUID.randomUUID());
    UUID projectA = createProject(tenantA);
    String featureId = createFeature(jwtA, projectA, "Tenant A Feature");

    given()
        .header("Authorization", "Bearer " + jwtB)
        .when()
        .get("/api/v1/work-items/" + featureId)
        .then()
        .statusCode(404)
        .body("error", equalTo("RESOURCE_NOT_FOUND"));
  }

  @Test
  void patchWorkItem_ofOtherTenant_returns404() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    String jwtA = buildJwt(tenantA, UUID.randomUUID());
    String jwtB = buildJwt(tenantB, UUID.randomUUID());
    UUID projectA = createProject(tenantA);
    String featureId = createFeature(jwtA, projectA, "Tenant A Feature");

    given()
        .header("Authorization", "Bearer " + jwtB)
        .contentType(ContentType.JSON)
        .body(Map.of("title", "Hijacked"))
        .when()
        .patch("/api/v1/work-items/" + featureId)
        .then()
        .statusCode(404)
        .body("error", equalTo("RESOURCE_NOT_FOUND"));

    given()
        .header("Authorization", "Bearer " + jwtB)
        .contentType(ContentType.JSON)
        .body(Map.of("status", "SPECIFY"))
        .when()
        .patch("/api/v1/work-items/" + featureId + "/status")
        .then()
        .statusCode(404)
        .body("error", equalTo("RESOURCE_NOT_FOUND"));

    given()
        .header("Authorization", "Bearer " + jwtA)
        .when()
        .get("/api/v1/work-items/" + featureId)
        .then()
        .statusCode(200)
        .body("title", equalTo("Tenant A Feature"))
        .body("status", equalTo("BACKLOG"));
  }

  @Test
  void listWorkItems_ofOtherTenantsProject_returnsEmpty() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    String jwtA = buildJwt(tenantA, UUID.randomUUID());
    String jwtB = buildJwt(tenantB, UUID.randomUUID());
    UUID projectA = createProject(tenantA);
    createFeature(jwtA, projectA, "Hidden Feature");

    given()
        .header("Authorization", "Bearer " + jwtB)
        .queryParam("projectId", projectA)
        .when()
        .get("/api/v1/work-items")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  @Test
  void displayKey_firstFeaturePerTenant_isF1InBothTenants() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    String jwtA = buildJwt(tenantA, UUID.randomUUID());
    String jwtB = buildJwt(tenantB, UUID.randomUUID());
    UUID projectA = createProject(tenantA);
    UUID projectB = createProject(tenantB);

    given()
        .header("Authorization", "Bearer " + jwtA)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectA)
        .body(Map.of("type", "FEATURE", "title", "A First", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .body("displayKey", equalTo("F1"));

    given()
        .header("Authorization", "Bearer " + jwtB)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectB)
        .body(Map.of("type", "FEATURE", "title", "B First", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .body("displayKey", equalTo("F1"));
  }

  @Test
  void getProject_ofOtherTenant_returns404() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    String jwtB = buildJwt(tenantB, UUID.randomUUID());
    UUID projectA = createProject(tenantA);

    given()
        .header("Authorization", "Bearer " + jwtB)
        .when()
        .get("/api/v1/projects/" + projectA)
        .then()
        .statusCode(404);
  }

  private String createFeature(String jwt, UUID projectId, String title) {
    return given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "FEATURE", "title", title, "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }
}
