package com.agentboard.board.repository;

import com.agentboard.board.domain.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Project} entities.
 *
 * <p>All queries are scoped to a tenant to prevent cross-tenant data exposure.
 */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

  /**
   * Finds the project matching the given API key and tenant.
   *
   * @param apiKey   the raw project API key (stored as plain text; hashing is not applied here)
   * @param tenantId the tenant that owns the project
   * @return the matching project, or empty if not found
   */
  Optional<Project> findByApiKeyAndTenantId(String apiKey, UUID tenantId);

  /**
   * Finds a project by its API key regardless of tenant (used during filter-level tenant
   * resolution before tenantId is known).
   *
   * @param apiKey the raw project API key
   * @return the matching project, or empty if not found
   */
  Optional<Project> findByApiKey(String apiKey);

  /**
   * Returns all projects belonging to a tenant.
   *
   * @param tenantId the owning tenant
   * @return ordered list of projects (creation order not guaranteed)
   */
  List<Project> findAllByTenantId(UUID tenantId);

  /**
   * Finds a project by its id and owning tenant — enforces cross-tenant isolation.
   *
   * @param id       the project identifier
   * @param tenantId the owning tenant
   * @return the matching project, or empty if not found or tenant mismatch
   */
  Optional<Project> findByIdAndTenantId(UUID id, UUID tenantId);
}
