package com.agentboard.auth.dto;

import com.agentboard.auth.domain.MembershipRole;
import java.util.UUID;

/** JWT session issued after successful authentication for a specific tenant. */
public record SessionResponse(
    String token,
    UUID userId,
    UUID tenantId,
    String tenantName,
    String email,
    String name,
    MembershipRole role
) {}
