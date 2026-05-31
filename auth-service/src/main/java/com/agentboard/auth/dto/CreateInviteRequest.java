package com.agentboard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/tenants/{tenantId}/invites. */
public record CreateInviteRequest(
    @NotBlank @Email String email
) {}
