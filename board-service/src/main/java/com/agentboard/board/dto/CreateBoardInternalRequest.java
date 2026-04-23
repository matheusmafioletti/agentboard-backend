package com.agentboard.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Request body for the internal POST /internal/boards endpoint. */
public record CreateBoardInternalRequest(
    @NotNull UUID tenantId,
    @NotBlank @Size(max = 100) String name
) {}
