package com.agentboard.commons.policy;

import com.agentboard.commons.domain.DataSource;
import com.agentboard.commons.exceptions.DataSourceNotAllowedException;
import java.util.UUID;

/**
 * Central rules for {@code X-Data-Source} usage across auth and board services.
 */
public class DataSourcePolicy {

  /**
   * Rejects automation/seed when creating a new tenant (register or POST /auth/tenants).
   */
  public void requireManualForNewTenant(DataSource source) {
    if (source != DataSource.MANUAL) {
      throw new DataSourceNotAllowedException(
          "automation and seed data sources are not allowed when creating a new tenant");
    }
  }

  /**
   * Allows {@link DataSource#MANUAL} always; automation/seed require {@code testTenant = true}.
   */
  public void requireTestTenantForSynthetic(UUID tenantId, DataSource source, boolean testTenant) {
    if (source == DataSource.MANUAL) {
      return;
    }
    if (!testTenant) {
      throw new DataSourceNotAllowedException(
          "data source '" + source.dbValue() + "' is not allowed for tenant " + tenantId);
    }
  }
}
