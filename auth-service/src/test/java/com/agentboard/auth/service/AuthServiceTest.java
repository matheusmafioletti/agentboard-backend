package com.agentboard.auth.service;

import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantMembership;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.ChangePasswordRequest;
import com.agentboard.auth.dto.LoginRequest;
import com.agentboard.auth.dto.RegisterRequest;
import com.agentboard.auth.dto.RegisterResponse;
import com.agentboard.auth.dto.SelectTenantRequest;
import com.agentboard.auth.dto.SessionResponse;
import com.agentboard.auth.dto.TenantMembershipSummary;
import com.agentboard.auth.dto.TenantSelectionResponse;
import com.agentboard.auth.exception.DuplicateEmailException;
import com.agentboard.auth.exception.DuplicateTenantNameException;
import com.agentboard.auth.exception.InvalidCredentialsException;
import com.agentboard.auth.exception.NoMembershipException;
import com.agentboard.auth.exception.NotMemberException;
import com.agentboard.auth.repository.TenantApiKeyRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private TenantApiKeyRepository tenantApiKeyRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MembershipService membershipService;
    @Mock
    private SessionFactory sessionFactory;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                tenantRepository,
                userAccountRepository,
                tenantApiKeyRepository,
                passwordEncoder,
                membershipService,
                sessionFactory);
    }

    @Test
    void shouldReturnRegisterResponse_whenRequestIsValid() {
        var request = new RegisterRequest("Name", "email@email.com", "12345678", "Tenant");
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Tenant tenant = new Tenant("Tenant");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        UserAccount user = new UserAccount("Name", "email@email.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        TenantMembership membership =
                new TenantMembership(userId, tenantId, MembershipRole.ADMIN);
        SessionResponse session = new SessionResponse(
                "jwt-token", userId, tenantId, "Tenant", "email@email.com", "Name", MembershipRole.ADMIN);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
        when(passwordEncoder.encode("12345678")).thenReturn("hashed-password");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(user);
        when(membershipService.createAdminMembership(userId, tenantId)).thenReturn(membership);
        when(sessionFactory.buildSession(user, tenant, membership)).thenReturn(session);

        RegisterResponse response = authService.register(request);

        assertThat(response)
                .returns(userId, RegisterResponse::userId)
                .returns(tenantId, RegisterResponse::tenantId)
                .returns("Tenant", RegisterResponse::tenantName)
                .returns("jwt-token", RegisterResponse::token)
                .returns(MembershipRole.ADMIN, RegisterResponse::role);
        assertThat(response.apiKey()).isNotBlank();

        verify(tenantRepository).save(any());
        verify(userAccountRepository).save(any());
        verify(tenantApiKeyRepository).save(any());
        verify(tenantRepository).existsByName(any());
        verify(userAccountRepository).existsByEmail(any());
    }

    @Test
    void shouldThrowDuplicateEmailException_whenEmailAlreadyExists(){
        var request = new RegisterRequest("Name", "email@email.com", "12345678", "Tenant");

        when(userAccountRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("Email already registered: " + request.email());

        verify(tenantRepository, never()).save(any());
        verify(userAccountRepository, never()).save(any());
        verify(tenantRepository, never()).existsByName(any());
    }

    @Test
    void shouldThrowDuplicateTenantNameException_whenTenantNameAlreadyExists() {
        var request = new RegisterRequest("Name", "email@email.com", "12345678", "Tenant");

        when(userAccountRepository.existsByEmail(request.email())).thenReturn(false);
        when(tenantRepository.existsByName(request.tenantName())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateTenantNameException.class)
                .hasMessage("Tenant name already taken: " + request.tenantName());

        verify(tenantRepository, never()).save(any());
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void shouldReturnSessionResponse_whenUserHasSingleMembership() {
        var request = new LoginRequest("user@example.com", "password123");
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.USER);
        TenantMembershipSummary summary = new TenantMembershipSummary(
                tenantId, "Workspace", MembershipRole.USER, OffsetDateTime.now());
        SessionResponse session = new SessionResponse(
                "jwt-token", userId, tenantId, "Workspace", "user@example.com", "Test User",
                MembershipRole.USER);

        when(userAccountRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), "hashed-password")).thenReturn(true);
        when(membershipService.listByUserId(userId)).thenReturn(List.of(summary));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(membershipService.getMembership(userId, tenantId)).thenReturn(membership);
        when(sessionFactory.buildSession(user, tenant, membership)).thenReturn(session);

        Object result = authService.login(request);

        assertThat(result).isInstanceOf(SessionResponse.class);
        assertThat((SessionResponse) result)
                .returns("jwt-token", SessionResponse::token)
                .returns(userId, SessionResponse::userId)
                .returns(tenantId, SessionResponse::tenantId);
        verify(membershipService).listByUserId(userId);
        verify(sessionFactory).buildSession(user, tenant, membership);
    }

    @Test
    void shouldReturnTenantSelectionResponse_whenUserHasMultipleMemberships() {
        var request = new LoginRequest("user@example.com", "password123");
        UUID userId = UUID.randomUUID();
        UUID tenantId1 = UUID.randomUUID();
        UUID tenantId2 = UUID.randomUUID();
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        TenantMembershipSummary summary1 = new TenantMembershipSummary(
                tenantId1, "Workspace A", MembershipRole.ADMIN, OffsetDateTime.now());
        TenantMembershipSummary summary2 = new TenantMembershipSummary(
                tenantId2, "Workspace B", MembershipRole.USER, OffsetDateTime.now());

        when(userAccountRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), "hashed-password")).thenReturn(true);
        when(membershipService.listByUserId(userId)).thenReturn(List.of(summary1, summary2));

        Object result = authService.login(request);

        assertThat(result).isInstanceOf(TenantSelectionResponse.class);
        assertThat((TenantSelectionResponse) result)
                .returns(true, TenantSelectionResponse::requiresTenantSelection)
                .returns(userId, TenantSelectionResponse::userId)
                .returns("user@example.com", TenantSelectionResponse::email)
                .returns("Test User", TenantSelectionResponse::name);
        assertThat(((TenantSelectionResponse) result).memberships()).hasSize(2);
        verify(sessionFactory, never()).buildSession(
                any(UserAccount.class), any(Tenant.class), any(TenantMembership.class));
    }

    @Test
    void shouldThrowNoMembershipException_whenUserHasNoMemberships() {
        var request = new LoginRequest("user@example.com", "password123");
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);

        when(userAccountRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), "hashed-password")).thenReturn(true);
        when(membershipService.listByUserId(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(NoMembershipException.class)
                .hasMessage("User has no workspace memberships");

        verify(sessionFactory, never()).buildSession(
                any(UserAccount.class), any(Tenant.class), any(TenantMembership.class));
    }

    @Test
    void shouldThrowInvalidCredentialsException_whenEmailIsUnknown() {
        var request = new LoginRequest("unknown@example.com", "password123");

        when(userAccountRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(membershipService, never()).listByUserId(any());
    }

    @Test
    void shouldThrowInvalidCredentialsException_whenPasswordIsWrong() {
        var request = new LoginRequest("user@example.com", "wrong-password");
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");

        when(userAccountRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(membershipService, never()).listByUserId(any());
    }

    @Test
    void shouldReturnSessionResponse_whenSelectTenantWithValidCredentials() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var request = new SelectTenantRequest("user@example.com", "password123", tenantId);
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.USER);
        SessionResponse session = new SessionResponse(
                "jwt-token", userId, tenantId, "Workspace", "user@example.com", "Test User",
                MembershipRole.USER);

        when(userAccountRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), "hashed-password")).thenReturn(true);
        when(membershipService.getMembership(userId, tenantId)).thenReturn(membership);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(sessionFactory.buildSession(user, tenant, membership)).thenReturn(session);

        SessionResponse response = authService.selectTenant(request);

        assertThat(response)
                .returns("jwt-token", SessionResponse::token)
                .returns(userId, SessionResponse::userId)
                .returns(tenantId, SessionResponse::tenantId);
        verify(membershipService).getMembership(userId, tenantId);
        verify(sessionFactory).buildSession(user, tenant, membership);
    }

    @Test
    void shouldThrowInvalidCredentialsException_whenSelectTenantWithWrongPassword() {
        UUID tenantId = UUID.randomUUID();
        var request = new SelectTenantRequest("user@example.com", "wrong-password", tenantId);
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");

        when(userAccountRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.selectTenant(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(membershipService, never()).getMembership(any(), any());
    }

    @Test
    void shouldThrowNotMemberException_whenSelectTenantForUnaffiliatedWorkspace() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var request = new SelectTenantRequest("user@example.com", "password123", tenantId);
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);

        when(userAccountRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), "hashed-password")).thenReturn(true);
        when(membershipService.getMembership(userId, tenantId)).thenThrow(new NotMemberException());

        assertThatThrownBy(() -> authService.selectTenant(request))
                .isInstanceOf(NotMemberException.class)
                .hasMessage("User is not a member of this workspace");

        verify(sessionFactory, never()).buildSession(
                any(UserAccount.class), any(Tenant.class), any(TenantMembership.class));
    }

    @Test
    void shouldReturnSessionResponse_whenSwitchTenantForMember() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.ADMIN);
        SessionResponse session = new SessionResponse(
                "jwt-token", userId, tenantId, "Workspace", "user@example.com", "Test User",
                MembershipRole.ADMIN);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipService.getMembership(userId, tenantId)).thenReturn(membership);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(sessionFactory.buildSession(user, tenant, membership)).thenReturn(session);

        SessionResponse response = authService.switchTenant(userId, tenantId);

        assertThat(response)
                .returns("jwt-token", SessionResponse::token)
                .returns(userId, SessionResponse::userId)
                .returns(tenantId, SessionResponse::tenantId)
                .returns(MembershipRole.ADMIN, SessionResponse::role);
        verify(membershipService).getMembership(userId, tenantId);
        verify(sessionFactory).buildSession(user, tenant, membership);
    }

    @Test
    void shouldThrowInvalidCredentialsException_whenSwitchTenantForUnknownUser() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.switchTenant(userId, tenantId))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(membershipService, never()).getMembership(any(), any());
    }

    @Test
    void shouldThrowNotMemberException_whenSwitchTenantForUnaffiliatedWorkspace() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipService.getMembership(userId, tenantId)).thenThrow(new NotMemberException());

        assertThatThrownBy(() -> authService.switchTenant(userId, tenantId))
                .isInstanceOf(NotMemberException.class)
                .hasMessage("User is not a member of this workspace");

        verify(sessionFactory, never()).buildSession(
                any(UserAccount.class), any(Tenant.class), any(TenantMembership.class));
    }

    @Test
    void shouldUpdatePasswordHash_whenChangePasswordWithValidCredentials() {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashedOld");
        ReflectionTestUtils.setField(user, "id", userId);
        var request = new ChangePasswordRequest(userId, "currentPass", "newPass123", "newPass123");

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPass", "hashedOld")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("hashedNew");

        authService.changePassword(request);

        verify(userAccountRepository).save(user);
        verify(passwordEncoder).encode("newPass123");
    }

    @Test
    void shouldThrowInvalidCredentialsException_whenChangePasswordWithWrongCurrentPassword() {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashedOld");
        var request = new ChangePasswordRequest(userId, "wrongPass", "newPass123", "newPass123");

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "hashedOld")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void shouldThrowIllegalArgumentException_whenNewPasswordsDoNotMatch() {
        UUID userId = UUID.randomUUID();
        var request = new ChangePasswordRequest(userId, "currentPass", "newPass123", "differentPass");

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("newPassword and confirmNewPassword do not match");

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidCredentialsException_whenChangePasswordForUnknownUser() {
        UUID userId = UUID.randomUUID();
        var request = new ChangePasswordRequest(userId, "currentPass", "newPass123", "newPass123");

        when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

}
