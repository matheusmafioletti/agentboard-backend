package com.agentboard.board.dto;

/** Response returned after completing a task. */
public record CompleteTaskResponse(
    TaskResponse task,
    FeatureCardResponse featureCard,
    boolean autoMovedToReview
) {}
