package com.agentboard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for POST /auth/tenants. */
public record CreateTenantRequest(
    @NotBlank @Size(min = 1, max = 100) String tenantName
) {}
