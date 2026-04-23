package com.agentboard.board.websocket;

import com.agentboard.commons.security.InvalidTokenException;
import com.agentboard.commons.security.JwtValidator;
import com.agentboard.commons.security.ParsedToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP channel interceptor that validates the JWT on CONNECT frames.
 *
 * <p>On successful validation the {@code tenantId} is stored in the STOMP session attributes
 * so that subsequent subscriptions and messages can access it without re-parsing the token.
 * Invalid or missing tokens cause a {@link MessageDeliveryException}, which Spring STOMP
 * translates into an ERROR frame sent back to the client.
 */
@Component
public class TenantWebSocketInterceptor implements ChannelInterceptor {

  private final JwtValidator jwtValidator;

  /** Creates the interceptor with the JWT signing secret. */
  public TenantWebSocketInterceptor(@Value("${jwt.secret}") String jwtSecret) {
    this.jwtValidator = new JwtValidator(jwtSecret);
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
      return message;
    }

    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new MessageDeliveryException(message, "Missing or malformed Authorization header");
    }

    String token = authHeader.substring("Bearer ".length());
    try {
      ParsedToken parsed = jwtValidator.validate(token);
      accessor.getSessionAttributes().put("tenantId", parsed.tenantId());
    } catch (InvalidTokenException e) {
      throw new MessageDeliveryException(
          message, "WebSocket authentication failed: " + e.getMessage());
    }

    return message;
  }
}
