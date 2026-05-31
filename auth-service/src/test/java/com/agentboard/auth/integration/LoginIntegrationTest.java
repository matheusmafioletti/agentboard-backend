package com.agentboard.auth.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Integration tests for POST /auth/login. */
@org.springframework.boot.test.context.SpringBootTest(
    webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginIntegrationTest extends AbstractAuthIntegrationTest {

  @LocalServerPort
  int port;

  private String loginEmail;

  @DynamicPropertySource
  static void registerPort(DynamicPropertyRegistry registry) {
    registerDatasource(registry);
    registry.add("app.invite-base-url", () -> "http://localhost:5173");
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    loginEmail = "login-" + UUID.randomUUID() + "@example.com";
    given()
        .contentType(ContentType.JSON)
        .body(String.format("""
            {
              "name": "Login User",
              "email": "%s",
              "password": "password123",
              "tenantName": "Login Corp %s"
            }
            """, loginEmail, UUID.randomUUID().toString().substring(0, 8)))
        .post("/auth/register");
  }

  @Test
  void login_validCredentials_returns200WithJwt() {
    given()
        .contentType(ContentType.JSON)
        .body(String.format("""
            {
              "email": "%s",
              "password": "password123"
            }
            """, loginEmail))
    .when()
        .post("/auth/login")
    .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("userId", notNullValue())
        .body("tenantId", notNullValue());
  }

  @Test
  void login_wrongPassword_returns401() {
    given()
        .contentType(ContentType.JSON)
        .body(String.format("""
            {
              "email": "%s",
              "password": "wrongpassword"
            }
            """, loginEmail))
    .when()
        .post("/auth/login")
    .then()
        .statusCode(401);
  }

  @Test
  void login_unknownEmail_returns401() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "email": "nobody@example.com",
              "password": "password123"
            }
            """)
    .when()
        .post("/auth/login")
    .then()
        .statusCode(401);
  }
}
