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
 * Integration tests for WebSocket tenant isolation.
 * Registers two tenants, verifies that mutations in tenant A's board are not received by tenant B.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TenantIsolationApiTest {

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

  private UUID tenantA;
  private UUID tenantB;
  private String jwtA;
  private String jwtB;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    tenantA = UUID.randomUUID();
    tenantB = UUID.randomUUID();
    jwtA = buildJwt(tenantA, UUID.randomUUID());
    jwtB = buildJwt(tenantB, UUID.randomUUID());
    boardRepository.save(new Board(tenantA, "Board A"));
    boardRepository.save(new Board(tenantB, "Board B"));
  }

  @Test
  void subscriberTenant_doesNotReceive_cardCreateFromOtherTenant() throws Exception {
    WebSocketStompClient stompClient = buildStompClient();
    String wsUrl = "ws://localhost:" + port + "/ws";

    CompletableFuture<Map<String, Object>> unexpectedEvent = new CompletableFuture<>();
    CompletableFuture<StompSession> connectFutureB = new CompletableFuture<>();

    StompHeaders connectHeadersB = new StompHeaders();
    connectHeadersB.add("Authorization", "Bearer " + jwtB);

    stompClient.connect(wsUrl, new WebSocketHttpHeaders(), connectHeadersB,
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            session.subscribe("/topic/tenant/" + tenantB + "/board-events",
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
            connectFutureB.complete(session);
          }
        });

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwtA)
        .body("""
            { "title": "Tenant A Feature" }
            """)
        .post("/api/features")
        .then().statusCode(201);

    boolean receivedByB = false;
    try {
      unexpectedEvent.get(2, TimeUnit.SECONDS);
      receivedByB = true;
    } catch (java.util.concurrent.TimeoutException e) {
      receivedByB = false;
    }

    assertThat(receivedByB)
        .as("Tenant B must not receive events from Tenant A")
        .isFalse();

    final StompSession sessionB = connectFutureB.get(5, TimeUnit.SECONDS);
    sessionB.disconnect();
  }

  @Test
  void tenantA_receives_ownCardCreate() throws Exception {
    WebSocketStompClient stompClient = buildStompClient();
    String wsUrl = "ws://localhost:" + port + "/ws";

    CompletableFuture<Map<String, Object>> cardEvent = new CompletableFuture<>();
    CompletableFuture<StompSession> connectFuture = new CompletableFuture<>();

    StompHeaders connectHeadersA = new StompHeaders();
    connectHeadersA.add("Authorization", "Bearer " + jwtA);

    stompClient.connect(wsUrl, new WebSocketHttpHeaders(), connectHeadersA,
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            session.subscribe("/topic/tenant/" + tenantA + "/board-events",
                new StompFrameHandler() {
                  @Override
                  public Type getPayloadType(StompHeaders headers) {
                    return Object.class;
                  }

                  @Override
                  @SuppressWarnings("unchecked")
                  public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload instanceof Map<?, ?> map) {
                      cardEvent.complete((Map<String, Object>) map);
                    }
                  }
                });
            connectFuture.complete(session);
          }
        });

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwtA)
        .body("""
            { "title": "Own Feature" }
            """)
        .post("/api/features")
        .then().statusCode(201);

    Map<String, Object> event = cardEvent.get(5, TimeUnit.SECONDS);
    assertThat(event.get("type")).isEqualTo("CARD_CREATED");

    final StompSession session = connectFuture.get(5, TimeUnit.SECONDS);
    session.disconnect();
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
