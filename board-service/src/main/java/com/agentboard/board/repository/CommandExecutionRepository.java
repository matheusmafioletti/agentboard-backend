package com.agentboard.board.repository;

import com.agentboard.board.domain.CommandExecution;
import com.agentboard.commons.multitenancy.TenantAwareRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link CommandExecution} entities, always scoped by {@code tenantId}. */
public interface CommandExecutionRepository
    extends JpaRepository<CommandExecution, UUID>, TenantAwareRepository {

  /**
   * Returns all command executions for a feature card within the tenant, ordered from newest to
   * oldest.
   */
  List<CommandExecution> findByFeatureCardIdAndTenantIdOrderByStartedAtDesc(
      UUID featureCardId, UUID tenantId);
}
