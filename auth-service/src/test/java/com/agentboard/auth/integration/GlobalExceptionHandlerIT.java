package com.agentboard.auth.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link com.agentboard.auth.controller.GlobalExceptionHandler}.
 *
 * <p>Each test reaches a domain exception through the public HTTP API and asserts the exact
 * status code, error code, and error body structure produced by the handler.
 */
class GlobalExceptionHandlerIT extends AbstractAuthIntegrationTest {

  @Test
  void duplicateEmail_mapsTo409WithErrorBodyStructure() {
    RegisteredUser user = registerUser();

    given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "Clone",
            "email", user.email(),
            "password", DEFAULT_PASSWORD,
            "tenantName", uniqueTenantName()))
        .when()
        .post("/auth/register")
        .then()
        .statusCode(409)
        .body("error", equalTo("EMAIL_ALREADY_REGISTERED"))
        .body("message", notNullValue())
        .body("timestamp", notNullValue());
  }

  @Test
  void duplicateTenantName_mapsTo409() {
    RegisteredUser user = registerUser();

    given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "Other",
            "email", uniqueEmail(),
            "password", DEFAULT_PASSWORD,
            "tenantName", user.tenantName()))
        .when()
        .post("/auth/register")
        .then()
        .statusCode(409)
        .body("error", equalTo("TENANT_NAME_TAKEN"));
  }

  @Test
  void invalidCredentials_mapsTo401() {
    RegisteredUser user = registerUser();

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", user.email(), "password", "wrong-password-123"))
        .when()
        .post("/auth/login")
        .then()
        .statusCode(401)
        .body("error", equalTo("INVALID_CREDENTIALS"));
  }

  @Test
  void duplicatePendingInvite_mapsTo409() {
    RegisteredUser admin = registerUser();
    String inviteeEmail = uniqueEmail();
    createInvite(admin, inviteeEmail);

    given()
        .header("Authorization", "Bearer " + admin.token())
        .contentType(ContentType.JSON)
        .body(Map.of("email", inviteeEmail))
        .when()
        .post("/auth/tenants/" + admin.tenantId() + "/invites")
        .then()
        .statusCode(409)
        .body("error", equalTo("CONFLICT"));
  }

  @Test
  void alreadyMember_mapsTo409() {
    RegisteredUser admin = registerUser();

    given()
        .header("Authorization", "Bearer " + admin.token())
        .contentType(ContentType.JSON)
        .body(Map.of("email", admin.email()))
        .when()
        .post("/auth/tenants/" + admin.tenantId() + "/invites")
        .then()
        .statusCode(409)
        .body("error", equalTo("CONFLICT"));
  }

  @Test
  void inviteGone_mapsTo410() {
    given()
        .when()
        .get("/auth/invites/" + UUID.randomUUID())
        .then()
        .statusCode(410)
        .body("error", equalTo("INVITE_GONE"));
  }

  @Test
  void lastAdmin_mapsTo409() {
    RegisteredUser admin = registerUser();

    given()
        .header("Authorization", "Bearer " + admin.token())
        .when()
        .delete("/auth/tenants/" + admin.tenantId() + "/members/" + admin.userId())
        .then()
        .statusCode(409)
        .body("error", equalTo("LAST_ADMIN"));
  }

  @Test
  void forbiddenOperation_tenantMismatch_mapsTo403() {
    RegisteredUser adminA = registerUser();
    RegisteredUser adminB = registerUser();

    given()
        .header("Authorization", "Bearer " + adminA.token())
        .when()
        .get("/auth/tenants/" + adminB.tenantId() + "/invites")
        .then()
        .statusCode(403)
        .body("error", equalTo("FORBIDDEN"));
  }

  @Test
  void notMember_switchToForeignTenant_mapsTo403() {
    RegisteredUser adminA = registerUser();
    RegisteredUser adminB = registerUser();

    given()
        .header("Authorization", "Bearer " + adminA.token())
        .contentType(ContentType.JSON)
        .body(Map.of("tenantId", adminB.tenantId().toString()))
        .when()
        .post("/auth/switch-tenant")
        .then()
        .statusCode(403)
        .body("error", equalTo("FORBIDDEN"));
  }

  @Test
  void noMembership_loginAfterOnlyMembershipRevoked_mapsTo403() {
    RegisteredUser admin = registerUser();
    String inviteeEmail = uniqueEmail();
    String token = createInvite(admin, inviteeEmail);

    String memberUserId = given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Member", "password", DEFAULT_PASSWORD))
        .when()
        .post("/auth/invites/" + token + "/accept")
        .then()
        .statusCode(200)
        .extract()
        .path("userId");

    given()
        .header("Authorization", "Bearer " + admin.token())
        .when()
        .delete("/auth/tenants/" + admin.tenantId() + "/members/" + memberUserId)
        .then()
        .statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", inviteeEmail, "password", DEFAULT_PASSWORD))
        .when()
        .post("/auth/login")
        .then()
        .statusCode(403)
        .body("error", equalTo("FORBIDDEN"));
  }

  @Test
  void passwordConfirmationMismatch_mapsTo400BadRequest() {
    RegisteredUser user = registerUser();

    given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "userId", user.userId().toString(),
            "currentPassword", user.password(),
            "newPassword", "N3w-secret-password",
            "confirmNewPassword", "different-password-1"))
        .when()
        .put("/auth/change-password")
        .then()
        .statusCode(400)
        .body("error", equalTo("BAD_REQUEST"));
  }

  @Test
  void beanValidationFailure_mapsTo400ValidationError() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", "not-an-email", "password", ""))
        .when()
        .post("/auth/login")
        .then()
        .statusCode(400)
        .body("error", equalTo("VALIDATION_ERROR"))
        .body("message", notNullValue())
        .body("timestamp", notNullValue());
  }

  private String createInvite(RegisteredUser admin, String inviteeEmail) {
    String inviteUrl = given()
        .header("Authorization", "Bearer " + admin.token())
        .contentType(ContentType.JSON)
        .body(Map.of("email", inviteeEmail))
        .when()
        .post("/auth/tenants/" + admin.tenantId() + "/invites")
        .then()
        .statusCode(201)
        .extract()
        .path("inviteUrl");
    return inviteUrl.substring(inviteUrl.lastIndexOf("/invite/") + "/invite/".length());
  }
}
