package com.agentboard.board.service;

import com.agentboard.commons.tenant.TenantTestFlagReader;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads {@code tenant.test_tenant} via JDBC against the shared database.
 *
 * <p>Returns {@code false} when the tenant row or table is absent (board-only test databases).
 */
@Component
public class TenantTestFlagReaderImpl implements TenantTestFlagReader {

  private final JdbcTemplate jdbcTemplate;

  /** Creates the reader backed by JDBC. */
  public TenantTestFlagReaderImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public boolean isTestTenant(UUID tenantId) {
    try {
      Boolean flag = jdbcTemplate.queryForObject(
          "SELECT test_tenant FROM tenant WHERE id = ?", Boolean.class, tenantId);
      return Boolean.TRUE.equals(flag);
    } catch (EmptyResultDataAccessException ex) {
      return false;
    } catch (DataAccessException ex) {
      return false;
    }
  }
}
