package com.agentboard.board.service;

import com.agentboard.board.domain.Stage;
import com.agentboard.board.dto.ArtifactResponse;
import com.agentboard.board.dto.FeatureCardResponse;
import com.agentboard.board.dto.TaskResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes board domain events to all WebSocket subscribers on the tenant-scoped STOMP topic
 * {@code /topic/tenant/{tenantId}/board-events}.
 */
@Service
public class BoardEventPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  /** Creates the publisher with the STOMP messaging template. */
  public BoardEventPublisher(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Publishes a CARD_MOVED event to all connected clients of the given tenant.
   *
   * @param tenantId     the tenant that owns the card
   * @param cardId       the card that was moved
   * @param fromColumnId the previous column
   * @param toColumnId   the target column
   * @param toStage      the workflow stage of the target column
   * @param displayOrder the card's new position within the target column
   */
  public void publishCardMoved(UUID tenantId, UUID cardId, UUID fromColumnId,
      UUID toColumnId, Stage toStage, int displayOrder) {
    Map<String, Object> event = new HashMap<>();
    event.put("type", "CARD_MOVED");
    event.put("featureCardId", cardId.toString());
    event.put("fromColumnId", fromColumnId.toString());
    event.put("toColumnId", toColumnId.toString());
    event.put("toStage", toStage.name());
    event.put("displayOrder", displayOrder);
    send(tenantId, event);
  }

  /**
   * Publishes a CARD_CREATED event after a new feature card is added to the board.
   *
   * @param tenantId the tenant that owns the card
   * @param card     the full card response
   */
  public void publishCardCreated(UUID tenantId, FeatureCardResponse card) {
    Map<String, Object> event = new HashMap<>();
    event.put("type", "CARD_CREATED");
    event.put("featureCard", card);
    send(tenantId, event);
  }

  /**
   * Publishes a CARD_UPDATED event after a feature card's fields are modified.
   *
   * @param tenantId the tenant that owns the card
   * @param card     the full updated card response
   */
  public void publishCardUpdated(UUID tenantId, FeatureCardResponse card) {
    Map<String, Object> event = new HashMap<>();
    event.put("type", "CARD_UPDATED");
    event.put("featureCardId", card.id().toString());
    event.put("title", card.title());
    event.put("description", card.description());
    event.put("reExecutionPending", card.reExecutionPending());
    event.put("updatedAt", card.updatedAt());
    send(tenantId, event);
  }

  /**
   * Publishes a CARD_DELETED event after a feature card is removed.
   *
   * @param tenantId the tenant that owns the card
   * @param cardId   the id of the deleted card
   */
  public void publishCardDeleted(UUID tenantId, UUID cardId) {
    Map<String, Object> event = new HashMap<>();
    event.put("type", "CARD_DELETED");
    event.put("featureCardId", cardId.toString());
    send(tenantId, event);
  }

  /**
   * Publishes a TASKS_CREATED event after a batch of tasks is added to a feature card.
   *
   * @param tenantId      the tenant that owns the card
   * @param featureCardId the card the tasks belong to
   * @param tasks         the newly created task responses
   */
  public void publishTasksCreated(UUID tenantId, UUID featureCardId, List<TaskResponse> tasks) {
    Map<String, Object> event = new HashMap<>();
    event.put("type", "TASKS_CREATED");
    event.put("featureCardId", featureCardId.toString());
    event.put("tasks", tasks);
    send(tenantId, event);
  }

  /**
   * Publishes a TASK_UPDATED event after a task status changes.
   *
   * @param tenantId      the tenant that owns the card
   * @param featureCardId the card the task belongs to
   * @param task          the updated task response
   */
  public void publishTaskUpdated(UUID tenantId, UUID featureCardId, TaskResponse task) {
    Map<String, Object> event = new HashMap<>();
    event.put("type", "TASK_UPDATED");
    event.put("featureCardId", featureCardId.toString());
    event.put("task", task);
    send(tenantId, event);
  }

  /**
   * Publishes an ARTIFACT_ADDED event after a SpecKit artifact is appended.
   *
   * @param tenantId      the tenant that owns the card
   * @param featureCardId the card the artifact belongs to
   * @param artifact      the new artifact response
   */
  public void publishArtifactAdded(UUID tenantId, UUID featureCardId, ArtifactResponse artifact) {
    Map<String, Object> event = new HashMap<>();
    event.put("type", "ARTIFACT_ADDED");
    event.put("featureCardId", featureCardId.toString());
    event.put("artifact", artifact);
    send(tenantId, event);
  }

  private void send(UUID tenantId, Map<String, Object> event) {
    messagingTemplate.convertAndSend(
        "/topic/tenant/" + tenantId + "/board-events", event);
  }
}
