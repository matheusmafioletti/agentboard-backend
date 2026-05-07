package com.agentboard.auth.integration.api;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.agentboard.auth.dto.BoardInfo;
import com.agentboard.auth.service.BoardServiceClient;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

/** Integration tests for PUT /auth/change-password. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerChangePasswordIT {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("agentboard")
          .withUsername("agentboard")
          .withPassword("agentboard");

  @LocalServerPort
  int port;

  @MockBean
  BoardServiceClient boardServiceClient;

  private String registeredUserId;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("board-service.url", () -> "http://localhost:9999");
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    when(boardServiceClient.createBoard(any(UUID.class), anyString()))
        .thenReturn(new BoardInfo(UUID.randomUUID(), "Test Board"));

    Map<?, ?> response = given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "Change Pwd User",
              "email": "changepwd@example.com",
              "password": "originalPass1",
              "tenantName": "Change Pwd Corp"
            }
            """)
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
        .body("""
            {
              "email": "changepwd@example.com",
              "password": "newSecurePass2"
            }
            """)
    .when()
        .post("/auth/login")
    .then()
        .statusCode(200);
  }
}
