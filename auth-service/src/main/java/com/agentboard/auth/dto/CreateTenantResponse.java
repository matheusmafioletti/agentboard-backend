package com.agentboard.auth.dto;

/** Response for POST /auth/tenants including session and onboarding artifacts. */
public record CreateTenantResponse(
    SessionResponse session,
    String apiKey
) {}
