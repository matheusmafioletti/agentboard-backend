package com.agentboard.board.repository;

import com.agentboard.board.domain.TenantUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Read-only data access for {@link TenantUser} (auth-service's {@code user_account} table).
 *
 * <p>IMPORTANT: Never call {@code save()} or {@code delete()} from board-service —
 * only auth-service owns the lifecycle of these records.
 */
public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {

  /**
   * Returns all users belonging to the given tenant, ordered by email for stable UI lists.
   *
   * @param tenantId the owning tenant
   * @return ordered list of tenant users
   */
  List<TenantUser> findAllByTenantIdOrderByEmail(UUID tenantId);
}
