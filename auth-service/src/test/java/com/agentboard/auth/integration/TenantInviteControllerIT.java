package com.agentboard.auth.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.agentboard.auth.domain.InviteStatus;
import com.agentboard.auth.repository.TenantInviteRepository;
import com.agentboard.auth.repository.TenantMembershipRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link com.agentboard.auth.controller.TenantInviteController}.
 *
 * <p>Covers the admin invite lifecycle (create, list, cancel) and the public token-based
 * preview/identify/accept flows.
 */
class TenantInviteControllerIT extends AbstractAuthIntegrationTest {

  @Autowired
  TenantInviteRepository inviteRepository;

  @Autowired
  TenantMembershipRepository membershipRepository;

  @Autowired
  UserAccountRepository userAccountRepository;

  @Autowired
  TestTenantSupport testTenantSupport;

  @Test
  void createInvite_withAutomationOnNonTestTenant_returns403() {
    RegisteredUser admin = registerUser();

    given()
        .header("Authorization", "Bearer " + admin.token())
        .header("X-Data-Source", "automation")
        .contentType(ContentType.JSON)
        .body(Map.of("email", uniqueEmail()))
        .when()
        .post("/auth/tenants/" + admin.tenantId() + "/invites")
        .then()
        .statusCode(403)
        .body("error", equalTo("DATA_SOURCE_NOT_ALLOWED"));
  }

  @Test
  void createInvite_withAutomationOnTestTenant_persistsAutomationDataSource() {
    RegisteredUser admin = registerUser();
    testTenantSupport.markTestTenant(admin.tenantId());
    String inviteeEmail = uniqueEmail();

    String inviteId = given()
        .header("Authorization", "Bearer " + admin.token())
        .header("X-Data-Source", "automation")
        .contentType(ContentType.JSON)
        .body(Map.of("email", inviteeEmail))
        .when()
        .post("/auth/tenants/" + admin.tenantId() + "/invites")
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    assertThat(testTenantSupport.inviteDataSource(UUID.fromString(inviteId)))
        .isEqualTo("automation");
  }

  @Test
  void acceptInvite_withSeedOnTestTenant_persistsSeedOnUserAndMembership() {
    RegisteredUser admin = registerUser();
    testTenantSupport.markTestTenant(admin.tenantId());
    String inviteeEmail = uniqueEmail();
    CreatedInvite invite = createInvite(admin, inviteeEmail);

    given()
        .header("X-Data-Source", "seed")
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Invited User", "password", DEFAULT_PASSWORD))
        .when()
        .post("/auth/invites/" + invite.token() + "/accept")
        .then()
        .statusCode(200);

    var createdUser = userAccountRepository.findByEmail(inviteeEmail).orElseThrow();
    assertThat(testTenantSupport.userDataSource(createdUser.getId())).isEqualTo("seed");
    assertThat(testTenantSupport.membershipDataSource(createdUser.getId(), admin.tenantId()))
        .isEqualTo("seed");
  }

  @Test
  void createInvite_asAdmin_returns201AndPersistsPendingInvite() {
    RegisteredUser admin = registerUser();
    String inviteeEmail = uniqueEmail();

    String inviteId = given()
        .header("Authorization", "Bearer " + admin.token())
        .contentType(ContentType.JSON)
        .body(Map.of("email", inviteeEmail))
        .when()
        .post("/auth/tenants/" + admin.tenantId() + "/invites")
        .then()
        .statusCode(201)
        .body("email", equalTo(inviteeEmail))
        .body("status", equalTo("PENDING"))
        .body("inviteUrl", containsString("/invite/"))
        .extract()
        .path("id");

    var persisted = inviteRepository.findById(UUID.fromString(inviteId)).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(InviteStatus.PENDING);
    assertThat(persisted.getTenantId()).isEqualTo(admin.tenantId());
  }

  @Test
  void createInvite_duplicatePendingEmail_returns409() {
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
  void cancelInvite_pendingInvite_returns204AndPersistsCancelledStatus() {
    RegisteredUser admin = registerUser();
    CreatedInvite invite = createInvite(admin, uniqueEmail());

    given()
        .header("Authorization", "Bearer " + admin.token())
        .when()
        .delete("/auth/tenants/" + admin.tenantId() + "/invites/" + invite.id())
        .then()
        .statusCode(204);

    var persisted = inviteRepository.findById(invite.id()).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(InviteStatus.CANCELLED);
  }

  @Test
  void listInvites_asAdmin_returns200WithCreatedInvites() {
    RegisteredUser admin = registerUser();
    String inviteeEmail = uniqueEmail();
    createInvite(admin, inviteeEmail);

    given()
        .header("Authorization", "Bearer " + admin.token())
        .when()
        .get("/auth/tenants/" + admin.tenantId() + "/invites")
        .then()
        .statusCode(200)
        .body("invites", hasSize(1))
        .body("invites[0].email", equalTo(inviteeEmail))
        .body("invites[0].status", equalTo("PENDING"));
  }

  @Test
  void previewInvite_byToken_publicEndpointReturns200() {
    RegisteredUser admin = registerUser();
    String inviteeEmail = uniqueEmail();
    CreatedInvite invite = createInvite(admin, inviteeEmail);

    given()
        .when()
        .get("/auth/invites/" + invite.token())
        .then()
        .statusCode(200)
        .body("tenantName", equalTo(admin.tenantName()))
        .body("email", equalTo(inviteeEmail))
        .body("status", equalTo("PENDING"))
        .body("requiresRegistration", equalTo(true));
  }

  @Test
  void identifyInvite_newAndExistingAccounts_reportAccountExistence() {
    RegisteredUser admin = registerUser();
    String newEmail = uniqueEmail();
    CreatedInvite newUserInvite = createInvite(admin, newEmail);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", newEmail))
        .when()
        .post("/auth/invites/" + newUserInvite.token() + "/identify")
        .then()
        .statusCode(200)
        .body("tenantName", equalTo(admin.tenantName()))
        .body("accountExists", equalTo(false));

    RegisteredUser existing = registerUser();
    CreatedInvite existingUserInvite = createInvite(admin, existing.email());

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", existing.email()))
        .when()
        .post("/auth/invites/" + existingUserInvite.token() + "/identify")
        .then()
        .statusCode(200)
        .body("accountExists", equalTo(true));
  }

  @Test
  void acceptInvite_newUser_createsAccountAndMembershipAndReturnsJwt() {
    RegisteredUser admin = registerUser();
    String inviteeEmail = uniqueEmail();
    CreatedInvite invite = createInvite(admin, inviteeEmail);

    String token = given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Invited User", "password", DEFAULT_PASSWORD))
        .when()
        .post("/auth/invites/" + invite.token() + "/accept")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("tenantId", equalTo(admin.tenantId().toString()))
        .body("role", equalTo("USER"))
        .extract()
        .path("token");

    assertThat(tenantIdClaim(token)).isEqualTo(admin.tenantId());
    var createdUser = userAccountRepository.findByEmail(inviteeEmail).orElseThrow();
    assertThat(membershipRepository.existsByUserIdAndTenantId(
        createdUser.getId(), admin.tenantId())).isTrue();
  }

  @Test
  void acceptInvite_existingUser_addsMembershipToInvitedTenant() {
    RegisteredUser admin = registerUser();
    RegisteredUser existing = registerUser();
    CreatedInvite invite = createInvite(admin, existing.email());

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", existing.email(), "password", existing.password()))
        .when()
        .post("/auth/invites/" + invite.token() + "/accept")
        .then()
        .statusCode(200)
        .body("tenantId", equalTo(admin.tenantId().toString()))
        .body("role", equalTo("USER"));

    assertThat(membershipRepository.findByUserId(existing.userId())).hasSize(2);
  }

  @Test
  void acceptInvite_alreadyAccepted_returns410() {
    RegisteredUser admin = registerUser();
    CreatedInvite invite = createInvite(admin, uniqueEmail());

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Invited User", "password", DEFAULT_PASSWORD))
        .when()
        .post("/auth/invites/" + invite.token() + "/accept")
        .then()
        .statusCode(200);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Invited User", "password", DEFAULT_PASSWORD))
        .when()
        .post("/auth/invites/" + invite.token() + "/accept")
        .then()
        .statusCode(410)
        .body("error", equalTo("INVITE_GONE"));
  }

  @Test
  void acceptInvite_cancelledInvite_returns410() {
    RegisteredUser admin = registerUser();
    CreatedInvite invite = createInvite(admin, uniqueEmail());

    given()
        .header("Authorization", "Bearer " + admin.token())
        .when()
        .delete("/auth/tenants/" + admin.tenantId() + "/invites/" + invite.id())
        .then()
        .statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Invited User", "password", DEFAULT_PASSWORD))
        .when()
        .post("/auth/invites/" + invite.token() + "/accept")
        .then()
        .statusCode(410)
        .body("error", equalTo("INVITE_GONE"));
  }

  /** Invite created via the admin API together with the raw token parsed from its URL. */
  record CreatedInvite(UUID id, String token) {}

  private CreatedInvite createInvite(RegisteredUser admin, String inviteeEmail) {
    var extracted = given()
        .header("Authorization", "Bearer " + admin.token())
        .contentType(ContentType.JSON)
        .body(Map.of("email", inviteeEmail))
        .when()
        .post("/auth/tenants/" + admin.tenantId() + "/invites")
        .then()
        .statusCode(201)
        .extract();
    String inviteUrl = extracted.path("inviteUrl");
    String token = inviteUrl.substring(inviteUrl.lastIndexOf("/invite/") + "/invite/".length());
    return new CreatedInvite(UUID.fromString(extracted.path("id")), token);
  }
}
