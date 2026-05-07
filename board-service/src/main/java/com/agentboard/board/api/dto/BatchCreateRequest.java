package com.agentboard.board.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Request body for creating multiple work items of the same type in one request. */
public record BatchCreateRequest(
    @NotNull String type,
    @NotNull UUID parentId,
    @NotNull List<BatchItem> items
) {

  /** A single item entry within a batch creation request. */
  public record BatchItem(
      @NotBlank @Size(max = 255) String title,
      @Size(max = 10000) String description,
      @Positive int priority
  ) {}
}
