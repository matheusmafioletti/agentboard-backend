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
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages tenant memberships and member listings. */
@Service
public class MembershipService {

  private final TenantMembershipRepository membershipRepository;
  private final TenantRepository tenantRepository;
  private final UserAccountRepository userAccountRepository;

  /**
   * Creates the service with required repositories.
   */
  public MembershipService(
      TenantMembershipRepository membershipRepository,
      TenantRepository tenantRepository,
      UserAccountRepository userAccountRepository) {
    this.membershipRepository = membershipRepository;
    this.tenantRepository = tenantRepository;
    this.userAccountRepository = userAccountRepository;
  }

  /** Returns all memberships for a user as summaries. */
  public List<TenantMembershipSummary> listByUserId(UUID userId) {
    return membershipRepository.findByUserId(userId).stream()
        .map(this::toSummary)
        .toList();
  }

  /** Returns the membership for a user in a tenant. */
  public TenantMembership getMembership(UUID userId, UUID tenantId) {
    return membershipRepository.findByUserIdAndTenantId(userId, tenantId)
        .orElseThrow(NotMemberException::new);
  }

  /** Ensures the user belongs to the tenant. */
  public TenantMembership assertIsMember(UUID userId, UUID tenantId) {
    return getMembership(userId, tenantId);
  }

  /** Returns whether the user has a membership in the tenant. */
  public boolean isMember(UUID userId, UUID tenantId) {
    return membershipRepository.existsByUserIdAndTenantId(userId, tenantId);
  }

  /** Creates an admin membership for a user in a tenant. */
  @Transactional
  public TenantMembership createAdminMembership(UUID userId, UUID tenantId) {
    return membershipRepository.save(
        new TenantMembership(userId, tenantId, MembershipRole.ADMIN));
  }

  /** Creates a user membership for a user in a tenant. */
  @Transactional
  public TenantMembership createUserMembership(UUID userId, UUID tenantId) {
    return membershipRepository.save(
        new TenantMembership(userId, tenantId, MembershipRole.USER));
  }

  /** Lists all members of a tenant with user details. */
  public List<MemberResponse> listMembers(UUID tenantId) {
    return membershipRepository.findByTenantId(tenantId).stream()
        .map(m -> {
          UserAccount user = userAccountRepository.findById(m.getUserId()).orElseThrow();
          return new MemberResponse(
              user.getId(), user.getName(), user.getEmail(), m.getRole(), m.getJoinedAt());
        })
        .toList();
  }

  /**
   * Revokes a user's membership in a tenant.
   *
   * @throws LastAdminException if revoking would leave the tenant without an admin
   */
  @Transactional
  public void revokeMembership(UUID tenantId, UUID targetUserId) {
    TenantMembership membership = membershipRepository
        .findByUserIdAndTenantId(targetUserId, tenantId)
        .orElseThrow(NotMemberException::new);

    if (membership.getRole() == MembershipRole.ADMIN
        && membershipRepository.countByTenantIdAndRole(tenantId, MembershipRole.ADMIN) <= 1) {
      throw new LastAdminException();
    }

    membershipRepository.delete(membership);
  }

  /** Ensures the caller is an admin of the tenant matching the JWT active tenant. */
  public void assertAdminOfActiveTenant(UUID callerUserId, UUID jwtTenantId, UUID pathTenantId) {
    if (!jwtTenantId.equals(pathTenantId)) {
      throw new ForbiddenOperationException("Tenant in token does not match requested workspace");
    }
    TenantMembership membership = assertIsMember(callerUserId, pathTenantId);
    if (membership.getRole() != MembershipRole.ADMIN) {
      throw new ForbiddenOperationException("Administrator role required");
    }
  }

  private TenantMembershipSummary toSummary(TenantMembership membership) {
    Tenant tenant = tenantRepository.findById(membership.getTenantId()).orElseThrow();
    return new TenantMembershipSummary(
        membership.getTenantId(),
        tenant.getName(),
        membership.getRole(),
        membership.getJoinedAt());
  }
}
