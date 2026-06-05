package com.agentboard.auth.service;

import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantMembership;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.MemberResponse;
import com.agentboard.auth.dto.TenantMembershipSummary;
import com.agentboard.auth.exception.ForbiddenOperationException;
import com.agentboard.auth.exception.LastAdminException;
import com.agentboard.auth.exception.NotMemberException;
import com.agentboard.auth.repository.TenantMembershipRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import java.util.List;
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
    void shouldReturnMembershipSummaries_whenListByUserId() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.ADMIN);
        Tenant tenant = new Tenant("Workspace");
        ReflectionTestUtils.setField(tenant, "id", tenantId);

        when(membershipRepository.findByUserId(userId)).thenReturn(List.of(membership));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        List<TenantMembershipSummary> summaries = membershipService.listByUserId(userId);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst())
                .returns(tenantId, TenantMembershipSummary::tenantId)
                .returns("Workspace", TenantMembershipSummary::tenantName)
                .returns(MembershipRole.ADMIN, TenantMembershipSummary::role);
    }

    @Test
    void shouldReturnMembership_whenGetMembershipForMember() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.USER);

        when(membershipRepository.findByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(membership));

        TenantMembership result = membershipService.getMembership(userId, tenantId);

        assertThat(result).isSameAs(membership);
    }

    @Test
    void shouldThrowNotMemberException_whenGetMembershipForNonMember() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(membershipRepository.findByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> membershipService.getMembership(userId, tenantId))
                .isInstanceOf(NotMemberException.class)
                .hasMessage("User is not a member of this workspace");
    }

    @Test
    void shouldReturnTrue_whenIsMember() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(membershipRepository.existsByUserIdAndTenantId(userId, tenantId)).thenReturn(true);

        assertThat(membershipService.isMember(userId, tenantId)).isTrue();
    }

    @Test
    void shouldReturnFalse_whenIsNotMember() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(membershipRepository.existsByUserIdAndTenantId(userId, tenantId)).thenReturn(false);

        assertThat(membershipService.isMember(userId, tenantId)).isFalse();
    }

    @Test
    void shouldSaveAdminMembership_whenCreateAdminMembership() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.ADMIN);

        when(membershipRepository.save(any(TenantMembership.class))).thenReturn(membership);

        TenantMembership result = membershipService.createAdminMembership(userId, tenantId);

        assertThat(result.getRole()).isEqualTo(MembershipRole.ADMIN);
        verify(membershipRepository).save(any(TenantMembership.class));
    }

    @Test
    void shouldSaveUserMembership_whenCreateUserMembership() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.USER);

        when(membershipRepository.save(any(TenantMembership.class))).thenReturn(membership);

        TenantMembership result = membershipService.createUserMembership(userId, tenantId);

        assertThat(result.getRole()).isEqualTo(MembershipRole.USER);
        verify(membershipRepository).save(any(TenantMembership.class));
    }

    @Test
    void shouldReturnMemberResponses_whenListMembers() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(userId, tenantId, MembershipRole.ADMIN);
        UserAccount user = new UserAccount("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", userId);

        when(membershipRepository.findByTenantId(tenantId)).thenReturn(List.of(membership));
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));

        List<MemberResponse> members = membershipService.listMembers(tenantId);

        assertThat(members).hasSize(1);
        assertThat(members.getFirst())
                .returns(userId, MemberResponse::userId)
                .returns("Test User", MemberResponse::name)
                .returns("user@example.com", MemberResponse::email)
                .returns(MembershipRole.ADMIN, MemberResponse::role);
    }

    @Test
    void shouldDeleteMembership_whenRevokeNonAdminMember() {
        UUID tenantId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(targetUserId, tenantId, MembershipRole.USER);

        when(membershipRepository.findByUserIdAndTenantId(targetUserId, tenantId))
                .thenReturn(Optional.of(membership));

        membershipService.revokeMembership(tenantId, targetUserId);

        verify(membershipRepository).delete(membership);
        verify(membershipRepository, never()).countByTenantIdAndRole(any(), any());
    }

    @Test
    void shouldThrowLastAdminException_whenRevokeLastAdmin() {
        UUID tenantId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(targetUserId, tenantId, MembershipRole.ADMIN);

        when(membershipRepository.findByUserIdAndTenantId(targetUserId, tenantId))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.countByTenantIdAndRole(tenantId, MembershipRole.ADMIN))
                .thenReturn(1L);

        assertThatThrownBy(() -> membershipService.revokeMembership(tenantId, targetUserId))
                .isInstanceOf(LastAdminException.class)
                .hasMessage("Cannot remove the last administrator of this workspace");

        verify(membershipRepository, never()).delete(any());
    }

    @Test
    void shouldThrowNotMemberException_whenRevokeUnknownMember() {
        UUID tenantId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(membershipRepository.findByUserIdAndTenantId(targetUserId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> membershipService.revokeMembership(tenantId, targetUserId))
                .isInstanceOf(NotMemberException.class)
                .hasMessage("User is not a member of this workspace");
    }

    @Test
    void shouldPass_whenAssertAdminOfActiveTenantForAdmin() {
        UUID callerUserId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(callerUserId, tenantId, MembershipRole.ADMIN);

        when(membershipRepository.findByUserIdAndTenantId(callerUserId, tenantId))
                .thenReturn(Optional.of(membership));

        membershipService.assertAdminOfActiveTenant(callerUserId, tenantId, tenantId);

        verify(membershipRepository).findByUserIdAndTenantId(callerUserId, tenantId);
    }

    @Test
    void shouldThrowForbiddenOperationException_whenJwtTenantDoesNotMatchPath() {
        UUID callerUserId = UUID.randomUUID();
        UUID jwtTenantId = UUID.randomUUID();
        UUID pathTenantId = UUID.randomUUID();

        assertThatThrownBy(() -> membershipService.assertAdminOfActiveTenant(
                callerUserId, jwtTenantId, pathTenantId))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Tenant in token does not match requested workspace");

        verify(membershipRepository, never()).findByUserIdAndTenantId(any(), any());
    }

    @Test
    void shouldThrowForbiddenOperationException_whenCallerIsNotAdmin() {
        UUID callerUserId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TenantMembership membership = new TenantMembership(callerUserId, tenantId, MembershipRole.USER);

        when(membershipRepository.findByUserIdAndTenantId(callerUserId, tenantId))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> membershipService.assertAdminOfActiveTenant(
                callerUserId, tenantId, tenantId))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Administrator role required");
    }
}
