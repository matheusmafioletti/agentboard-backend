package com.agentboard.commons.tenant;

import java.util.UUID;

/**
 * Reads whether a tenant is marked as a test workspace ({@code tenant.test_tenant}).
 */
public interface TenantTestFlagReader {

  /**
   * Returns {@code true} when the tenant exists and {@code test_tenant} is set.
   */
  boolean isTestTenant(UUID tenantId);
}
