package com.agentboard.auth.service;

import com.agentboard.auth.domain.InviteStatus;
import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantInvite;
import com.agentboard.auth.domain.TenantMembership;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.AcceptInviteRequest;
import com.agentboard.auth.dto.IdentifyInviteResponse;
import com.agentboard.auth.dto.InvitePreviewResponse;
import com.agentboard.auth.dto.InviteResponse;
import com.agentboard.auth.dto.SessionResponse;
import com.agentboard.auth.dto.VerifyInviteCredentialsResponse;
import com.agentboard.auth.exception.AlreadyMemberException;
import com.agentboard.auth.exception.DuplicatePendingInviteException;
import com.agentboard.auth.exception.ForbiddenOperationException;
import com.agentboard.auth.exception.InviteGoneException;
import com.agentboard.auth.exception.InvalidCredentialsException;
import com.agentboard.auth.repository.TenantInviteRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import com.agentboard.commons.domain.DataSource;
import com.agentboard.commons.policy.DataSourcePolicy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    private static final String INVITE_BASE_URL = "http://localhost:5173";

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
                dataSourcePolicy,
                INVITE_BASE_URL);
        lenient().when(tenantRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            Tenant tenant = new Tenant("Test Tenant");
            ReflectionTestUtils.setField(tenant, "id", invocation.getArgument(0));
            return Optional.of(tenant);
        });
    }

    @Test
    void shouldReturnInviteResponse_whenCreateInviteForNewEmail() {
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        UUID inviteId = UUID.randomUUID();
        String email = "Invited@Example.com";
        TenantInvite savedInvite = new TenantInvite(
                tenantId, "invited@example.com", "token-hash", invitedBy,
                OffsetDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(savedInvite, "id", inviteId);

        when(userAccountRepository.findByEmail("invited@example.com")).thenReturn(Optional.empty());
        when(inviteRepository.findByTenantIdAndEmailAndStatus(
                tenantId, "invited@example.com", InviteStatus.PENDING))
                .thenReturn(Optional.empty());
        when(inviteRepository.save(any(TenantInvite.class))).thenReturn(savedInvite);

        InviteResponse response = inviteService.createInvite(tenantId, invitedBy, email);

        assertThat(response.id()).isEqualTo(inviteId);
        assertThat(response.email()).isEqualTo("invited@example.com");
        assertThat(response.status()).isEqualTo(InviteStatus.PENDING);
        assertThat(response.inviteUrl()).startsWith(INVITE_BASE_URL + "/invite/");

        verify(inviteRepository).save(any(TenantInvite.class));
    }

    @Test
    void shouldThrowAlreadyMemberException_whenCreateInviteForExistingMember() {
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount("Member", "member@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);

        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(membershipService.isMember(userId, tenantId)).thenReturn(true);

        assertThatThrownBy(() -> inviteService.createInvite(tenantId, invitedBy, "member@example.com"))
                .isInstanceOf(AlreadyMemberException.class)
                .hasMessage("User is already a member of this workspace");

        verify(inviteRepository, never()).save(any());
    }

    @Test
    void shouldThrowDuplicatePendingInviteException_whenPendingInviteExists() {
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        TenantInvite pendingInvite = new TenantInvite(
                tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().plusDays(7));

        when(userAccountRepository.findByEmail("guest@example.com")).thenReturn(Optional.empty());
        when(inviteRepository.findByTenantIdAndEmailAndStatus(
                tenantId, "guest@example.com", InviteStatus.PENDING))
                .thenReturn(Optional.of(pendingInvite));

        assertThatThrownBy(() -> inviteService.createInvite(tenantId, invitedBy, "guest@example.com"))
                .isInstanceOf(DuplicatePendingInviteException.class)
                .hasMessage("A pending invite already exists for this email");

        verify(inviteRepository, never()).save(any());
    }

    @Test
    void shouldReturnInviteList_whenListInvites() {
        UUID tenantId = UUID.randomUUID();
        UUID inviteId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        TenantInvite invite = new TenantInvite(
                tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(invite, "id", inviteId);

        when(inviteRepository.findByTenantId(tenantId)).thenReturn(List.of(invite));

        List<InviteResponse> invites = inviteService.listInvites(tenantId);

        assertThat(invites).hasSize(1);
        assertThat(invites.getFirst())
                .returns(inviteId, InviteResponse::id)
                .returns("guest@example.com", InviteResponse::email)
                .returns(InviteStatus.PENDING, InviteResponse::status);
        assertThat(invites.getFirst().inviteUrl()).isNull();
    }

    @Test
    void shouldMarkCancelled_whenCancelPendingInvite() {
        UUID tenantId = UUID.randomUUID();
        UUID inviteId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        TenantInvite invite = new TenantInvite(
                tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(invite, "id", inviteId);

        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(inviteRepository.save(invite)).thenReturn(invite);

        inviteService.cancelInvite(tenantId, inviteId);

        assertThat(invite.getStatus()).isEqualTo(InviteStatus.CANCELLED);
        verify(inviteRepository).save(invite);
    }

    @Test
    void shouldThrowInviteGoneException_whenCancelUnknownInvite() {
        UUID tenantId = UUID.randomUUID();
        UUID inviteId = UUID.randomUUID();

        when(inviteRepository.findById(inviteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.cancelInvite(tenantId, inviteId))
                .isInstanceOf(InviteGoneException.class)
                .hasMessage("Invite not found");
    }

    @Test
    void shouldThrowInviteGoneException_whenCancelNonPendingInvite() {
        UUID tenantId = UUID.randomUUID();
        UUID inviteId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        TenantInvite invite = new TenantInvite(
                tenantId, "guest@example.com", "hash", invitedBy, OffsetDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(invite, "id", inviteId);
        invite.markCancelled();

        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> inviteService.cancelInvite(tenantId, inviteId))
                .isInstanceOf(InviteGoneException.class)
                .hasMessage("Invite is no longer pending");
    }

    @Test
    void shouldReturnIdentifyResponse_whenInviteEmailMatches() {
        String rawToken = "raw-invite-token";
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        TenantInvite invite = activeInvite(tenantId, "guest@example.com", rawToken, invitedBy);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);

        when(inviteRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(invite));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userAccountRepository.findByEmail("guest@example.com")).thenReturn(Optional.empty());

        IdentifyInviteResponse response = inviteService.identifyInvite(rawToken, "Guest@Example.com");

        assertThat(response)
                .returns("Workspace", IdentifyInviteResponse::tenantName)
                .returns("guest@example.com", IdentifyInviteResponse::inviteEmail)
                .returns(false, IdentifyInviteResponse::accountExists);
    }

    @Test
    void shouldThrowForbiddenOperationException_whenIdentifyWithWrongEmail() {
        String rawToken = "raw-invite-token";
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        TenantInvite invite = activeInvite(tenantId, "guest@example.com", rawToken, invitedBy);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);

        when(inviteRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(invite));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> inviteService.identifyInvite(rawToken, "other@example.com"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Email does not match the invitation");
    }

    @Test
    void shouldReturnVerifyResponse_whenCredentialsAreValid() {
        String rawToken = "raw-invite-token";
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantInvite invite = activeInvite(tenantId, "guest@example.com", rawToken, invitedBy);
        UserAccount user = new UserAccount("Guest User", "guest@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);

        when(inviteRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(invite));
        when(userAccountRepository.findByEmail("guest@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        VerifyInviteCredentialsResponse response = inviteService.verifyCredentials(
                rawToken, "guest@example.com", "password123");

        assertThat(response)
                .returns("Guest User", VerifyInviteCredentialsResponse::name)
                .returns("guest@example.com", VerifyInviteCredentialsResponse::email);
    }

    @Test
    void shouldThrowInvalidCredentialsException_whenVerifyWithWrongPassword() {
        String rawToken = "raw-invite-token";
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        TenantInvite invite = activeInvite(tenantId, "guest@example.com", rawToken, invitedBy);
        UserAccount user = new UserAccount("Guest User", "guest@example.com", "hashed-password");

        when(inviteRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(invite));
        when(userAccountRepository.findByEmail("guest@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> inviteService.verifyCredentials(
                rawToken, "guest@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void shouldReturnPreview_whenInviteIsActive() {
        String rawToken = "raw-invite-token";
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        TenantInvite invite = activeInvite(tenantId, "guest@example.com", rawToken, invitedBy);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);

        when(inviteRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(invite));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userAccountRepository.findByEmail("guest@example.com")).thenReturn(Optional.empty());

        InvitePreviewResponse response = inviteService.getInvitePreview(rawToken);

        assertThat(response)
                .returns("Workspace", InvitePreviewResponse::tenantName)
                .returns("guest@example.com", InvitePreviewResponse::email)
                .returns(InviteStatus.PENDING, InvitePreviewResponse::status)
                .returns(true, InvitePreviewResponse::requiresRegistration);
    }

    @Test
    void shouldThrowInviteGoneException_whenInviteIsExpired() {
        String rawToken = "raw-invite-token";
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        TenantInvite invite = new TenantInvite(
                tenantId, "guest@example.com", sha256Hex(rawToken), invitedBy,
                OffsetDateTime.now().minusDays(1));

        when(inviteRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(invite));
        when(inviteRepository.save(invite)).thenReturn(invite);

        assertThatThrownBy(() -> inviteService.getInvitePreview(rawToken))
                .isInstanceOf(InviteGoneException.class)
                .hasMessage("Invite is no longer valid");

        assertThat(invite.getStatus()).isEqualTo(InviteStatus.EXPIRED);
    }

    @Test
    void shouldReturnSessionResponse_whenAcceptInviteForNewUser() {
        String rawToken = "raw-invite-token";
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantInvite invite = activeInvite(tenantId, "guest@example.com", rawToken, invitedBy);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        UserAccount user = new UserAccount("New Guest", "guest@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.USER);
        SessionResponse session = new SessionResponse(
                "jwt-token", userId, tenantId, "Workspace", "guest@example.com", "New Guest",
                MembershipRole.USER);
        var request = new AcceptInviteRequest("New Guest", "password123", null);

        when(inviteRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(invite));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userAccountRepository.findByEmail("guest@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(user);
        when(membershipService.isMember(userId, tenantId)).thenReturn(false);
        when(membershipService.createUserMembership(userId, tenantId, DataSource.MANUAL))
            .thenReturn(membership);
        when(membershipService.getMembership(userId, tenantId)).thenReturn(membership);
        when(sessionFactory.buildSession(user, tenant, membership)).thenReturn(session);
        when(inviteRepository.save(invite)).thenReturn(invite);

        SessionResponse response = inviteService.acceptInvite(rawToken, request);

        assertThat(response)
                .returns("jwt-token", SessionResponse::token)
                .returns(userId, SessionResponse::userId)
                .returns(tenantId, SessionResponse::tenantId);
        assertThat(invite.getStatus()).isEqualTo(InviteStatus.ACCEPTED);
        verify(membershipService).createUserMembership(userId, tenantId, DataSource.MANUAL);
    }

    @Test
    void shouldReturnSessionResponse_whenAcceptInviteForExistingUser() {
        String rawToken = "raw-invite-token";
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantInvite invite = activeInvite(tenantId, "guest@example.com", rawToken, invitedBy);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        UserAccount user = new UserAccount("Guest User", "guest@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.USER);
        SessionResponse session = new SessionResponse(
                "jwt-token", userId, tenantId, "Workspace", "guest@example.com", "Guest User",
                MembershipRole.USER);
        var request = new AcceptInviteRequest(null, "password123", "guest@example.com");

        when(inviteRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(invite));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userAccountRepository.findByEmail("guest@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(membershipService.isMember(userId, tenantId)).thenReturn(false);
        when(membershipService.createUserMembership(userId, tenantId, DataSource.MANUAL))
            .thenReturn(membership);
        when(membershipService.getMembership(userId, tenantId)).thenReturn(membership);
        when(sessionFactory.buildSession(user, tenant, membership)).thenReturn(session);
        when(inviteRepository.save(invite)).thenReturn(invite);

        SessionResponse response = inviteService.acceptInvite(rawToken, request);

        assertThat(response)
                .returns("jwt-token", SessionResponse::token)
                .returns(userId, SessionResponse::userId);
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void shouldThrowAlreadyMemberException_whenAcceptInviteForMember() {
        String rawToken = "raw-invite-token";
        UUID tenantId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantInvite invite = activeInvite(tenantId, "guest@example.com", rawToken, invitedBy);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        UserAccount user = new UserAccount("Guest User", "guest@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);
        var request = new AcceptInviteRequest(null, "password123", "guest@example.com");

        when(inviteRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(invite));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userAccountRepository.findByEmail("guest@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(membershipService.isMember(userId, tenantId)).thenReturn(true);

        assertThatThrownBy(() -> inviteService.acceptInvite(rawToken, request))
                .isInstanceOf(AlreadyMemberException.class)
                .hasMessage("User is already a member of this workspace");

        verify(membershipService, never()).createUserMembership(any(), any(), any());
    }

    private static TenantInvite activeInvite(
            UUID tenantId, String email, String rawToken, UUID invitedBy) {
        return new TenantInvite(
                tenantId, email, sha256Hex(rawToken), invitedBy, OffsetDateTime.now().plusDays(7));
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
