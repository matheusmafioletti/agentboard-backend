package com.agentboard.board.dto;

import java.util.List;

/** Response wrapper for a list of feature cards. */
public record FeaturesListResponse(List<FeatureCardResponse> features) {}
