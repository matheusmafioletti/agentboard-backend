package com.agentboard.board.dto;

import java.util.UUID;

/** Response returned from the internal POST /internal/boards endpoint. */
public record BoardCreatedResponse(UUID id, String name) {}
