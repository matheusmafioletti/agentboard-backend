package com.agentboard.board.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.board.domain.Board;
import com.agentboard.board.repository.BoardRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for WebSocket real-time board updates.
 * Verifies that board mutations are published as STOMP events to tenant-scoped topics.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class WebSocketIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("agentboard")
          .withUsername("agentboard")
          .withPassword("agentboard");

  @LocalServerPort
  int port;

  @Value("${jwt.secret}")
  String jwtSecret;

  @Autowired
  BoardRepository boardRepository;

  private UUID tenantId;
  private String jwt;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    tenantId = UUID.randomUUID();
    jwt = buildJwt(tenantId, UUID.randomUUID());
    boardRepository.save(new Board(tenantId, "Test Board"));
  }

  @Test
  void cardMove_publishesCardMovedEvent_toSubscriber() throws Exception {
    String cardId = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "title": "WS Test Card" }
            """)
        .post("/api/features")
        .then().statusCode(201)
        .extract().path("id");

    String fromColumnId = given()
        .header("Authorization", "Bearer " + jwt)
        .get("/api/features/{id}", cardId)
        .then().statusCode(200)
        .extract().path("columnId");

    String specifyColumnId = given()
        .header("Authorization", "Bearer " + jwt)
        .get("/api/boards/current")
        .then().statusCode(200)
        .extract().path("columns.find { it.stage == 'SPECIFY' }.id");

    WebSocketStompClient stompClient = buildStompClient();
    CompletableFuture<Map<String, Object>> eventFuture = new CompletableFuture<>();
    String wsUrl = "ws://localhost:" + port + "/ws";

    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + jwt);

    CompletableFuture<StompSession> sessionFuture = new CompletableFuture<>();
    stompClient.connect(wsUrl, new WebSocketHttpHeaders(), connectHeaders,
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            session.subscribe("/topic/tenant/" + tenantId + "/board-events",
                new StompFrameHandler() {
                  @Override
                  public Type getPayloadType(StompHeaders headers) {
                    return Object.class;
                  }

                  @Override
                  @SuppressWarnings("unchecked")
                  public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload instanceof Map<?, ?> map) {
                      eventFuture.complete((Map<String, Object>) map);
                    }
                  }
                });
            sessionFuture.complete(session);
          }

          @Override
          public void handleTransportError(StompSession session, Throwable exception) {
            sessionFuture.completeExceptionally(exception);
          }
        });

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwt)
        .body("""
            { "targetColumnId": "%s", "displayOrder": 0 }
            """.formatted(specifyColumnId))
        .patch("/api/features/{id}/move", cardId)
        .then().statusCode(200);

    Map<String, Object> event = eventFuture.get(5, TimeUnit.SECONDS);
    assertThat(event.get("type")).isEqualTo("CARD_MOVED");
    assertThat(event.get("featureCardId")).isEqualTo(cardId);

    final StompSession session = sessionFuture.get(5, TimeUnit.SECONDS);
    session.disconnect();
  }

  @Test
  void connect_withInvalidJwt_failsTransport() throws Exception {
    WebSocketStompClient stompClient = buildStompClient();
    String wsUrl = "ws://localhost:" + port + "/ws";

    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer invalid.jwt.token");

    CompletableFuture<Throwable> errorFuture = new CompletableFuture<>();
    stompClient.connect(wsUrl, new WebSocketHttpHeaders(), connectHeaders,
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            errorFuture.completeExceptionally(new AssertionError("Should not connect"));
          }

          @Override
          public void handleTransportError(StompSession session, Throwable exception) {
            errorFuture.complete(exception);
          }

          @Override
          public void handleException(StompSession session, StompCommand command,
              StompHeaders headers, byte[] payload, Throwable exception) {
            errorFuture.complete(exception);
          }
        });

    Throwable error = errorFuture.get(5, TimeUnit.SECONDS);
    assertThat(error).isNotNull();
  }

  @Test
  void crossTenant_subscriberDoesNotReceiveOtherTenantCardMove() throws Exception {
    UUID tenantB = UUID.randomUUID();
    String jwtB = buildJwt(tenantB, UUID.randomUUID());
    boardRepository.save(new Board(tenantB, "Tenant B Board"));

    String cardId = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwtB)
        .body("""
            { "title": "Tenant B Card" }
            """)
        .post("/api/features")
        .then().statusCode(201)
        .extract().path("id");

    String specifyColB = given()
        .header("Authorization", "Bearer " + jwtB)
        .get("/api/boards/current")
        .then().statusCode(200)
        .extract().path("columns.find { it.stage == 'SPECIFY' }.id");

    WebSocketStompClient stompClient = buildStompClient();
    String wsUrl = "ws://localhost:" + port + "/ws";

    StompHeaders connectHeadersA = new StompHeaders();
    connectHeadersA.add("Authorization", "Bearer " + jwt);

    CompletableFuture<Map<String, Object>> unexpectedEvent = new CompletableFuture<>();
    CompletableFuture<StompSession> connectFuture = new CompletableFuture<>();
    stompClient.connect(wsUrl, new WebSocketHttpHeaders(), connectHeadersA,
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            session.subscribe("/topic/tenant/" + tenantId + "/board-events",
                new StompFrameHandler() {
                  @Override
                  public Type getPayloadType(StompHeaders headers) {
                    return Object.class;
                  }

                  @Override
                  @SuppressWarnings("unchecked")
                  public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload instanceof Map<?, ?> map) {
                      unexpectedEvent.complete((Map<String, Object>) map);
                    }
                  }
                });
            connectFuture.complete(session);
          }
        });

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwtB)
        .body("""
            { "targetColumnId": "%s", "displayOrder": 0 }
            """.formatted(specifyColB))
        .patch("/api/features/{id}/move", cardId)
        .then().statusCode(200);

    boolean receivedEvent = false;
    try {
      unexpectedEvent.get(2, TimeUnit.SECONDS);
      receivedEvent = true;
    } catch (java.util.concurrent.TimeoutException e) {
      receivedEvent = false;
    }

    assertThat(receivedEvent).as("Tenant A should NOT receive Tenant B events").isFalse();
    final StompSession sessionA = connectFuture.get(5, TimeUnit.SECONDS);
    sessionA.disconnect();
  }

  private WebSocketStompClient buildStompClient() {
    WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    client.setMessageConverter(
        new org.springframework.messaging.converter.MappingJackson2MessageConverter());
    return client;
  }

  private String buildJwt(UUID tenant, UUID userId) {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    return Jwts.builder()
        .subject(userId.toString())
        .claim("tenantId", tenant.toString())
        .claim("roles", new String[]{"USER"})
        .expiration(new Date(System.currentTimeMillis() + 3_600_000))
        .signWith(Keys.hmacShaKeyFor(keyBytes))
        .compact();
  }
}
