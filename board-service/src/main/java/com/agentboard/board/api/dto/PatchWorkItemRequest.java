package com.agentboard.board.api.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Request body for updating mutable fields of a work item. */
public record PatchWorkItemRequest(
    @Size(max = 255) String title,
    @Size(max = 10000) String description,
    AssigneeUpdate assignee
) {

  /**
   * Discriminated union for assignee update: null = no change; {clear:true} = unassign;
   * {id: UUID} = set assignee.
   */
  public record AssigneeUpdate(UUID id, boolean clear) {}
}
