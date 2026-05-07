package com.agentboard.auth.dto;

import java.util.UUID;

/** Response body for a successful POST /auth/login. */
public record LoginResponse(String token, UUID userId, UUID tenantId, String email) {}
