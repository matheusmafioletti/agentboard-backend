package com.agentboard.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.auth.domain.InviteStatus;
import com.agentboard.commons.domain.DataSource;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantInvite;
import com.agentboard.auth.domain.UserAccount;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Repository slice tests for {@link TenantInviteRepository} against PostgreSQL 16.
 */
class TenantInviteRepositoryTest extends AbstractRepositoryTest {

  @Autowired
  TenantInviteRepository inviteRepository;

  @Test
  void save_persistsDataSourceColumn() {
    Tenant tenant = persistTenant();
    UserAccount inviter = persistUser();
    TenantInvite saved = inviteRepository.save(new TenantInvite(
        tenant.getId(), "ds@example.com", "hash-ds-" + UUID.randomUUID(), inviter.getId(),
        OffsetDateTime.now().plusDays(7), DataSource.SEED));

    assertThat(inviteRepository.findById(saved.getId()).orElseThrow().getDataSource())
        .isEqualTo(DataSource.SEED);
  }

  @Test
  void findByTokenHash_returnsMatchingInvite() {
    Tenant tenant = persistTenant();
    UserAccount inviter = persistUser();
    String tokenHash = "hash-" + UUID.randomUUID();
    TenantInvite saved = inviteRepository.save(new TenantInvite(
        tenant.getId(), "invitee@example.com", tokenHash, inviter.getId(),
        OffsetDateTime.now().plusDays(7)));

    assertThat(inviteRepository.findByTokenHash(tokenHash))
        .isPresent()
        .get()
        .extracting(TenantInvite::getId)
        .isEqualTo(saved.getId());
    assertThat(inviteRepository.findByTokenHash("missing-hash")).isEmpty();
  }

  @Test
  void findByTenantIdAndEmailAndStatus_doesNotCollideAcrossTenants() {
    Tenant tenantA = persistTenant();
    Tenant tenantB = persistTenant();
    UserAccount inviter = persistUser();
    String sharedEmail = "shared@example.com";
    OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

    TenantInvite inviteA = inviteRepository.save(new TenantInvite(
        tenantA.getId(), sharedEmail, "hash-a-" + UUID.randomUUID(), inviter.getId(), expiresAt));
    inviteRepository.save(new TenantInvite(
        tenantB.getId(), sharedEmail, "hash-b-" + UUID.randomUUID(), inviter.getId(), expiresAt));

    assertThat(inviteRepository.findByTenantIdAndEmailAndStatus(
        tenantA.getId(), sharedEmail, InviteStatus.PENDING))
        .isPresent()
        .get()
        .extracting(TenantInvite::getId)
        .isEqualTo(inviteA.getId());
  }

  @Test
  void findByTenantIdAndEmailAndStatus_excludesNonPendingInvites() {
    Tenant tenant = persistTenant();
    UserAccount inviter = persistUser();
    TenantInvite cancelled = new TenantInvite(
        tenant.getId(), "cancelled@example.com", "hash-" + UUID.randomUUID(), inviter.getId(),
        OffsetDateTime.now().plusDays(7));
    cancelled.markCancelled();
    inviteRepository.save(cancelled);

    assertThat(inviteRepository.findByTenantIdAndEmailAndStatus(
        tenant.getId(), "cancelled@example.com", InviteStatus.PENDING)).isEmpty();
    assertThat(inviteRepository.findByTenantIdAndEmailAndStatus(
        tenant.getId(), "cancelled@example.com", InviteStatus.CANCELLED)).isPresent();
  }

  @Test
  void findByTenantId_returnsOnlyInvitesOfThatTenant() {
    Tenant tenantA = persistTenant();
    Tenant tenantB = persistTenant();
    UserAccount inviter = persistUser();
    OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

    inviteRepository.save(new TenantInvite(
        tenantA.getId(), "a1@example.com", "hash-1-" + UUID.randomUUID(),
        inviter.getId(), expiresAt));
    inviteRepository.save(new TenantInvite(
        tenantA.getId(), "a2@example.com", "hash-2-" + UUID.randomUUID(),
        inviter.getId(), expiresAt));
    inviteRepository.save(new TenantInvite(
        tenantB.getId(), "b1@example.com", "hash-3-" + UUID.randomUUID(),
        inviter.getId(), expiresAt));

    assertThat(inviteRepository.findByTenantId(tenantA.getId()))
        .hasSize(2)
        .allSatisfy(invite -> assertThat(invite.getTenantId()).isEqualTo(tenantA.getId()));
    assertThat(inviteRepository.findByTenantId(tenantB.getId())).hasSize(1);
  }

  @Test
  void expiredPendingInvite_isDetectedByDomainGuard() {
    Tenant tenant = persistTenant();
    UserAccount inviter = persistUser();
    TenantInvite expired = inviteRepository.save(new TenantInvite(
        tenant.getId(), "expired@example.com", "hash-" + UUID.randomUUID(), inviter.getId(),
        OffsetDateTime.now().minusDays(1)));

    assertThat(expired.isExpiredPending()).isTrue();
    expired.markExpired();
    inviteRepository.save(expired);

    assertThat(inviteRepository.findByTenantIdAndEmailAndStatus(
        tenant.getId(), "expired@example.com", InviteStatus.EXPIRED)).isPresent();
  }
}
