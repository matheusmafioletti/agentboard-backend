package com.agentboard.auth.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
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

import java.util.UUID;

/** Integration tests for POST /auth/login. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LoginIntegrationTest {

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
        .thenReturn(new BoardInfo(UUID.randomUUID(), "My Board"));
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "Login User",
              "email": "login@example.com",
              "password": "password123",
              "tenantName": "Login Corp"
            }
            """)
        .post("/auth/register");
  }

  @Test
  void login_validCredentials_returns200WithJwt() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "email": "login@example.com",
              "password": "password123"
            }
            """)
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
        .body("""
            {
              "email": "login@example.com",
              "password": "wrongpassword"
            }
            """)
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
