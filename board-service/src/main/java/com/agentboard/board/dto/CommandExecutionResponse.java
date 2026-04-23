package com.agentboard.board.dto;

import java.util.UUID;

/** Response DTO for a single command execution audit record. */
public record CommandExecutionResponse(
    UUID id,
    UUID featureCardId,
    String command,
    String status,
    String agentIdentifier,
    String errorMessage,
    String startedAt,
    String finishedAt,
    Long durationMs
) {}
