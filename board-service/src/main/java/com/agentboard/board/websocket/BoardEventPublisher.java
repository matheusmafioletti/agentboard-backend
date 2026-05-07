package com.agentboard.board.websocket;

import com.agentboard.commons.domain.FeatureStage;
import com.agentboard.commons.domain.UserStoryStage;
import com.agentboard.commons.domain.WorkItemType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes board domain events to project-scoped STOMP topics after every stage transition.
 *
 * <p>Feature Board events go to {@code /topic/projects/{projectId}/features}.
 * US Board events go to {@code /topic/projects/{projectId}/user-stories}.
 */
@Component
public class BoardEventPublisher {

  private static final String TYPE_STAGE_CHANGED = "STAGE_CHANGED";

  private final SimpMessagingTemplate messaging;

  /** Creates the publisher backed by the STOMP messaging template. */
  public BoardEventPublisher(SimpMessagingTemplate messaging) {
    this.messaging = messaging;
  }

  /**
   * Sends a STAGE_CHANGED event for a Feature to its project topic.
   *
   * @param projectId the project owning the feature
   * @param featureId the feature that changed stage
   * @param newStage  the new FeatureStage value
   */
  public void publishFeatureStageChanged(UUID projectId, UUID featureId, FeatureStage newStage) {
    Map<String, Object> payload = buildPayload(TYPE_STAGE_CHANGED, "FEATURE",
        featureId, newStage.name());
    messaging.convertAndSend(featureTopic(projectId), payload);
  }

  /**
   * Sends a FEATURE_CREATED event to the project's feature topic.
   *
   * @param projectId the project owning the feature
   * @param featureId the newly created feature
   * @param stage     the initial stage (always BACKLOG)
   */
  public void publishFeatureCreated(UUID projectId, UUID featureId, FeatureStage stage) {
    Map<String, Object> payload = buildPayload("FEATURE_CREATED", "FEATURE",
        featureId, stage.name());
    messaging.convertAndSend(featureTopic(projectId), payload);
  }

  /**
   * Sends a STAGE_CHANGED event for a UserStory to its project topic.
   *
   * @param projectId     the project owning the user story
   * @param userStoryId   the user story that changed stage
   * @param newStage      the new UserStoryStage value
   */
  public void publishUserStoryStageChanged(UUID projectId, UUID userStoryId,
      UserStoryStage newStage) {
    Map<String, Object> payload = buildPayload(TYPE_STAGE_CHANGED, "USER_STORY",
        userStoryId, newStage.name());
    messaging.convertAndSend(userStoryTopic(projectId), payload);
  }

  /**
   * Sends a USER_STORY_CREATED event to the project's user-story topic.
   *
   * @param projectId   the project owning the user story
   * @param userStoryId the newly created user story
   */
  public void publishUserStoryCreated(UUID projectId, UUID userStoryId) {
    Map<String, Object> payload = buildPayload("USER_STORY_CREATED", "USER_STORY",
        userStoryId, UserStoryStage.READY.name());
    messaging.convertAndSend(userStoryTopic(projectId), payload);
  }

  /**
   * Broadcasts a WorkItem status change over the unified board WebSocket topic.
   *
   * @param projectId    the owning project
   * @param workItemId   the transitioned work item
   * @param workItemType the type of the work item
   * @param newStatus    the status it moved to
   */
  public void publishWorkItemStatusChanged(UUID projectId, UUID workItemId,
      WorkItemType workItemType, String newStatus) {
    Map<String, Object> payload = buildPayload(TYPE_STAGE_CHANGED,
        workItemType.name(), workItemId, newStatus);
    messaging.convertAndSend(boardTopic(projectId), payload);
  }

  private static String boardTopic(UUID projectId) {
    return "/topic/projects/" + projectId + "/board";
  }

  private static Map<String, Object> buildPayload(String type, String entityType,
      UUID entityId, String newStage) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("type", type);
    payload.put("entityType", entityType);
    payload.put("entityId", entityId.toString());
    payload.put("newStage", newStage);
    payload.put("timestamp", Instant.now().toString());
    return payload;
  }

  private static String featureTopic(UUID projectId) {
    return "/topic/projects/" + projectId + "/features";
  }

  private static String userStoryTopic(UUID projectId) {
    return "/topic/projects/" + projectId + "/user-stories";
  }
}
