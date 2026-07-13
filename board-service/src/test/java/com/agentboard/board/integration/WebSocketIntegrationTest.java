package com.agentboard.board.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.restassured.http.ContentType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * End-to-end STOMP WebSocket tests against the running board-service.
 *
 * <p>Connects through the SockJS raw-WebSocket transport ({@code /ws/websocket}), authenticates
 * via the {@code Authorization} native CONNECT header, and asserts that auto-transition events
 * published by {@link com.agentboard.board.event.WorkItemTransitionEventListener} arrive on the
 * project-scoped {@code /topic/projects/{projectId}/board} topic.
 */
class WebSocketIntegrationTest extends AbstractIntegrationTest {

  private WebSocketStompClient stompClient;
  private StompSession sessionA;
  private StompSession sessionB;

  @AfterEach
  void disconnectSessions() {
    if (sessionA != null && sessionA.isConnected()) {
      sessionA.disconnect();
    }
    if (sessionB != null && sessionB.isConnected()) {
      sessionB.disconnect();
    }
    if (stompClient != null) {
      stompClient.stop();
    }
  }

  @Test
  void statusChangeEvent_isDeliveredToProjectTopic_andNotToOtherTenantSubscriber()
      throws Exception {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    String jwtA = buildJwt(tenantA, UUID.randomUUID());
    String jwtB = buildJwt(tenantB, UUID.randomUUID());
    UUID projectA = createProject(tenantA);
    UUID projectB = createProject(tenantB);

    String featureId = createWorkItem(jwtA, projectA,
        Map.of("type", "FEATURE", "title", "WS Feature", "priority", 5));
    String storyId = createWorkItem(jwtA, projectA,
        Map.of("type", "USER_STORY", "title", "WS Story", "parentId", featureId, "priority", 1));
    String taskId = createWorkItem(jwtA, projectA,
        Map.of("type", "TASK", "title", "WS Task", "parentId", storyId, "priority", 1));

    stompClient = newStompClient();
    CompletableFuture<Map<String, Object>> eventA = new CompletableFuture<>();
    CompletableFuture<Map<String, Object>> eventB = new CompletableFuture<>();

    sessionA = connect(stompClient, jwtA);
    subscribe(sessionA, projectA, eventA);
    sessionB = connect(stompClient, jwtB);
    subscribe(sessionB, projectB, eventB);
    Thread.sleep(500);

    given()
        .header("Authorization", "Bearer " + jwtA)
        .contentType(ContentType.JSON)
        .body(Map.of("status", "CLOSED"))
        .when()
        .patch("/api/v1/work-items/" + taskId + "/status")
        .then()
        .statusCode(200);

    Map<String, Object> payload = eventA.get(10, TimeUnit.SECONDS);
    assertThat(payload)
        .containsEntry("type", "STAGE_CHANGED")
        .containsEntry("entityType", "USER_STORY")
        .containsEntry("entityId", storyId)
        .containsEntry("newStage", "DONE");

    assertThatThrownBy(() -> eventB.get(2, TimeUnit.SECONDS))
        .isInstanceOf(java.util.concurrent.TimeoutException.class);
  }

  @Test
  void connect_withInvalidJwt_failsStompHandshake() {
    stompClient = newStompClient();

    assertThatThrownBy(() -> connect(stompClient, "not-a-valid-jwt"))
        .isInstanceOf(Exception.class);
  }

  private WebSocketStompClient newStompClient() {
    WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    client.setMessageConverter(new MappingJackson2MessageConverter());
    return client;
  }

  private StompSession connect(WebSocketStompClient client, String jwt) throws Exception {
    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + jwt);
    return client
        .connectAsync(
            "ws://localhost:" + port + "/ws/websocket",
            new WebSocketHttpHeaders(),
            connectHeaders,
            new StompSessionHandlerAdapter() {})
        .get(10, TimeUnit.SECONDS);
  }

  private void subscribe(
      StompSession session, UUID projectId, CompletableFuture<Map<String, Object>> future) {
    session.subscribe("/topic/projects/" + projectId + "/board", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return Map.class;
      }

      @Override
      @SuppressWarnings("unchecked")
      public void handleFrame(StompHeaders headers, Object payload) {
        future.complete((Map<String, Object>) payload);
      }
    });
  }

  private String createWorkItem(String jwt, UUID projectId, Map<String, ?> body) {
    return given()
        .header("Authorization", "Bearer " + jwt)
        .contentType(ContentType.JSON)
        .queryParam("projectId", projectId)
        .body(body)
        .when()
        .post("/api/v1/work-items")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }
}
