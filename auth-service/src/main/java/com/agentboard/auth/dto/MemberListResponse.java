package com.agentboard.auth.dto;

import java.util.List;

/** Response wrapper for GET /auth/tenants/{tenantId}/members. */
public record MemberListResponse(List<MemberResponse> members) {}
