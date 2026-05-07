package com.agentboard.board.security;

import java.util.UUID;

/**
 * Security principal representing a project-authenticated MCP client.
 *
 * <p>Carries both the projectId and tenantId so controllers can retrieve both
 * from the security context without a database lookup.
 *
 * @param projectId the authenticated project
 * @param tenantId  the tenant that owns the project
 */
public record ProjectPrincipal(UUID projectId, UUID tenantId) {}
