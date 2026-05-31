package com.agentboard.auth.integration.api;

import static io.restassured.RestAssured.given;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.agentboard.auth.integration.AbstractAuthIntegrationTest;

/** Integration tests for PUT /auth/change-password. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerChangePasswordIT extends AbstractAuthIntegrationTest {

  @LocalServerPort
  int port;

  private String registeredUserId;
  private String registeredEmail;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registerDatasource(registry);
    registry.add("app.invite-base-url", () -> "http://localhost:5173");
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    registeredEmail = "changepwd-" + UUID.randomUUID() + "@example.com";
    Map<?, ?> response = given()
        .contentType(ContentType.JSON)
        .body(String.format("""
            {
              "name": "Change Pwd User",
              "email": "%s",
              "password": "originalPass1",
              "tenantName": "Change Pwd Corp %s"
            }
            """, registeredEmail, UUID.randomUUID().toString().substring(0, 8)))
        .post("/auth/register")
        .then()
        .statusCode(201)
        .extract()
        .as(Map.class);

    registeredUserId = response.get("userId").toString();
  }

  @Test
  void changePassword_validRequest_returns204() {
    given()
        .contentType(ContentType.JSON)
        .body(String.format("""
            {
              "userId": "%s",
              "currentPassword": "originalPass1",
              "newPassword": "newSecurePass2",
              "confirmNewPassword": "newSecurePass2"
            }
            """, registeredUserId))
    .when()
        .put("/auth/change-password")
    .then()
        .statusCode(204);
  }

  @Test
  void changePassword_wrongCurrentPassword_returns401() {
    given()
        .contentType(ContentType.JSON)
        .body(String.format("""
            {
              "userId": "%s",
              "currentPassword": "wrongPassword",
              "newPassword": "newSecurePass2",
              "confirmNewPassword": "newSecurePass2"
            }
            """, registeredUserId))
    .when()
        .put("/auth/change-password")
    .then()
        .statusCode(401);
  }

  @Test
  void changePassword_passwordMismatch_returns400() {
    given()
        .contentType(ContentType.JSON)
        .body(String.format("""
            {
              "userId": "%s",
              "currentPassword": "originalPass1",
              "newPassword": "newSecurePass2",
              "confirmNewPassword": "differentPass3"
            }
            """, registeredUserId))
    .when()
        .put("/auth/change-password")
    .then()
        .statusCode(400);
  }

  @Test
  void changePassword_afterSuccess_canLoginWithNewPassword() {
    given()
        .contentType(ContentType.JSON)
        .body(String.format("""
            {
              "userId": "%s",
              "currentPassword": "originalPass1",
              "newPassword": "newSecurePass2",
              "confirmNewPassword": "newSecurePass2"
            }
            """, registeredUserId))
        .put("/auth/change-password");

    given()
        .contentType(ContentType.JSON)
        .body(String.format("""
            {
              "email": "%s",
              "password": "newSecurePass2"
            }
            """, registeredEmail))
    .when()
        .post("/auth/login")
    .then()
        .statusCode(200);
  }
}
