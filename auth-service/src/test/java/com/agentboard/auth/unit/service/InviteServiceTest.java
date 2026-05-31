package com.agentboard.auth.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentboard.auth.domain.InviteStatus;
import com.agentboard.auth.domain.TenantInvite;
import com.agentboard.auth.exception.DuplicatePendingInviteException;
import com.agentboard.auth.repository.TenantInviteRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import com.agentboard.auth.service.InviteService;
import com.agentboard.auth.service.MembershipService;
import com.agentboard.auth.service.SessionFactory;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for {@link InviteService}. */
@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

  @Mock private TenantInviteRepository inviteRepository;
  @Mock private TenantRepository tenantRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private MembershipService membershipService;
  @Mock private SessionFactory sessionFactory;
  @Mock private PasswordEncoder passwordEncoder;

  private InviteService inviteService;

  @BeforeEach
  void setUp() {
    inviteService = new InviteService(
        inviteRepository,
        tenantRepository,
        userAccountRepository,
        membershipService,
        sessionFactory,
        passwordEncoder,
        "http://localhost:5173");
  }

  @Test
  void createInvite_rejectsDuplicatePending() {
    UUID tenantId = UUID.randomUUID();
    when(inviteRepository.findByTenantIdAndEmailAndStatus(
        tenantId, "invitee@example.com", InviteStatus.PENDING))
        .thenReturn(Optional.of(new TenantInvite(
            tenantId, "invitee@example.com", "hash", UUID.randomUUID(),
            OffsetDateTime.now().plusDays(7))));

    assertThatThrownBy(() -> inviteService.createInvite(
        tenantId, UUID.randomUUID(), "invitee@example.com"))
        .isInstanceOf(DuplicatePendingInviteException.class);
  }

  @Test
  void createInvite_returnsInviteUrlWithRawToken() {
    UUID tenantId = UUID.randomUUID();
    when(inviteRepository.findByTenantIdAndEmailAndStatus(
        any(), any(), any())).thenReturn(Optional.empty());
    when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(inviteRepository.save(any(TenantInvite.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var response = inviteService.createInvite(tenantId, UUID.randomUUID(), "New@Example.com");

    assertThat(response.inviteUrl()).startsWith("http://localhost:5173/invite/");
    assertThat(response.email()).isEqualTo("new@example.com");
    verify(inviteRepository).save(any(TenantInvite.class));
  }
}
