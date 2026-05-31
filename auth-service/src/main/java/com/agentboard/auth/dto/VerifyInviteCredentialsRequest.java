package com.agentboard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for POST /auth/invites/{token}/verify-credentials. */
public record VerifyInviteCredentialsRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 255) String password
) {}
