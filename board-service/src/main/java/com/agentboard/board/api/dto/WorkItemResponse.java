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
    String updatedAt
) {

  /** Creates a summary response from the given entity. */
  public static WorkItemResponse from(WorkItem wi) {
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
        wi.getUpdatedAt().toString()
    );
  }
}
