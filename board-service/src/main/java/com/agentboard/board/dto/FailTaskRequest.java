package com.agentboard.board.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for marking a task as blocked. */
public record FailTaskRequest(
    @NotBlank(message = "reason is required")
    String reason
) {}
