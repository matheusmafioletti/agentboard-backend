package com.agentboard.auth.integration;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Marks tenants as test workspaces for data-source integration scenarios. */
@Component
public class TestTenantSupport {

  private final JdbcTemplate jdbcTemplate;

  /** Creates the helper with JDBC access to the shared test database. */
  public TestTenantSupport(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Sets {@code tenant.test_tenant = true} for the given workspace id.
   */
  public void markTestTenant(UUID tenantId) {
    jdbcTemplate.update("UPDATE tenant SET test_tenant = true WHERE id = ?", tenantId);
  }

  /**
   * Returns the {@code data_source} column for a user account row.
   */
  public String userDataSource(UUID userId) {
    return jdbcTemplate.queryForObject(
        "SELECT data_source FROM user_account WHERE id = ?", String.class, userId);
  }

  /**
   * Returns the {@code data_source} column for a membership row.
   */
  public String membershipDataSource(UUID userId, UUID tenantId) {
    return jdbcTemplate.queryForObject(
        "SELECT data_source FROM tenant_membership WHERE user_id = ? AND tenant_id = ?",
        String.class, userId, tenantId);
  }

  /**
   * Returns the {@code data_source} column for an invite row.
   */
  public String inviteDataSource(UUID inviteId) {
    return jdbcTemplate.queryForObject(
        "SELECT data_source FROM tenant_invite WHERE id = ?", String.class, inviteId);
  }
}
