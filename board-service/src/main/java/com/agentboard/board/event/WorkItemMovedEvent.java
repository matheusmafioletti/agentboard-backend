package com.agentboard.board.event;

import com.agentboard.commons.domain.WorkItemType;
import java.util.UUID;

/**
 * Domain event published after a {@code WorkItem} transitions to a new status.
 *
 * @param workItemId   the identifier of the transitioned item
 * @param workItemType the type of the transitioned item
 * @param oldStatus    the previous status
 * @param newStatus    the new status
 * @param parentId     the parent work item identifier (may be null for FEATURE)
 * @param projectId    the owning project
 * @param tenantId     the owning tenant
 */
public record WorkItemMovedEvent(
    UUID workItemId,
    WorkItemType workItemType,
    String oldStatus,
    String newStatus,
    UUID parentId,
    UUID projectId,
    UUID tenantId
) {
}
