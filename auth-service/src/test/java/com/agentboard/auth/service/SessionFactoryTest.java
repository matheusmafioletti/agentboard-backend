package com.agentboard.auth.service;

import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantMembership;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.SessionResponse;
import com.agentboard.auth.security.JwtTokenService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionFactoryTest {

    @Mock
    private JwtTokenService jwtTokenService;

    private SessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        sessionFactory = new SessionFactory(jwtTokenService);
    }

    @Test
    void shouldReturnSessionResponse_whenBuildSessionWithMembership() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.USER);

        when(jwtTokenService.generate(userId, tenantId, MembershipRole.USER)).thenReturn("jwt-token");

        SessionResponse response = sessionFactory.buildSession(user, tenant, membership);

        assertThat(response)
                .returns("jwt-token", SessionResponse::token)
                .returns(userId, SessionResponse::userId)
                .returns(tenantId, SessionResponse::tenantId)
                .returns("Workspace", SessionResponse::tenantName)
                .returns("user@example.com", SessionResponse::email)
                .returns("Test User", SessionResponse::name)
                .returns(MembershipRole.USER, SessionResponse::role);
        verify(jwtTokenService).generate(userId, tenantId, MembershipRole.USER);
    }

    @Test
    void shouldReturnSessionResponse_whenBuildSessionWithRole() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);

        when(jwtTokenService.generate(userId, tenantId, MembershipRole.ADMIN)).thenReturn("jwt-token");

        SessionResponse response = sessionFactory.buildSession(user, tenant, MembershipRole.ADMIN);

        assertThat(response)
                .returns("jwt-token", SessionResponse::token)
                .returns(userId, SessionResponse::userId)
                .returns(tenantId, SessionResponse::tenantId)
                .returns(MembershipRole.ADMIN, SessionResponse::role);
        verify(jwtTokenService).generate(userId, tenantId, MembershipRole.ADMIN);
    }
}
