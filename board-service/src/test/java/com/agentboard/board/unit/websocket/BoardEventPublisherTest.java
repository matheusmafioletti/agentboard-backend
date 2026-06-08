package com.agentboard.board.unit.websocket;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.agentboard.board.websocket.BoardEventPublisher;
import com.agentboard.commons.domain.FeatureStage;
import com.agentboard.commons.domain.UserStoryStage;
import com.agentboard.commons.domain.WorkItemType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class BoardEventPublisherTest {

  @Mock
  private SimpMessagingTemplate messaging;

  private BoardEventPublisher boardEventPublisher;

  @BeforeEach
  void setUp() {
    boardEventPublisher = new BoardEventPublisher(messaging);
  }

  @Test
  void shouldPublishToFeatureTopic_whenPublishFeatureStageChanged() {
    UUID projectId = UUID.randomUUID();
    UUID featureId = UUID.randomUUID();

    boardEventPublisher.publishFeatureStageChanged(projectId, featureId, FeatureStage.SPECIFY);

    verify(messaging).convertAndSend(
        eq("/topic/projects/" + projectId + "/features"),
        argThat((Map<String, Object> payload) ->
            "STAGE_CHANGED".equals(payload.get("type"))
                && "FEATURE".equals(payload.get("entityType"))
                && featureId.toString().equals(payload.get("entityId"))
                && "SPECIFY".equals(payload.get("newStage"))
        )
    );
  }

  @Test
  void shouldPublishToFeatureTopic_whenPublishFeatureCreated() {
    UUID projectId = UUID.randomUUID();
    UUID featureId = UUID.randomUUID();

    boardEventPublisher.publishFeatureCreated(projectId, featureId, FeatureStage.BACKLOG);

    verify(messaging).convertAndSend(
        eq("/topic/projects/" + projectId + "/features"),
        argThat((Map<String, Object> payload) ->
            "FEATURE_CREATED".equals(payload.get("type"))
                && "FEATURE".equals(payload.get("entityType"))
                && featureId.toString().equals(payload.get("entityId"))
        )
    );
  }

  @Test
  void shouldPublishToUserStoryTopic_whenPublishUserStoryStageChanged() {
    UUID projectId = UUID.randomUUID();
    UUID usId = UUID.randomUUID();

    boardEventPublisher.publishUserStoryStageChanged(projectId, usId, UserStoryStage.IN_PROGRESS);

    verify(messaging).convertAndSend(
        eq("/topic/projects/" + projectId + "/user-stories"),
        argThat((Map<String, Object> payload) ->
            "STAGE_CHANGED".equals(payload.get("type"))
                && "USER_STORY".equals(payload.get("entityType"))
                && usId.toString().equals(payload.get("entityId"))
                && "IN_PROGRESS".equals(payload.get("newStage"))
        )
    );
  }

  @Test
  void shouldPublishToUserStoryTopic_whenPublishUserStoryCreated() {
    UUID projectId = UUID.randomUUID();
    UUID usId = UUID.randomUUID();

    boardEventPublisher.publishUserStoryCreated(projectId, usId);

    verify(messaging).convertAndSend(
        eq("/topic/projects/" + projectId + "/user-stories"),
        argThat((Map<String, Object> payload) ->
            "USER_STORY_CREATED".equals(payload.get("type"))
                && "USER_STORY".equals(payload.get("entityType"))
        )
    );
  }

  @Test
  void shouldPublishToBoardTopic_whenPublishWorkItemStatusChanged() {
    UUID projectId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();

    boardEventPublisher.publishWorkItemStatusChanged(
        projectId, workItemId, WorkItemType.TASK, "CLOSED");

    verify(messaging).convertAndSend(
        eq("/topic/projects/" + projectId + "/board"),
        argThat((Map<String, Object> payload) ->
            "STAGE_CHANGED".equals(payload.get("type"))
                && "TASK".equals(payload.get("entityType"))
                && workItemId.toString().equals(payload.get("entityId"))
                && "CLOSED".equals(payload.get("newStage"))
        )
    );
  }
}
