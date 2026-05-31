package com.agentboard.auth.dto;

import java.util.List;

/** Response wrapper for GET /auth/tenants/{tenantId}/invites. */
public record InviteListResponse(List<InviteResponse> invites) {}
