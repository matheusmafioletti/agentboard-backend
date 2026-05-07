package com.agentboard.board.event;

import com.agentboard.board.repository.WorkItemRepository;
import com.agentboard.board.service.WorkItemService;
import com.agentboard.board.websocket.BoardEventPublisher;
import com.agentboard.commons.domain.WorkItemType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Applies automatic parent status transitions after a WorkItem status change commits.
 *
 * <p>Auto-transition rules per data-model.md:
 * <ul>
 *   <li>TASK → CLOSED: if all sibling TASKs are CLOSED → parent USER_STORY → DONE</li>
 *   <li>USER_STORY → IN_PROGRESS (first): → parent FEATURE → IN_DEVELOPMENT</li>
 *   <li>USER_STORY → DONE (all): → parent FEATURE → PR_REVIEW</li>
 * </ul>
 *
 * <p>Each transition runs in a new transaction so a failed auto-transition does not
 * roll back the already-committed parent move.
 */
@Component
public class WorkItemTransitionEventListener {

  private final WorkItemService workItemService;
  private final WorkItemRepository workItemRepository;
  private final BoardEventPublisher eventPublisher;

  /** Creates the listener with its required collaborators. */
  public WorkItemTransitionEventListener(
      WorkItemService workItemService,
      WorkItemRepository workItemRepository,
      BoardEventPublisher eventPublisher) {
    this.workItemService = workItemService;
    this.workItemRepository = workItemRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Handles a WorkItem status change event after the originating transaction commits.
   *
   * @param event the published event containing work item identity and status transition
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onWorkItemMoved(WorkItemMovedEvent event) {
    if (event.workItemType() == WorkItemType.TASK && "CLOSED".equals(event.newStatus())) {
      handleTaskClosed(event);
    } else if (event.workItemType() == WorkItemType.USER_STORY) {
      if ("IN_PROGRESS".equals(event.newStatus())) {
        handleUserStoryInProgress(event);
      } else if ("DONE".equals(event.newStatus())) {
        handleUserStoryDone(event);
      }
    }
  }

  private void handleTaskClosed(WorkItemMovedEvent event) {
    if (event.parentId() == null) {
      return;
    }
    long openTasks = workItemRepository.countByParentIdAndStatusNot(event.parentId(), "CLOSED");
    if (openTasks == 0) {
      workItemService.autoTransition(event.tenantId(), event.parentId(), "DONE");
      eventPublisher.publishWorkItemStatusChanged(
          event.projectId(), event.parentId(), WorkItemType.USER_STORY, "DONE");
    }
  }

  private void handleUserStoryInProgress(WorkItemMovedEvent event) {
    if (event.parentId() == null) {
      return;
    }
    long inProgressCount = workItemRepository.countByParentIdAndStatus(
        event.parentId(), "IN_PROGRESS");
    workItemRepository.findByIdAndTenantId(event.parentId(), event.tenantId())
        .ifPresent(feature -> {
          if (inProgressCount == 1 && "READY".equals(feature.getStatus())) {
            workItemService.autoTransition(event.tenantId(), event.parentId(), "IN_DEVELOPMENT");
            eventPublisher.publishWorkItemStatusChanged(
                event.projectId(), event.parentId(), WorkItemType.FEATURE, "IN_DEVELOPMENT");
          }
        });
  }

  private void handleUserStoryDone(WorkItemMovedEvent event) {
    if (event.parentId() == null) {
      return;
    }
    long notDoneCount = workItemRepository.countByParentIdAndStatusNot(event.parentId(), "DONE");
    workItemRepository.findByIdAndTenantId(event.parentId(), event.tenantId())
        .ifPresent(feature -> {
          if (notDoneCount == 0 && "IN_DEVELOPMENT".equals(feature.getStatus())) {
            workItemService.autoTransition(event.tenantId(), event.parentId(), "PR_REVIEW");
            eventPublisher.publishWorkItemStatusChanged(
                event.projectId(), event.parentId(), WorkItemType.FEATURE, "PR_REVIEW");
          }
        });
  }
}
