package com.agentboard.board.integration;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Marks tenants as test workspaces for board data-source integration scenarios. */
@Component
public class TestTenantSupport {

  private final JdbcTemplate jdbcTemplate;

  /** Creates the helper with JDBC access to the shared test database. */
  public TestTenantSupport(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Ensures a minimal {@code tenant} row exists and sets {@code test_tenant = true}.
   *
   * <p>NOTE: board-service integration tests run board Flyway only; the tenant table may be absent
   * until this helper creates it for synthetic data-source scenarios.
   */
  public void markTestTenant(UUID tenantId) {
    jdbcTemplate.execute(
        "CREATE TABLE IF NOT EXISTS tenant ("
            + "id UUID PRIMARY KEY, "
            + "name VARCHAR(100) NOT NULL, "
            + "created_at TIMESTAMPTZ NOT NULL DEFAULT now(), "
            + "test_tenant BOOLEAN NOT NULL DEFAULT false)");
    jdbcTemplate.update(
        "INSERT INTO tenant (id, name, test_tenant) VALUES (?, ?, true) "
            + "ON CONFLICT (id) DO UPDATE SET test_tenant = true",
        tenantId, "IT Tenant " + tenantId);
  }

  /**
   * Returns the {@code data_source} column for a project row.
   */
  public String projectDataSource(UUID projectId) {
    return jdbcTemplate.queryForObject(
        "SELECT data_source FROM project WHERE id = ?", String.class, projectId);
  }

  /**
   * Returns the {@code data_source} column for a work item row.
   */
  public String workItemDataSource(UUID workItemId) {
    return jdbcTemplate.queryForObject(
        "SELECT data_source FROM work_item WHERE id = ?", String.class, workItemId);
  }
}
