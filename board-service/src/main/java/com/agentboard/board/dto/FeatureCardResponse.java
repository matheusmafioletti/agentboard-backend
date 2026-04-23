package com.agentboard.board.dto;

import java.util.List;
import java.util.UUID;

/** Full feature card representation including tasks, artifacts, and execution history. */
public record FeatureCardResponse(
    UUID id,
    UUID columnId,
    UUID tenantId,
    String title,
    String description,
    boolean reExecutionPending,
    int displayOrder,
    String createdAt,
    String updatedAt,
    List<TaskResponse> tasks,
    List<ArtifactResponse> artifacts,
    List<CommandExecutionResponse> commandExecutions
) {}
