package com.agentboard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/invites/{token}/identify. */
public record IdentifyInviteRequest(
    @NotBlank @Email String email
) {}
