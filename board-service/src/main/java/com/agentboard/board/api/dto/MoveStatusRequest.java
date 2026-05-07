package com.agentboard.board.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for moving a work item to a new status. */
public record MoveStatusRequest(@NotBlank String status) {}
