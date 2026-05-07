package com.agentboard.board.api.dto;

import jakarta.validation.constraints.Size;

/** Request body for updating mutable fields of a work item. */
public record PatchWorkItemRequest(
    @Size(max = 255) String title,
    @Size(max = 10000) String description
) {}
