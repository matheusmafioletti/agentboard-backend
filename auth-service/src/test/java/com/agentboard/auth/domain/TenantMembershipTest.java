package com.agentboard.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantMembershipTest {

  @Test
  void shouldReturnStoredFields_whenGettersInvoked() {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.ADMIN);

    assertThat(membership.getUserId()).isEqualTo(userId);
    assertThat(membership.getTenantId()).isEqualTo(tenantId);
    assertThat(membership.getRole()).isEqualTo(MembershipRole.ADMIN);
    assertThat(membership.getJoinedAt()).isNotNull();
    assertThat(membership.getId()).isNull();
  }
}
