package com.agentboard.board.repository;

import com.agentboard.board.domain.Artifact;
import com.agentboard.commons.multitenancy.TenantAwareRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Artifact} entities, always scoped by {@code tenantId}. */
public interface ArtifactRepository extends JpaRepository<Artifact, UUID>, TenantAwareRepository {

  /** Returns all artifacts for a given feature card within the tenant, ordered by creation time. */
  List<Artifact> findByFeatureCardIdAndTenantIdOrderByCreatedAtAsc(
      UUID featureCardId, UUID tenantId);
}
