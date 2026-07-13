package com.agentboard.auth.tenant;

import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.commons.tenant.TenantTestFlagReader;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Loads {@code tenant.test_tenant} via {@link com.agentboard.auth.repository.TenantRepository}. */
@Component
public class TenantTestFlagReaderImpl implements TenantTestFlagReader {

  private final TenantRepository tenantRepository;

  /** Creates the reader backed by the tenant repository. */
  public TenantTestFlagReaderImpl(TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  @Override
  public boolean isTestTenant(UUID tenantId) {
    return tenantRepository.findById(tenantId)
        .map(tenant -> tenant.isTestTenant())
        .orElse(false);
  }
}
