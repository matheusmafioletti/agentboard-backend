package com.agentboard.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/features. */
public record CreateFeatureRequest(
    @NotBlank @Size(min = 1, max = 255) String title,
    @Size(max = 10000) String description
) {}
