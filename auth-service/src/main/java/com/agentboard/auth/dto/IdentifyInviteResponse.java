package com.agentboard.auth.dto;

/** Response for POST /auth/invites/{token}/identify. */
public record IdentifyInviteResponse(
    String tenantName,
    String inviteEmail,
    boolean accountExists
) {}
