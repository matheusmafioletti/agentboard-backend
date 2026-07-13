package com.agentboard.board.integration.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.board.integration.AbstractIntegrationTest;
import com.agentboard.board.integration.TestTenantSupport;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Integration tests for work item creation with {@code X-Data-Source} policy. */
class WorkItemDataSourceIT extends AbstractIntegrationTest {

  @Autowired
  TestTenantSupport testTenantSupport;

  @Test
  void createWorkItem_withSeedOnTestTenant_persistsSeedDataSource() {
    UUID tenantId = UUID.randomUUID();
    testTenantSupport.markTestTenant(tenantId);
    String jwt = buildJwt(tenantId, UUID.randomUUID());
    UUID projectId = createProject(tenantId);

    String workItemId = given()
        .header("Authorization", "Bearer " + jwt)
        .header("X-Data-Source", "seed")
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(Map.of("type", "FEATURE", "title", "Seed Feature", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    assertThat(testTenantSupport.workItemDataSource(UUID.fromString(workItemId)))
        .isEqualTo("seed");
  }

  @Test
  void createWorkItem_forOtherTenantProject_returns404() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    String jwtB = buildJwt(tenantB, UUID.randomUUID());
    UUID projectA = createProject(tenantA);

    given()
        .header("Authorization", "Bearer " + jwtB)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectA)
        .body(Map.of("type", "FEATURE", "title", "Cross Tenant", "priority", 5))
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(404);
  }
}
