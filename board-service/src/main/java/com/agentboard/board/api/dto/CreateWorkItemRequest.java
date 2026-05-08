package com.agentboard.board.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Request body for creating a single work item. */
public record CreateWorkItemRequest(
    @NotNull String type,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 10000) String description,
    UUID parentId,
    @Positive int priority,
    UUID assigneeId
) {}
