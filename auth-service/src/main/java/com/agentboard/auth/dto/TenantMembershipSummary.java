package com.agentboard.auth.dto;

import com.agentboard.auth.domain.MembershipRole;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Summary of a user's membership in a workspace. */
public record TenantMembershipSummary(
    UUID tenantId,
    String tenantName,
    MembershipRole role,
    OffsetDateTime joinedAt
) {}
