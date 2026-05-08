package com.agentboard.board.api.dto;

import com.agentboard.board.domain.WorkItem;
import java.util.UUID;

/** Slim parent envelope for Kanban payloads when {@code includeParent=true}. */
public record ParentPreviewResponse(
    UUID id,
    String type,
    String title,
    String displayKey
) {

  /** Maps a hydrated parent entity to the wire shape. */
  public static ParentPreviewResponse fromWorkItem(WorkItem parent) {
    return new ParentPreviewResponse(
        parent.getId(),
        parent.getType().name(),
        parent.getTitle(),
        parent.getDisplayKey());
  }
}
