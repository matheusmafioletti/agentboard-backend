package com.agentboard.board.unit.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentboard.board.domain.WorkItem;
import com.agentboard.board.event.WorkItemMovedEvent;
import com.agentboard.board.event.WorkItemTransitionEventListener;
import com.agentboard.board.repository.WorkItemRepository;
import com.agentboard.board.service.WorkItemService;
import com.agentboard.board.websocket.BoardEventPublisher;
import com.agentboard.commons.domain.WorkItemType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkItemTransitionEventListenerTest {

  @Mock
  private WorkItemService workItemService;

  @Mock
  private WorkItemRepository workItemRepository;

  @Mock
  private BoardEventPublisher eventPublisher;

  @InjectMocks
  private WorkItemTransitionEventListener listener;

  @Test
  void taskClosed_allSiblingsClosed_autoTransitionsParentUserStoryToDone() {
    UUID tenantId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID parentUsId = UUID.randomUUID();
    UUID taskId = UUID.randomUUID();

    when(workItemRepository.countByParentIdAndStatusNot(parentUsId, "CLOSED")).thenReturn(0L);

    WorkItemMovedEvent event = new WorkItemMovedEvent(
        taskId, WorkItemType.TASK, "ACTIVE", "CLOSED", parentUsId, projectId, tenantId);

    listener.onWorkItemMoved(event);

    verify(workItemService).autoTransition(tenantId, parentUsId, "DONE");
  }

  @Test
  void taskClosed_otherTasksStillOpen_noAutoTransition() {
    UUID tenantId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID parentUsId = UUID.randomUUID();

    when(workItemRepository.countByParentIdAndStatusNot(parentUsId, "CLOSED")).thenReturn(2L);

    WorkItemMovedEvent event = new WorkItemMovedEvent(
        UUID.randomUUID(), WorkItemType.TASK, "ACTIVE", "CLOSED", parentUsId, projectId, tenantId);

    listener.onWorkItemMoved(event);

    verify(workItemService, never()).autoTransition(any(), any(), any());
  }

  @Test
  void userStoryInProgress_firstUs_autoTransitionsParentFeatureToInDevelopment() {
    UUID tenantId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID parentFeatureId = UUID.randomUUID();

    WorkItem feature = stubFeature(parentFeatureId, tenantId, "READY");
    when(workItemRepository.findByIdAndTenantId(parentFeatureId, tenantId))
        .thenReturn(Optional.of(feature));
    when(workItemRepository.countByParentIdAndStatus(parentFeatureId, "IN_PROGRESS"))
        .thenReturn(1L);

    WorkItemMovedEvent event = new WorkItemMovedEvent(
        UUID.randomUUID(), WorkItemType.USER_STORY, "READY", "IN_PROGRESS",
        parentFeatureId, projectId, tenantId);

    listener.onWorkItemMoved(event);

    verify(workItemService).autoTransition(tenantId, parentFeatureId, "IN_DEVELOPMENT");
  }

  @Test
  void userStoryDone_allUsDone_autoTransitionsParentFeatureToPrReview() {
    UUID tenantId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID parentFeatureId = UUID.randomUUID();

    WorkItem feature = stubFeature(parentFeatureId, tenantId, "IN_DEVELOPMENT");
    when(workItemRepository.findByIdAndTenantId(parentFeatureId, tenantId))
        .thenReturn(Optional.of(feature));
    when(workItemRepository.countByParentIdAndStatusNot(parentFeatureId, "DONE")).thenReturn(0L);

    WorkItemMovedEvent event = new WorkItemMovedEvent(
        UUID.randomUUID(), WorkItemType.USER_STORY, "IN_PROGRESS", "DONE",
        parentFeatureId, projectId, tenantId);

    listener.onWorkItemMoved(event);

    verify(workItemService).autoTransition(tenantId, parentFeatureId, "PR_REVIEW");
  }

  @Test
  void userStoryDone_featureNotInDevelopment_noAutoTransition() {
    UUID tenantId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID parentFeatureId = UUID.randomUUID();

    WorkItem feature = stubFeature(parentFeatureId, tenantId, "SPECIFY");
    when(workItemRepository.findByIdAndTenantId(parentFeatureId, tenantId))
        .thenReturn(Optional.of(feature));
    when(workItemRepository.countByParentIdAndStatusNot(parentFeatureId, eq("DONE")))
        .thenReturn(0L);

    WorkItemMovedEvent event = new WorkItemMovedEvent(
        UUID.randomUUID(), WorkItemType.USER_STORY, "IN_PROGRESS", "DONE",
        parentFeatureId, projectId, tenantId);

    listener.onWorkItemMoved(event);

    verify(workItemService, never()).autoTransition(any(), any(), any());
  }

  private WorkItem stubFeature(UUID id, UUID tenantId, String status) {
    WorkItem item = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5);
    item.transitionTo(status);
    return item;
  }
}
