package com.agentboard.board.unit.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentboard.board.websocket.TenantWebSocketInterceptor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Unit tests for {@link TenantWebSocketInterceptor}.
 *
 * <p>Covers JWT validation on STOMP CONNECT frames and pass-through behaviour for all other
 * frame types (the interceptor performs no per-subscription authorization).
 */
class TenantWebSocketInterceptorTest {

  private static final String SECRET = "test-secret-must-be-at-least-32-characters-long";

  private TenantWebSocketInterceptor interceptor;
  private MessageChannel channel;

  @BeforeEach
  void setUp() {
    interceptor = new TenantWebSocketInterceptor(SECRET);
    channel = Mockito.mock(MessageChannel.class);
  }

  @Test
  void connect_withValidJwt_storesTenantIdInSessionAttributes() {
    UUID tenantId = UUID.randomUUID();
    Map<String, Object> sessionAttributes = new HashMap<>();
    Message<byte[]> message = connectMessage(
        "Bearer " + buildJwt(tenantId, SECRET, 3_600_000), sessionAttributes);

    Message<?> result = interceptor.preSend(message, channel);

    assertThat(result).isSameAs(message);
    assertThat(sessionAttributes).containsEntry("tenantId", tenantId);
  }

  @Test
  void connect_withMissingAuthorizationHeader_isRejected() {
    Message<byte[]> message = connectMessage(null, new HashMap<>());

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessageContaining("Missing or malformed Authorization header");
  }

  @Test
  void connect_withMalformedAuthorizationHeader_isRejected() {
    Message<byte[]> message = connectMessage("Token abc", new HashMap<>());

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessageContaining("Missing or malformed Authorization header");
  }

  @Test
  void connect_withWrongSignature_isRejected() {
    String foreignSecret = "another-secret-that-is-32-characters-long!!";
    Message<byte[]> message = connectMessage(
        "Bearer " + buildJwt(UUID.randomUUID(), foreignSecret, 3_600_000), new HashMap<>());

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessageContaining("WebSocket authentication failed");
  }

  @Test
  void connect_withExpiredJwt_isRejected() {
    Message<byte[]> message = connectMessage(
        "Bearer " + buildJwt(UUID.randomUUID(), SECRET, -60_000), new HashMap<>());

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessageContaining("WebSocket authentication failed");
  }

  @Test
  void subscribe_framesPassThroughWithoutAuthentication() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/topic/projects/" + UUID.randomUUID() + "/board");
    Message<byte[]> message =
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
  }

  private static Message<byte[]> connectMessage(
      String authorizationHeader, Map<String, Object> sessionAttributes) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setSessionAttributes(sessionAttributes);
    if (authorizationHeader != null) {
      accessor.setNativeHeader("Authorization", authorizationHeader);
    }
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private static String buildJwt(UUID tenantId, String secret, long ttlMs) {
    return Jwts.builder()
        .subject(UUID.randomUUID().toString())
        .claim("tenantId", tenantId.toString())
        .claim("roles", new String[]{"USER"})
        .expiration(new Date(System.currentTimeMillis() + ttlMs))
        .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }
}
