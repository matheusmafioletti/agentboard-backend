package com.agentboard.board.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP WebSocket broker configuration for the two-board real-time update model.
 *
 * <p>Clients connect to {@code /ws} and subscribe to project-scoped topics:
 * <ul>
 *   <li>{@code /topic/projects/{projectId}/features} — Feature Board stage changes</li>
 *   <li>{@code /topic/projects/{projectId}/user-stories} — US Board stage changes</li>
 * </ul>
 *
 * <p>Authentication is enforced on STOMP CONNECT frames by {@link TenantWebSocketInterceptor}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class BoardWebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final TenantWebSocketInterceptor tenantWebSocketInterceptor;
  private final String allowedOrigins;

  /** Creates the config with the STOMP auth interceptor and the allowed CORS origin. */
  public BoardWebSocketConfig(
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
        .setAllowedOrigins(allowedOrigins.split(","))
        .withSockJS();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(tenantWebSocketInterceptor);
  }
}
