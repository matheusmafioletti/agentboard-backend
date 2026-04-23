package com.agentboard.board.repository;

import com.agentboard.board.domain.Task;
import com.agentboard.commons.multitenancy.TenantAwareRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Task} entities, always scoped by {@code tenantId}. */
public interface TaskRepository extends JpaRepository<Task, UUID>, TenantAwareRepository {

  /** Returns all tasks for a given feature card within the tenant. */
  List<Task> findByFeatureCardIdAndTenantId(UUID featureCardId, UUID tenantId);

  /** Returns a task by its id within the tenant. */
  Optional<Task> findByIdAndTenantId(UUID id, UUID tenantId);

  /**
   * Returns the count of incomplete (non-completed) tasks for a feature card within the tenant.
   * Used to determine whether auto-move to REVIEW should trigger.
   */
  long countByFeatureCardIdAndTenantIdAndCompletedFalse(UUID featureCardId, UUID tenantId);
}
