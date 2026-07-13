package com.agentboard.auth.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import com.agentboard.auth.repository.TenantMembershipRepository;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link com.agentboard.auth.controller.TenantMemberController}.
 *
 * <p>Exercises admin-only member listing and revocation, including the last-admin guard and
 * cross-tenant access denial.
 */
class TenantMemberControllerIT extends AbstractAuthIntegrationTest {

  @Autowired
  TenantMembershipRepository membershipRepository;

  @Test
  void listMembers_asAdmin_returns200IncludingSelf() {
    RegisteredUser admin = registerUser();

    given()
        .header("Authorization", "Bearer " + admin.token())
        .when()
        .get("/auth/tenants/" + admin.tenantId() + "/members")
        .then()
        .statusCode(200)
        .body("members", hasSize(1))
        .body("members[0].userId", equalTo(admin.userId().toString()))
        .body("members[0].email", equalTo(admin.email()))
        .body("members[0].role", equalTo("ADMIN"));
  }

  @Test
  void listMembers_asNonAdmin_returns403() {
    RegisteredUser admin = registerUser();
    MemberSession member = inviteAndAcceptNewUser(admin);

    given()
        .header("Authorization", "Bearer " + member.token())
        .when()
        .get("/auth/tenants/" + admin.tenantId() + "/members")
        .then()
        .statusCode(403)
        .body("error", equalTo("FORBIDDEN"));
  }

  @Test
  void revokeMember_regularMember_returns204AndRemovesMembership() {
    RegisteredUser admin = registerUser();
    MemberSession member = inviteAndAcceptNewUser(admin);

    given()
        .header("Authorization", "Bearer " + admin.token())
        .when()
        .delete("/auth/tenants/" + admin.tenantId() + "/members/" + member.userId())
        .then()
        .statusCode(204);

    assertThat(membershipRepository.existsByUserIdAndTenantId(
        member.userId(), admin.tenantId())).isFalse();

    given()
        .header("Authorization", "Bearer " + admin.token())
        .when()
        .get("/auth/tenants/" + admin.tenantId() + "/members")
        .then()
        .statusCode(200)
        .body("members", hasSize(1))
        .body("members.userId", hasItems(admin.userId().toString()));
  }

  @Test
  void revokeMember_lastAdmin_returns409() {
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
  void listMembers_ofForeignTenant_returns403() {
    RegisteredUser adminA = registerUser();
    RegisteredUser adminB = registerUser();

    given()
        .header("Authorization", "Bearer " + adminA.token())
        .when()
        .get("/auth/tenants/" + adminB.tenantId() + "/members")
        .then()
        .statusCode(403)
        .body("error", equalTo("FORBIDDEN"));
  }

  /** Session of a USER-role member created through the invite accept flow. */
  record MemberSession(UUID userId, String token) {}

  private MemberSession inviteAndAcceptNewUser(RegisteredUser admin) {
    String inviteeEmail = uniqueEmail();
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
    String token = inviteUrl.substring(inviteUrl.lastIndexOf("/invite/") + "/invite/".length());

    var extracted = given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Member User", "password", DEFAULT_PASSWORD))
        .when()
        .post("/auth/invites/" + token + "/accept")
        .then()
        .statusCode(200)
        .extract();
    return new MemberSession(
        UUID.fromString(extracted.path("userId")),
        extracted.path("token"));
  }
}
