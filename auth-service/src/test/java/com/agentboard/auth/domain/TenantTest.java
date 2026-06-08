package com.agentboard.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TenantTest {

  @Test
  void shouldReturnName_whenGetNameCalled() {
    Tenant tenant = new Tenant("My Workspace");

    assertThat(tenant.getName()).isEqualTo("My Workspace");
  }

  @Test
  void shouldReturnNullId_whenNewlyCreated() {
    Tenant tenant = new Tenant("New Workspace");

    assertThat(tenant.getId()).isNull();
  }
}
