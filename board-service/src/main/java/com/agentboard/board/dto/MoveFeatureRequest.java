package com.agentboard.board.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request body for {@code PATCH /api/features/{id}/move}.
 *
 * @param targetColumnId the column to move the card into
 * @param displayOrder   zero-based position within the target column
 */
public record MoveFeatureRequest(
    @NotNull UUID targetColumnId,
    @NotNull @Min(0) Integer displayOrder) {
}
