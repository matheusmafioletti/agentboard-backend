package com.agentboard.auth.service;

import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantMembership;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.CreateTenantResponse;
import com.agentboard.auth.dto.SessionResponse;
import com.agentboard.auth.exception.DuplicateTenantNameException;
import com.agentboard.auth.repository.TenantApiKeyRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private TenantApiKeyRepository tenantApiKeyRepository;
    @Mock
    private MembershipService membershipService;
    @Mock
    private SessionFactory sessionFactory;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(
                tenantRepository,
                userAccountRepository,
                tenantApiKeyRepository,
                membershipService,
                sessionFactory);
    }

    @Test
    void shouldReturnCreateTenantResponse_whenNameIsAvailable() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String tenantName = "New Workspace";
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        Tenant tenant = new Tenant(tenantName);
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.ADMIN);
        SessionResponse session = new SessionResponse(
                "jwt-token", userId, tenantId, tenantName, "user@example.com", "Test User",
                MembershipRole.ADMIN);

        when(tenantRepository.existsByName(tenantName)).thenReturn(false);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
        when(membershipService.createAdminMembership(userId, tenantId)).thenReturn(membership);
        when(sessionFactory.buildSession(user, tenant, membership)).thenReturn(session);

        CreateTenantResponse response = tenantService.createTenantForUser(userId, tenantName);

        assertThat(response.session())
                .returns("jwt-token", SessionResponse::token)
                .returns(userId, SessionResponse::userId)
                .returns(tenantId, SessionResponse::tenantId)
                .returns(MembershipRole.ADMIN, SessionResponse::role);
        assertThat(response.apiKey()).isNotBlank();

        verify(tenantRepository).save(any(Tenant.class));
        verify(tenantApiKeyRepository).save(any());
        verify(membershipService).createAdminMembership(userId, tenantId);
        verify(sessionFactory).buildSession(user, tenant, membership);
    }

    @Test
    void shouldThrowDuplicateTenantNameException_whenNameIsTaken() {
        UUID userId = UUID.randomUUID();
        String tenantName = "Existing Workspace";

        when(tenantRepository.existsByName(tenantName)).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenantForUser(userId, tenantName))
                .isInstanceOf(DuplicateTenantNameException.class)
                .hasMessage("Tenant name already taken: " + tenantName);

        verify(tenantRepository, never()).save(any());
        verify(userAccountRepository, never()).findById(any());
    }
}
