package com.agentboard.board.dto;

import java.util.List;
import java.util.UUID;

/** Full board representation returned by GET /api/boards/current. */
public record BoardResponse(
    UUID id,
    String name,
    UUID tenantId,
    List<ColumnResponse> columns
) {}
