package com.agentboard.auth.dto;

import com.agentboard.auth.domain.MembershipRole;
import java.util.UUID;

/** Response body for a successful POST /auth/register. */
public record RegisterResponse(
    UUID userId,
    UUID tenantId,
    String tenantName,
    String token,
    MembershipRole role,
    String apiKey
) {}
