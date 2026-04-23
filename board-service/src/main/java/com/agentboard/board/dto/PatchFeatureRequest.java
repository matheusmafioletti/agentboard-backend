package com.agentboard.board.dto;

import jakarta.validation.constraints.Size;

/** Request body for PATCH /api/features/{id}. All fields are optional; null means unchanged. */
public record PatchFeatureRequest(
    @Size(min = 1, max = 255) String title,
    @Size(max = 10000) String description,
    Boolean reExecutionPending
) {}
