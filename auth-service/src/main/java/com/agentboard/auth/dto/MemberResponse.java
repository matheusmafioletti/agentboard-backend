package com.agentboard.auth.dto;

import com.agentboard.auth.domain.MembershipRole;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Member entry in tenant member list responses. */
public record MemberResponse(
    UUID userId,
    String name,
    String email,
    MembershipRole role,
    OffsetDateTime joinedAt
) {}
