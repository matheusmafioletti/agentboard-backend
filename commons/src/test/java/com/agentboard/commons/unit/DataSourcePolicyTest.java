package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentboard.commons.domain.DataSource;
import com.agentboard.commons.exceptions.DataSourceNotAllowedException;
import com.agentboard.commons.policy.DataSourcePolicy;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataSourcePolicyTest {

  private final DataSourcePolicy policy = new DataSourcePolicy();
  private final UUID tenantId = UUID.randomUUID();

  @Test
  void requireManualForNewTenant_manualAllowed() {
    assertThatCode(() -> policy.requireManualForNewTenant(DataSource.MANUAL))
        .doesNotThrowAnyException();
  }

  @Test
  void requireManualForNewTenant_automationOrSeedRejected() {
    assertThatThrownBy(() -> policy.requireManualForNewTenant(DataSource.AUTOMATION))
        .isInstanceOf(DataSourceNotAllowedException.class);
    assertThatThrownBy(() -> policy.requireManualForNewTenant(DataSource.SEED))
        .isInstanceOf(DataSourceNotAllowedException.class);
  }

  @Test
  void requireTestTenantForSynthetic_manualAlwaysAllowed() {
    assertThatCode(() -> policy.requireTestTenantForSynthetic(tenantId, DataSource.MANUAL, false))
        .doesNotThrowAnyException();
    assertThatCode(() -> policy.requireTestTenantForSynthetic(tenantId, DataSource.MANUAL, true))
        .doesNotThrowAnyException();
  }

  @Test
  void requireTestTenantForSynthetic_syntheticRequiresTestTenantFlag() {
    assertThatThrownBy(() ->
        policy.requireTestTenantForSynthetic(tenantId, DataSource.AUTOMATION, false))
        .isInstanceOf(DataSourceNotAllowedException.class);
    assertThatThrownBy(() ->
        policy.requireTestTenantForSynthetic(tenantId, DataSource.SEED, false))
        .isInstanceOf(DataSourceNotAllowedException.class);

    assertThatCode(() ->
        policy.requireTestTenantForSynthetic(tenantId, DataSource.AUTOMATION, true))
        .doesNotThrowAnyException();
    assertThatCode(() ->
        policy.requireTestTenantForSynthetic(tenantId, DataSource.SEED, true))
        .doesNotThrowAnyException();
  }
}
