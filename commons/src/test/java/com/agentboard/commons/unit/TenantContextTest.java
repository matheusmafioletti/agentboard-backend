package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.commons.security.TenantContext;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

  @AfterEach
  void cleanup() {
    TenantContext.clear();
  }

  @Test
  void shouldReturnSameTenantId_whenSetAndGet() {
    UUID tenantId = UUID.randomUUID();
    TenantContext.set(tenantId);
    assertThat(TenantContext.get()).isEqualTo(tenantId);
  }

  @Test
  void shouldReturnNull_whenClearCalledAfterSet() {
    TenantContext.set(UUID.randomUUID());
    TenantContext.clear();
    assertThat(TenantContext.get()).isNull();
  }

  @Test
  void shouldReturnNull_whenNeverSet() {
    assertThat(TenantContext.get()).isNull();
  }

  @Test
  void shouldIsolateTenantIdAcrossThreads_whenRunConcurrently() throws InterruptedException {
    UUID mainTenantId = UUID.randomUUID();
    UUID otherTenantId = UUID.randomUUID();
    TenantContext.set(mainTenantId);
    AtomicReference<UUID> threadResult = new AtomicReference<>();
    Thread other = new Thread(() -> {
      TenantContext.set(otherTenantId);
      threadResult.set(TenantContext.get());
      TenantContext.clear();
    });
    other.start();
    other.join();
    assertThat(TenantContext.get()).isEqualTo(mainTenantId);
    assertThat(threadResult.get()).isEqualTo(otherTenantId);
  }
}
