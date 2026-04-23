package com.agentboard.board.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures the STOMP WebSocket message broker for board real-time events.
 *
 * <p>Clients connect to {@code /ws}, subscribe to
 * {@code /topic/tenant/{tenantId}/board-events}, and receive board mutation events.
 * Authentication is enforced in {@link TenantWebSocketInterceptor} on CONNECT frames.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final TenantWebSocketInterceptor tenantWebSocketInterceptor;
  private final String allowedOrigins;

  /** Creates the config with the STOMP auth interceptor and the allowed CORS origin. */
  public WebSocketConfig(
      TenantWebSocketInterceptor tenantWebSocketInterceptor,
      @Value("${websocket.allowed-origins}") String allowedOrigins) {
    this.tenantWebSocketInterceptor = tenantWebSocketInterceptor;
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOrigins(allowedOrigins);
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(tenantWebSocketInterceptor);
  }
}
