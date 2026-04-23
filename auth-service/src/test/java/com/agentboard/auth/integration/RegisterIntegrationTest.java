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

/** Integration tests for POST /auth/register. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RegisterIntegrationTest {

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
  }

  @Test
  void register_happyPath_returns201WithJwtAndApiKey() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "Test User",
              "email": "happy@example.com",
              "password": "secret123",
              "tenantName": "Happy Corp"
            }
            """)
    .when()
        .post("/auth/register")
    .then()
        .statusCode(201)
        .body("token", notNullValue())
        .body("apiKey", notNullValue())
        .body("userId", notNullValue())
        .body("tenantId", notNullValue())
        .body("board.id", notNullValue())
        .body("board.name", notNullValue());
  }

  @Test
  void register_duplicateEmail_returns409() {
    String firstBody = """
        {
          "name": "User A",
          "email": "dup@example.com",
          "password": "secret123",
          "tenantName": "Corp A"
        }
        """;
    given().contentType(ContentType.JSON).body(firstBody).post("/auth/register")
        .then().statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "User B",
              "email": "dup@example.com",
              "password": "secret123",
              "tenantName": "Corp B"
            }
            """)
    .when()
        .post("/auth/register")
    .then()
        .statusCode(409);
  }

  @Test
  void register_missingRequiredFields_returns400() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"email\": \"nopwd@example.com\"}")
    .when()
        .post("/auth/register")
    .then()
        .statusCode(400);
  }
}
