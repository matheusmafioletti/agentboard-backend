package com.agentboard.board.dto;

import jakarta.validation.constraints.NotNull;

/** Request body for moving a feature card to a specific workflow stage. */
public record MoveStageRequest(
    @NotNull(message = "stage is required") String stage
) {}
