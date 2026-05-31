package com.agentboard.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request body for POST /auth/switch-tenant. */
public record SwitchTenantRequest(
    @NotNull UUID tenantId
) {}
