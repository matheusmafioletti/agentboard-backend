package com.agentboard.board.dto;

import java.util.List;

/** Response returned after creating tasks for a feature card. */
public record CreateTasksResponse(
    List<TaskResponse> tasks,
    FeatureCardResponse featureCard
) {}
