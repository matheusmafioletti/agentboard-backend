package com.agentboard.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantInviteTest {

  @Test
  void shouldNormalizeEmail_whenConstructed() {
    UUID tenantId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();
    TenantInvite invite = new TenantInvite(
        tenantId, "Guest@EXAMPLE.COM", "hash", invitedBy, OffsetDateTime.now().plusDays(7));

    assertThat(invite.getEmail()).isEqualTo("guest@example.com");
  }

  @Test
  void shouldBePending_whenNewlyCreated() {
    UUID tenantId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();
    TenantInvite invite = new TenantInvite(
        tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().plusDays(7));

    assertThat(invite.getStatus()).isEqualTo(InviteStatus.PENDING);
  }

  @Test
  void shouldMarkAccepted_whenMarkAcceptedCalled() {
    UUID tenantId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();
    TenantInvite invite = new TenantInvite(
        tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().plusDays(7));

    invite.markAccepted();

    assertThat(invite.getStatus()).isEqualTo(InviteStatus.ACCEPTED);
    assertThat(invite.getAcceptedAt()).isNotNull();
  }

  @Test
  void shouldMarkCancelled_whenMarkCancelledCalled() {
    UUID tenantId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();
    TenantInvite invite = new TenantInvite(
        tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().plusDays(7));

    invite.markCancelled();

    assertThat(invite.getStatus()).isEqualTo(InviteStatus.CANCELLED);
  }

  @Test
  void shouldMarkExpired_whenMarkExpiredCalled() {
    UUID tenantId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();
    TenantInvite invite = new TenantInvite(
        tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().plusDays(7));

    invite.markExpired();

    assertThat(invite.getStatus()).isEqualTo(InviteStatus.EXPIRED);
  }

  @Test
  void shouldReturnTrue_whenInviteIsExpiredPending() {
    UUID tenantId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();
    TenantInvite invite = new TenantInvite(
        tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().minusDays(1));

    assertThat(invite.isExpiredPending()).isTrue();
  }

  @Test
  void shouldReturnFalse_whenInviteIsNotExpired() {
    UUID tenantId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();
    TenantInvite invite = new TenantInvite(
        tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().plusDays(7));

    assertThat(invite.isExpiredPending()).isFalse();
  }

  @Test
  void shouldReturnFalse_whenAlreadyAcceptedAndPastExpiry() {
    UUID tenantId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();
    TenantInvite invite = new TenantInvite(
        tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().minusDays(1));
    invite.markAccepted();

    assertThat(invite.isExpiredPending()).isFalse();
  }

  @Test
  void shouldReturnStoredFields_whenGettersInvoked() {
    UUID tenantId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();
    OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);
    TenantInvite invite = new TenantInvite(
        tenantId, "guest@example.com", "token-hash", invitedBy, expiresAt);

    assertThat(invite.getTenantId()).isEqualTo(tenantId);
    assertThat(invite.getTokenHash()).isEqualTo("token-hash");
    assertThat(invite.getInvitedBy()).isEqualTo(invitedBy);
    assertThat(invite.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(invite.getCreatedAt()).isNotNull();
  }
}
