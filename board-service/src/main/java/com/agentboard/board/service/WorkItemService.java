package com.agentboard.board.service;

import com.agentboard.board.domain.WorkItem;
import com.agentboard.board.event.WorkItemMovedEvent;
import com.agentboard.board.repository.WorkItemRepository;
import com.agentboard.commons.domain.FeatureStage;
import com.agentboard.commons.domain.TaskStatus;
import com.agentboard.commons.domain.UserStoryStage;
import com.agentboard.commons.domain.WorkItemType;
import com.agentboard.commons.exceptions.ResourceNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Business logic for the unified WorkItem lifecycle.
 *
 * <p>Hierarchy constraints (FEATURE→USER_STORY→TASK), status validation per type, and
 * auto-only stage enforcement are all applied here before persisting.
 */
@Service
public class WorkItemService {

  private static final Set<String> FEATURE_STATUSES = Arrays.stream(FeatureStage.values())
      .map(Enum::name).collect(Collectors.toUnmodifiableSet());

  private static final Set<String> USER_STORY_STATUSES = Arrays.stream(UserStoryStage.values())
      .map(Enum::name).collect(Collectors.toUnmodifiableSet());

  private static final Set<String> TASK_STATUSES = Arrays.stream(TaskStatus.values())
      .map(Enum::name).collect(Collectors.toUnmodifiableSet());

  private final WorkItemRepository workItemRepository;
  private final ApplicationEventPublisher eventPublisher;

  /** Creates the service backed by the given repository. */
  public WorkItemService(WorkItemRepository workItemRepository,
      ApplicationEventPublisher eventPublisher) {
    this.workItemRepository = workItemRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Returns work items for a project, with optional type/parentId/status filters.
   *
   * @param tenantId  the owning tenant
   * @param projectId the owning project
   * @param type      optional type filter
   * @param parentId  optional parent filter
   * @param status    optional status filter
   * @return filtered list
   */
  @Transactional(readOnly = true)
  public List<WorkItem> listWorkItems(UUID tenantId, UUID projectId,
      WorkItemType type, UUID parentId, String status) {
    return workItemRepository.findFiltered(projectId, tenantId, type, parentId, status);
  }

  /**
   * Returns a single work item with full detail by id, scoped to tenant.
   *
   * @param tenantId   the owning tenant
   * @param workItemId the work item identifier
   * @return the work item
   * @throws ResourceNotFoundException if not found or tenant mismatch
   */
  @Transactional(readOnly = true)
  public WorkItem getWorkItemDetail(UUID tenantId, UUID workItemId) {
    return requireWorkItem(workItemId, tenantId);
  }

  /**
   * Creates a new work item after validating type, parentId, and status constraints.
   *
   * @param tenantId    the owning tenant
   * @param projectId   the owning project
   * @param type        the item type
   * @param title       non-blank title
   * @param description optional description
   * @param parentId    null for FEATURE; required for USER_STORY and TASK
   * @param priority    item priority (defaults to 5)
   * @return the persisted work item
   */
  @Transactional
  public WorkItem createWorkItem(UUID tenantId, UUID projectId, WorkItemType type,
      String title, String description, UUID parentId, int priority) {
    validateParent(type, parentId, tenantId);
    WorkItem item = new WorkItem(projectId, tenantId, type, title, description, parentId, priority);
    return workItemRepository.save(item);
  }

  /**
   * Updates mutable fields (title and/or description) of a work item.
   *
   * @param tenantId   the owning tenant
   * @param workItemId the work item to patch
   * @param title      optional new title
   * @param description optional new description
   * @return the updated work item
   */
  @Transactional
  public WorkItem patchWorkItem(UUID tenantId, UUID workItemId, String title, String description) {
    WorkItem item = requireWorkItem(workItemId, tenantId);
    item.patch(title, description);
    return workItemRepository.save(item);
  }

  /**
   * Moves a work item to a new status after validating the status is valid for its type.
   *
   * <p>Auto-only statuses (FEATURE: IN_DEVELOPMENT, PR_REVIEW; USER_STORY: DONE) are rejected
   * when set via this method. Use the event listener for auto-transitions.
   *
   * @param tenantId   the owning tenant
   * @param workItemId the work item to move
   * @param newStatus  the target status string
   * @return the updated work item
   */
  @Transactional
  public WorkItem moveStatus(UUID tenantId, UUID workItemId, String newStatus) {
    WorkItem item = requireWorkItem(workItemId, tenantId);
    validateStatus(item.getType(), newStatus);
    rejectAutoOnlyStatus(item.getType(), newStatus);
    String oldStatus = item.getStatus();
    item.transitionTo(newStatus);
    WorkItem saved = workItemRepository.save(item);
    eventPublisher.publishEvent(new WorkItemMovedEvent(
        saved.getId(), saved.getType(), oldStatus, newStatus,
        saved.getParentId(), saved.getProjectId(), saved.getTenantId()
    ));
    return saved;
  }

  /**
   * Auto-transitions a work item to a status without the auto-only guard.
   *
   * <p>IMPORTANT: Only callable from event listeners, not exposed via the REST API.
   *
   * @param tenantId   the owning tenant
   * @param workItemId the work item to transition
   * @param newStatus  the target status (may be an auto-only value)
   * @return the updated work item
   */
  @Transactional
  public WorkItem autoTransition(UUID tenantId, UUID workItemId, String newStatus) {
    WorkItem item = requireWorkItem(workItemId, tenantId);
    validateStatus(item.getType(), newStatus);
    String oldStatus = item.getStatus();
    item.transitionTo(newStatus);
    WorkItem saved = workItemRepository.save(item);
    eventPublisher.publishEvent(new WorkItemMovedEvent(
        saved.getId(), saved.getType(), oldStatus, newStatus,
        saved.getParentId(), saved.getProjectId(), saved.getTenantId()
    ));
    return saved;
  }

  /**
   * Validates the parent constraint for the given type and parentId.
   *
   * <p>FEATURE must have no parent. USER_STORY must have a FEATURE parent.
   * TASK must have a USER_STORY parent. Throws 400 on any violation.
   *
   * @param type     the work item type being created
   * @param parentId the provided parent identifier
   * @param tenantId the owning tenant (used to load the parent for type verification)
   */
  public void validateParent(WorkItemType type, UUID parentId, UUID tenantId) {
    switch (type) {
      case FEATURE -> {
        if (parentId != null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "FEATURE items must not have a parent (PARENT_FORBIDDEN)");
        }
      }
      case USER_STORY -> {
        if (parentId == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "USER_STORY requires a parentId pointing to a FEATURE (PARENT_REQUIRED)");
        }
        WorkItem parent = workItemRepository.findByIdAndTenantId(parentId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent " + parentId + " not found"));
        if (parent.getType() != WorkItemType.FEATURE) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "INVALID_PARENT_TYPE: USER_STORY parent must be a FEATURE");
        }
      }
      case TASK -> {
        if (parentId == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "TASK requires a parentId pointing to a USER_STORY (PARENT_REQUIRED)");
        }
        WorkItem parent = workItemRepository.findByIdAndTenantId(parentId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent " + parentId + " not found"));
        if (parent.getType() != WorkItemType.USER_STORY) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "INVALID_PARENT_TYPE: TASK parent must be a USER_STORY");
        }
      }
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Unknown type: " + type);
    }
  }

  private void validateStatus(WorkItemType type, String status) {
    Set<String> validStatuses = switch (type) {
      case FEATURE -> FEATURE_STATUSES;
      case USER_STORY -> USER_STORY_STATUSES;
      case TASK -> TASK_STATUSES;
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Unknown type: " + type);
    };
    if (!validStatuses.contains(status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "INVALID_STATUS: '" + status + "' is not valid for type " + type);
    }
  }

  private void rejectAutoOnlyStatus(WorkItemType type, String status) {
    if (type == WorkItemType.FEATURE) {
      FeatureStage stage = FeatureStage.valueOf(status);
      if (stage.isAutoOnly()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "AUTO_ONLY_STATUS: '" + status + "' is managed automatically");
      }
    }
    if (type == WorkItemType.USER_STORY && "DONE".equals(status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "AUTO_ONLY_STATUS: DONE is set automatically when all child TASKs are CLOSED");
    }
  }

  private WorkItem requireWorkItem(UUID workItemId, UUID tenantId) {
    return workItemRepository.findByIdAndTenantId(workItemId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("WorkItem " + workItemId + " not found"));
  }
}
