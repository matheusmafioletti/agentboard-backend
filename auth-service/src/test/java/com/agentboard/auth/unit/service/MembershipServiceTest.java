package com.agentboard.auth.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantMembership;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.exception.LastAdminException;
import com.agentboard.auth.exception.NotMemberException;
import com.agentboard.auth.repository.TenantMembershipRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import com.agentboard.auth.service.MembershipService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link MembershipService}. */
@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

  @Mock
  private TenantMembershipRepository membershipRepository;
  @Mock
  private TenantRepository tenantRepository;
  @Mock
  private UserAccountRepository userAccountRepository;

  private MembershipService membershipService;

  @BeforeEach
  void setUp() {
    membershipService = new MembershipService(
        membershipRepository, tenantRepository, userAccountRepository);
  }

  @Test
  void listByUserId_returnsSummaries() {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.ADMIN);
    when(membershipRepository.findByUserId(userId)).thenReturn(List.of(membership));
    Tenant tenant = org.mockito.Mockito.mock(Tenant.class);
    when(tenant.getName()).thenReturn("Acme");
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

    var summaries = membershipService.listByUserId(userId);

    assertThat(summaries).hasSize(1);
    assertThat(summaries.getFirst().tenantName()).isEqualTo("Acme");
    assertThat(summaries.getFirst().role()).isEqualTo(MembershipRole.ADMIN);
  }

  @Test
  void assertIsMember_throwsWhenMissing() {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    when(membershipRepository.findByUserIdAndTenantId(userId, tenantId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> membershipService.assertIsMember(userId, tenantId))
        .isInstanceOf(NotMemberException.class);
  }

  @Test
  void createAdminMembership_persistsAdminRole() {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    when(membershipRepository.save(any(TenantMembership.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    TenantMembership result = membershipService.createAdminMembership(userId, tenantId);

    assertThat(result.getRole()).isEqualTo(MembershipRole.ADMIN);
    verify(membershipRepository).save(any(TenantMembership.class));
  }

  @Test
  void revokeMembership_blocksLastAdmin() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    TenantMembership admin = new TenantMembership(userId, tenantId, MembershipRole.ADMIN);
    when(membershipRepository.findByUserIdAndTenantId(userId, tenantId))
        .thenReturn(Optional.of(admin));
    when(membershipRepository.countByTenantIdAndRole(tenantId, MembershipRole.ADMIN))
        .thenReturn(1L);

    assertThatThrownBy(() -> membershipService.revokeMembership(tenantId, userId))
        .isInstanceOf(LastAdminException.class);
  }
}
