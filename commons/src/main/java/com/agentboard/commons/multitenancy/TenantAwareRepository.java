package com.agentboard.commons.multitenancy;

/**
 * Marker interface documenting the contract that all repository implementations
 * MUST include {@code tenantId} as a filter in every query.
 *
 * <p>Repositories that implement this interface signal that they have been reviewed for
 * tenant isolation. Queries that omit a {@code tenantId} filter are a security defect.
 */
public interface TenantAwareRepository {}
