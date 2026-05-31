package com.agentboard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Request body for POST /auth/invites/{token}/accept. */
public record AcceptInviteRequest(
    @Size(max = 255) String name,
    @Size(min = 8, max = 255) String password,
    @Email String email
) {}
