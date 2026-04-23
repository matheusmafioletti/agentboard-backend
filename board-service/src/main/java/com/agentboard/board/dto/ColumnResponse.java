package com.agentboard.board.dto;

import com.agentboard.board.domain.Stage;
import java.util.List;
import java.util.UUID;

/** Representation of a board column returned to clients. */
public record ColumnResponse(
    UUID id,
    String name,
    Stage stage,
    int displayOrder,
    List<FeatureCardSummary> featureCards
) {

  /** Minimal feature card fields returned within a column listing. */
  public record FeatureCardSummary(
      UUID id,
      String title,
      String description,
      boolean reExecutionPending,
      int taskCount,
      int completedTaskCount,
      int displayOrder,
      String createdAt,
      String updatedAt
  ) {}
}
