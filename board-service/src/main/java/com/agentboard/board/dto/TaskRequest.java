package com.agentboard.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A single task definition within a create-tasks request. */
public record TaskRequest(
    @NotBlank(message = "title is required")
    @Size(max = 500, message = "title must not exceed 500 characters")
    String title,

    String description,

    @NotBlank(message = "priority is required")
    @Pattern(regexp = "P1|P2|P3", message = "priority must be P1, P2, or P3")
    String priority
) {}
