package com.agentboard.board.dto;

import java.util.UUID;

/** Response DTO for a single task. */
public record TaskResponse(
    UUID id,
    UUID featureCardId,
    String title,
    String description,
    String priority,
    boolean completed,
    boolean blocked,
    String blockedReason,
    String completedAt,
    String createdAt
) {}
