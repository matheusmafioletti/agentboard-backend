package com.agentboard.auth.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.agentboard.auth.repository.UserAccountRepository;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link com.agentboard.auth.controller.AuthController}.
 *
 * <p>Runs against a Flyway-migrated PostgreSQL 16 TestContainer through the full HTTP and
 * Spring Security stack.
 */
class AuthControllerIT extends AbstractAuthIntegrationTest {

  @Autowired
  UserAccountRepository userAccountRepository;

  @Autowired
  TestTenantSupport testTenantSupport;

  @Test
  void register_withoutHeader_persistsManualDataSource() {
    String email = uniqueEmail();
    String tenantName = uniqueTenantName();

    var extracted = given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "Alice",
            "email", email,
            "password", DEFAULT_PASSWORD,
            "tenantName", tenantName))
        .when()
        .post("/auth/register")
        .then()
        .statusCode(201)
        .extract();

    UUID userId = UUID.fromString(extracted.path("userId"));
    assertThat(testTenantSupport.userDataSource(userId)).isEqualTo("manual");
  }

  @Test
  void register_withAutomationHeader_returns403() {
    given()
        .header("X-Data-Source", "automation")
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "Bot User",
            "email", uniqueEmail(),
            "password", DEFAULT_PASSWORD,
            "tenantName", uniqueTenantName()))
        .when()
        .post("/auth/register")
        .then()
        .statusCode(403)
        .body("error", equalTo("DATA_SOURCE_NOT_ALLOWED"));
  }

  @Test
  void register_withInvalidHeader_returns400() {
    given()
        .header("X-Data-Source", "bot")
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "Bad Header",
            "email", uniqueEmail(),
            "password", DEFAULT_PASSWORD,
            "tenantName", uniqueTenantName()))
        .when()
        .post("/auth/register")
        .then()
        .statusCode(400)
        .body("error", equalTo("INVALID_DATA_SOURCE"));
  }

  @Test
  void createTenant_withAutomationHeader_returns403() {
    RegisteredUser user = registerUser();

    given()
        .header("Authorization", "Bearer " + user.token())
        .header("X-Data-Source", "seed")
        .contentType(ContentType.JSON)
        .body(Map.of("tenantName", uniqueTenantName()))
        .when()
        .post("/auth/tenants")
        .then()
        .statusCode(403)
        .body("error", equalTo("DATA_SOURCE_NOT_ALLOWED"));
  }

  @Test
  void register_validPayload_returns201AndPersistsUser() {
    String email = uniqueEmail();
    String tenantName = uniqueTenantName();

    given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "Alice",
            "email", email,
            "password", DEFAULT_PASSWORD,
            "tenantName", tenantName))
        .when()
        .post("/auth/register")
        .then()
        .statusCode(201)
        .body("userId", notNullValue())
        .body("tenantId", notNullValue())
        .body("tenantName", equalTo(tenantName))
        .body("token", notNullValue())
        .body("role", equalTo("ADMIN"))
        .body("apiKey", notNullValue());

    assertThat(userAccountRepository.findByEmail(email)).isPresent();
  }

  @Test
  void register_duplicateEmail_returns409() {
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
        .body("error", equalTo("EMAIL_ALREADY_REGISTERED"));
  }

  @Test
  void register_invalidEmail_returns400() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "Bad Email",
            "email", "not-an-email",
            "password", DEFAULT_PASSWORD,
            "tenantName", uniqueTenantName()))
        .when()
        .post("/auth/register")
        .then()
        .statusCode(400)
        .body("error", equalTo("VALIDATION_ERROR"));
  }

  @Test
  void register_shortPassword_returns400() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "Short Password",
            "email", uniqueEmail(),
            "password", "short",
            "tenantName", uniqueTenantName()))
        .when()
        .post("/auth/register")
        .then()
        .statusCode(400)
        .body("error", equalTo("VALIDATION_ERROR"));
  }

  @Test
  void login_singleTenant_returns200WithSession() {
    RegisteredUser user = registerUser();

    String token = given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", user.email(), "password", user.password()))
        .when()
        .post("/auth/login")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("userId", equalTo(user.userId().toString()))
        .body("tenantId", equalTo(user.tenantId().toString()))
        .body("tenantName", equalTo(user.tenantName()))
        .body("email", equalTo(user.email()))
        .body("role", equalTo("ADMIN"))
        .extract()
        .path("token");

    assertThat(tenantIdClaim(token)).isEqualTo(user.tenantId());
  }

  @Test
  void login_multiTenant_returns200WithTenantSelection() {
    RegisteredUser user = registerUser();
    createSecondTenant(user);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", user.email(), "password", user.password()))
        .when()
        .post("/auth/login")
        .then()
        .statusCode(200)
        .body("requiresTenantSelection", equalTo(true))
        .body("userId", equalTo(user.userId().toString()))
        .body("memberships", hasSize(2))
        .body("token", org.hamcrest.Matchers.nullValue());
  }

  @Test
  void login_wrongPassword_returns401() {
    RegisteredUser user = registerUser();

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", user.email(), "password", "totally-wrong-password"))
        .when()
        .post("/auth/login")
        .then()
        .statusCode(401)
        .body("error", equalTo("INVALID_CREDENTIALS"));
  }

  @Test
  void selectTenant_validCredentials_returnsSessionForChosenTenant() {
    RegisteredUser user = registerUser();
    UUID secondTenantId = createSecondTenant(user);

    String token = given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "email", user.email(),
            "password", user.password(),
            "tenantId", secondTenantId.toString()))
        .when()
        .post("/auth/select-tenant")
        .then()
        .statusCode(200)
        .body("tenantId", equalTo(secondTenantId.toString()))
        .extract()
        .path("token");

    assertThat(tenantIdClaim(token)).isEqualTo(secondTenantId);
  }

  @Test
  void switchTenant_withValidJwt_returnsNewJwtForOtherTenant() {
    RegisteredUser user = registerUser();
    UUID secondTenantId = createSecondTenant(user);

    String token = given()
        .header("Authorization", "Bearer " + user.token())
        .contentType(ContentType.JSON)
        .body(Map.of("tenantId", secondTenantId.toString()))
        .when()
        .post("/auth/switch-tenant")
        .then()
        .statusCode(200)
        .body("tenantId", equalTo(secondTenantId.toString()))
        .extract()
        .path("token");

    assertThat(tenantIdClaim(token))
        .isEqualTo(secondTenantId)
        .isNotEqualTo(tenantIdClaim(user.token()));
  }

  @Test
  void switchTenant_withoutJwt_returns403() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("tenantId", UUID.randomUUID().toString()))
        .when()
        .post("/auth/switch-tenant")
        .then()
        .statusCode(403);
  }

  @Test
  void changePassword_correctCurrentPassword_returns204AndNewPasswordWorks() {
    RegisteredUser user = registerUser();
    String newPassword = "N3w-secret-password";

    given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "userId", user.userId().toString(),
            "currentPassword", user.password(),
            "newPassword", newPassword,
            "confirmNewPassword", newPassword))
        .when()
        .put("/auth/change-password")
        .then()
        .statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", user.email(), "password", newPassword))
        .when()
        .post("/auth/login")
        .then()
        .statusCode(200)
        .body("token", notNullValue());

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", user.email(), "password", user.password()))
        .when()
        .post("/auth/login")
        .then()
        .statusCode(401);
  }

  @Test
  void changePassword_wrongCurrentPassword_returns401() {
    RegisteredUser user = registerUser();

    given()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "userId", user.userId().toString(),
            "currentPassword", "incorrect-current",
            "newPassword", "N3w-secret-password",
            "confirmNewPassword", "N3w-secret-password"))
        .when()
        .put("/auth/change-password")
        .then()
        .statusCode(401)
        .body("error", equalTo("INVALID_CREDENTIALS"));
  }

  @Test
  void createTenant_authenticated_returns201WithSessionAndApiKey() {
    RegisteredUser user = registerUser();
    String tenantName = uniqueTenantName();

    given()
        .header("Authorization", "Bearer " + user.token())
        .contentType(ContentType.JSON)
        .body(Map.of("tenantName", tenantName))
        .when()
        .post("/auth/tenants")
        .then()
        .statusCode(201)
        .body("session.tenantName", equalTo(tenantName))
        .body("session.role", equalTo("ADMIN"))
        .body("session.token", notNullValue())
        .body("apiKey", notNullValue());
  }

  @Test
  void createTenant_duplicateName_returns409() {
    RegisteredUser user = registerUser();

    given()
        .header("Authorization", "Bearer " + user.token())
        .contentType(ContentType.JSON)
        .body(Map.of("tenantName", user.tenantName()))
        .when()
        .post("/auth/tenants")
        .then()
        .statusCode(409)
        .body("error", equalTo("TENANT_NAME_TAKEN"));
  }

  @Test
  void listMyMemberships_returnsInitialTenant() {
    RegisteredUser user = registerUser();

    given()
        .header("Authorization", "Bearer " + user.token())
        .when()
        .get("/auth/me/memberships")
        .then()
        .statusCode(200)
        .body("memberships", hasSize(1))
        .body("memberships[0].tenantId", equalTo(user.tenantId().toString()))
        .body("memberships[0].tenantName", equalTo(user.tenantName()))
        .body("memberships[0].role", equalTo("ADMIN"));
  }

  private UUID createSecondTenant(RegisteredUser user) {
    String tenantId = given()
        .header("Authorization", "Bearer " + user.token())
        .contentType(ContentType.JSON)
        .body(Map.of("tenantName", uniqueTenantName()))
        .when()
        .post("/auth/tenants")
        .then()
        .statusCode(201)
        .extract()
        .path("session.tenantId");
    return UUID.fromString(tenantId);
  }
}
