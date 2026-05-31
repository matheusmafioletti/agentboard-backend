package com.agentboard.board.repository;

import com.agentboard.board.domain.TenantUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
  @Query("""
      SELECT u FROM TenantUser u
      JOIN TenantMembership m ON m.userId = u.id
      WHERE m.tenantId = :tenantId
      ORDER BY u.email ASC
      """)
  List<TenantUser> findAllByTenantIdOrderByEmail(@Param("tenantId") UUID tenantId);
}
