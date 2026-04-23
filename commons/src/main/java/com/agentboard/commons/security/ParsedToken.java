package com.agentboard.commons.security;

import java.util.List;
import java.util.UUID;

/**
 * Immutable data extracted from a validated JWT, containing the principal's identity and roles.
 */
public record ParsedToken(UUID userId, UUID tenantId, List<String> roles) {}
