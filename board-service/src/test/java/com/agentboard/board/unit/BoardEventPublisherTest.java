package com.agentboard.board.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.agentboard.board.domain.Stage;
import com.agentboard.board.dto.ArtifactResponse;
import com.agentboard.board.dto.FeatureCardResponse;
import com.agentboard.board.dto.TaskResponse;
import com.agentboard.board.service.BoardEventPublisher;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Unit tests for {@link BoardEventPublisher}: verifies that each publish method sends to
 * {@code /topic/tenant/{tenantId}/board-events} and that the payload contains the correct
 * {@code type} field.
 */
class BoardEventPublisherTest {

  private SimpMessagingTemplate messagingTemplate;
  private BoardEventPublisher publisher;

  @BeforeEach
  void setUp() {
    messagingTemplate = mock(SimpMessagingTemplate.class);
    publisher = new BoardEventPublisher(messagingTemplate);
  }

  @Test
  void publishCardMoved_sendsToTenantTopic() {
    UUID tenantId = UUID.randomUUID();
    UUID cardId = UUID.randomUUID();
    UUID fromCol = UUID.randomUUID();
    UUID toCol = UUID.randomUUID();

    publisher.publishCardMoved(tenantId, cardId, fromCol, toCol, Stage.SPECIFY, 0);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messagingTemplate).convertAndSend(
        eq("/topic/tenant/" + tenantId + "/board-events"),
        payloadCaptor.capture());
    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
    assertThat(payload.get("type")).isEqualTo("CARD_MOVED");
    assertThat(payload.get("featureCardId")).isEqualTo(cardId.toString());
  }

  @Test
  void publishTasksCreated_sendsToTenantTopic() {
    UUID tenantId = UUID.randomUUID();
    UUID featureCardId = UUID.randomUUID();

    publisher.publishTasksCreated(tenantId, featureCardId, List.of());

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messagingTemplate).convertAndSend(
        eq("/topic/tenant/" + tenantId + "/board-events"),
        payloadCaptor.capture());
    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
    assertThat(payload.get("type")).isEqualTo("TASKS_CREATED");
  }

  @Test
  void publishTaskUpdated_sendsToTenantTopic() {
    UUID tenantId = UUID.randomUUID();
    UUID featureCardId = UUID.randomUUID();
    TaskResponse task = new TaskResponse(
        UUID.randomUUID(), featureCardId, "Title", null, "P1",
        false, false, null, null, null);

    publisher.publishTaskUpdated(tenantId, featureCardId, task);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messagingTemplate).convertAndSend(
        eq("/topic/tenant/" + tenantId + "/board-events"),
        payloadCaptor.capture());
    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
    assertThat(payload.get("type")).isEqualTo("TASK_UPDATED");
  }

  @Test
  void publishArtifactAdded_sendsToTenantTopic() {
    UUID tenantId = UUID.randomUUID();
    UUID featureCardId = UUID.randomUUID();
    ArtifactResponse artifact = new ArtifactResponse(
        UUID.randomUUID(), featureCardId, "specify", "content", null, null);

    publisher.publishArtifactAdded(tenantId, featureCardId, artifact);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messagingTemplate).convertAndSend(
        eq("/topic/tenant/" + tenantId + "/board-events"),
        payloadCaptor.capture());
    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
    assertThat(payload.get("type")).isEqualTo("ARTIFACT_ADDED");
  }

  @Test
  void publishCardCreated_sendsToTenantTopic() {
    UUID tenantId = UUID.randomUUID();
    FeatureCardResponse card = new FeatureCardResponse(
        UUID.randomUUID(), UUID.randomUUID(), tenantId,
        "Title", null, false, 0, null, null, List.of(), List.of(), List.of());

    publisher.publishCardCreated(tenantId, card);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messagingTemplate).convertAndSend(
        eq("/topic/tenant/" + tenantId + "/board-events"),
        payloadCaptor.capture());
    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
    assertThat(payload.get("type")).isEqualTo("CARD_CREATED");
  }

  @Test
  void publishCardUpdated_sendsToTenantTopic() {
    UUID tenantId = UUID.randomUUID();
    FeatureCardResponse card = new FeatureCardResponse(
        UUID.randomUUID(), UUID.randomUUID(), tenantId,
        "Title", null, false, 0, null, null, List.of(), List.of(), List.of());

    publisher.publishCardUpdated(tenantId, card);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messagingTemplate).convertAndSend(
        eq("/topic/tenant/" + tenantId + "/board-events"),
        payloadCaptor.capture());
    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
    assertThat(payload.get("type")).isEqualTo("CARD_UPDATED");
  }

  @Test
  void publishCardDeleted_sendsToTenantTopic() {
    UUID tenantId = UUID.randomUUID();
    UUID cardId = UUID.randomUUID();

    publisher.publishCardDeleted(tenantId, cardId);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messagingTemplate).convertAndSend(
        eq("/topic/tenant/" + tenantId + "/board-events"),
        payloadCaptor.capture());
    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
    assertThat(payload.get("type")).isEqualTo("CARD_DELETED");
    assertThat(payload.get("featureCardId")).isEqualTo(cardId.toString());
  }

  @Test
  void allPublishMethods_includeTopicWithTenantId() {
    UUID tenantId = UUID.randomUUID();
    String expectedTopic = "/topic/tenant/" + tenantId + "/board-events";

    publisher.publishCardDeleted(tenantId, UUID.randomUUID());

    verify(messagingTemplate).convertAndSend(eq(expectedTopic), any(Object.class));
  }
}
