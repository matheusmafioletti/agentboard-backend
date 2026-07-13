package com.agentboard.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.agentboard.auth.domain.InviteStatus;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantInvite;
import com.agentboard.auth.dto.InviteResponse;
import com.agentboard.auth.repository.TenantInviteRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import com.agentboard.commons.policy.DataSourcePolicy;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InviteServiceTrailingSlashTest {

  @Mock
  private TenantInviteRepository inviteRepository;
  @Mock
  private TenantRepository tenantRepository;
  @Mock
  private UserAccountRepository userAccountRepository;
  @Mock
  private MembershipService membershipService;
  @Mock
  private SessionFactory sessionFactory;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private DataSourcePolicy dataSourcePolicy;

  @Test
  void shouldStripTrailingSlash_whenBaseUrlEndsWithSlash() {
    InviteService service = new InviteService(
        inviteRepository, tenantRepository, userAccountRepository,
        membershipService, sessionFactory, passwordEncoder, dataSourcePolicy,
        "http://localhost:5173/");

    UUID tenantId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();
    UUID inviteId = UUID.randomUUID();
    TenantInvite savedInvite = new TenantInvite(
        tenantId, "user@example.com", "hash", invitedBy,
        OffsetDateTime.now().plusDays(7));
    ReflectionTestUtils.setField(savedInvite, "id", inviteId);

    Tenant tenant = new Tenant("Tenant");
    ReflectionTestUtils.setField(tenant, "id", tenantId);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(userAccountRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
    when(inviteRepository.findByTenantIdAndEmailAndStatus(
        tenantId, "user@example.com", InviteStatus.PENDING)).thenReturn(Optional.empty());
    when(inviteRepository.save(any(TenantInvite.class))).thenReturn(savedInvite);

    InviteResponse response = service.createInvite(tenantId, invitedBy, "user@example.com");

    assertThat(response.inviteUrl()).startsWith("http://localhost:5173/invite/");
    assertThat(response.inviteUrl()).doesNotContain("5173//invite");
  }
}
