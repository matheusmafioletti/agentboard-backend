package com.agentboard.auth.repository;

import com.agentboard.auth.domain.InviteStatus;
import com.agentboard.auth.domain.TenantInvite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for {@link TenantInvite} entities. */
@Repository
public interface TenantInviteRepository extends JpaRepository<TenantInvite, UUID> {

  /** Returns a pending invite for the tenant and email, if one exists. */
  Optional<TenantInvite> findByTenantIdAndEmailAndStatus(
      UUID tenantId, String email, InviteStatus status);

  /** Returns all invites for a tenant. */
  List<TenantInvite> findByTenantId(UUID tenantId);

  /** Returns an invite by token hash. */
  Optional<TenantInvite> findByTokenHash(String tokenHash);
}
