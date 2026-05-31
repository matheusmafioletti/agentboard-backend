package com.agentboard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request body for POST /auth/select-tenant. */
public record SelectTenantRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotNull UUID tenantId
) {}
