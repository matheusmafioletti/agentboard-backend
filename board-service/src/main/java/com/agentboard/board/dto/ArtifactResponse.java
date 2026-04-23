package com.agentboard.board.dto;

import java.util.UUID;

/** Response DTO for a single artifact. */
public record ArtifactResponse(
    UUID id,
    UUID featureCardId,
    String command,
    String content,
    String agentIdentifier,
    String createdAt
) {}
