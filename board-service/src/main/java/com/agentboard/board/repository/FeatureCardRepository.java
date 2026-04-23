package com.agentboard.board.repository;

import com.agentboard.board.domain.FeatureCard;
import com.agentboard.commons.multitenancy.TenantAwareRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Tenant-scoped repository for {@link FeatureCard} entities. */
public interface FeatureCardRepository
    extends JpaRepository<FeatureCard, UUID>, TenantAwareRepository {

  /** Finds a card by id, enforcing tenant ownership. */
  Optional<FeatureCard> findByIdAndTenantId(UUID id, UUID tenantId);

  /** Returns all cards in a column for a tenant, ordered by display position. */
  List<FeatureCard> findByColumnIdAndTenantIdOrderByDisplayOrderAsc(UUID columnId, UUID tenantId);

  /** Returns the highest displayOrder in a column, or empty if the column has no cards. */
  @Query("""
      SELECT MAX(f.displayOrder)
      FROM FeatureCard f
      WHERE f.columnId = :columnId AND f.tenantId = :tenantId
      """)
  Optional<Integer> findMaxDisplayOrderByColumnIdAndTenantId(
      @Param("columnId") UUID columnId,
      @Param("tenantId") UUID tenantId);

  /** Returns all cards for a tenant, ordered by creation time descending. */
  List<FeatureCard> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
