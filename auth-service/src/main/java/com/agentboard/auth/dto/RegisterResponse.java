package com.agentboard.auth.dto;

import java.util.UUID;

/** Response body for a successful POST /auth/register. */
public record RegisterResponse(
    UUID userId,
    UUID tenantId,
    String token,
    String apiKey,
    BoardInfo board
) {}
