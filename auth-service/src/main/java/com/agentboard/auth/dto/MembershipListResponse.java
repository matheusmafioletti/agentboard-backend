package com.agentboard.auth.dto;

import java.util.List;

/** Response wrapper for GET /auth/me/memberships. */
public record MembershipListResponse(List<TenantMembershipSummary> memberships) {}
