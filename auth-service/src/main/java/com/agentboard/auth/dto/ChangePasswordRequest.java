package com.agentboard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Request body for PUT /auth/change-password. */
public record ChangePasswordRequest(
    @NotNull UUID userId,
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 8) String newPassword,
    @NotBlank String confirmNewPassword
) {}
