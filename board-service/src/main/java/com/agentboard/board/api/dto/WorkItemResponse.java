package com.agentboard.board.api.dto;

import com.agentboard.board.domain.WorkItem;
import java.util.UUID;

/** Summary DTO for a WorkItem — no children or nested collections. */
public record WorkItemResponse(
    UUID id,
    UUID projectId,
    UUID tenantId,
    String type,
    String title,
    String description,
    String status,
    UUID parentId,
    int priority,
    int displayOrder,
    String createdAt,
    String updatedAt,
    String displayKey,
    ParentPreviewResponse parentPreview,
    UUID assigneeId
) {

  /** Creates a summary response from the entity without optional parent hydrate. */
  public static WorkItemResponse from(WorkItem wi) {
    return from(wi, null);
  }

  /**
   * Creates a summary response optionally carrying a parent silhouette.
   *
   * @param wi            persisted item
   * @param parentPreview optional parent summary (must be {@code null} for FEATURE roots)
   */
  public static WorkItemResponse from(WorkItem wi, ParentPreviewResponse parentPreview) {
    return new WorkItemResponse(
        wi.getId(),
        wi.getProjectId(),
        wi.getTenantId(),
        wi.getType().name(),
        wi.getTitle(),
        wi.getDescription(),
        wi.getStatus(),
        wi.getParentId(),
        wi.getPriority(),
        wi.getDisplayOrder(),
        wi.getCreatedAt().toString(),
        wi.getUpdatedAt().toString(),
        wi.getDisplayKey(),
        parentPreview,
        wi.getAssigneeId());
  }
}
