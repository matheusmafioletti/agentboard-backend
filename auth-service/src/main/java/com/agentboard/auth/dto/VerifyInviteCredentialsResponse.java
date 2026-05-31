package com.agentboard.auth.dto;

/** Response for POST /auth/invites/{token}/verify-credentials. */
public record VerifyInviteCredentialsResponse(
    String name,
    String email
) {}
