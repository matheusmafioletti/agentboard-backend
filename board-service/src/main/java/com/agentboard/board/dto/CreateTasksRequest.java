package com.agentboard.board.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Request body for creating multiple tasks for a feature card. */
public record CreateTasksRequest(
    @NotEmpty(message = "tasks list must not be empty")
    @Valid
    List<TaskRequest> tasks
) {}
