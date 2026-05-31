package com.agentboard.auth.repository;

import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.auth.domain.TenantMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for {@link TenantMembership} entities. */
@Repository
public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

  /** Returns all memberships for the given user. */
  List<TenantMembership> findByUserId(UUID userId);

  /** Returns the membership for a user in a specific tenant, if present. */
  Optional<TenantMembership> findByUserIdAndTenantId(UUID userId, UUID tenantId);

  /** Returns all memberships in a tenant. */
  List<TenantMembership> findByTenantId(UUID tenantId);

  /** Counts memberships with the given role in a tenant. */
  long countByTenantIdAndRole(UUID tenantId, MembershipRole role);

  /** Returns whether a membership exists for the user and tenant. */
  boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
}
