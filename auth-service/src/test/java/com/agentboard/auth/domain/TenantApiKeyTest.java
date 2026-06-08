package com.agentboard.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantApiKeyTest {

  @Test
  void shouldReturnStoredFields_whenGettersInvoked() {
    UUID tenantId = UUID.randomUUID();
    TenantApiKey apiKey = new TenantApiKey(tenantId, "sha256hash");

    assertThat(apiKey.getTenantId()).isEqualTo(tenantId);
    assertThat(apiKey.getKeyHash()).isEqualTo("sha256hash");
    assertThat(apiKey.getCreatedAt()).isNotNull();
    assertThat(apiKey.getRevokedAt()).isNull();
  }
}
