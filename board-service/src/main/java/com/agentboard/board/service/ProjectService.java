package com.agentboard.board.service;

import com.agentboard.board.domain.Project;
import com.agentboard.board.repository.ProjectRepository;
import com.agentboard.commons.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for Project lifecycle: creation, retrieval, and tenant-scoped queries.
 *
 * <p>API keys are generated server-side with the {@code agb_} prefix plus a random UUID.
 */
@Service
public class ProjectService {

  private final ProjectRepository projectRepository;

  /** Creates the service backed by the given repository. */
  public ProjectService(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  /**
   * Creates a new Project for the given tenant with an auto-generated API key.
   *
   * @param tenantId            the owning tenant
   * @param name                unique project name within the tenant
   * @param constitutionContent optional initial constitution (null → default template)
   * @return the persisted project
   */
  @Transactional
  public Project createProject(UUID tenantId, String name, String constitutionContent) {
    String apiKey = "agb_" + UUID.randomUUID().toString().replace("-", "");
    Project project = new Project(tenantId, name, constitutionContent, apiKey);
    return projectRepository.save(project);
  }

  /**
   * Returns all projects belonging to the given tenant.
   *
   * @param tenantId the owning tenant
   * @return list of projects (may be empty)
   */
  @Transactional(readOnly = true)
  public List<Project> listByTenant(UUID tenantId) {
    return projectRepository.findAllByTenantId(tenantId);
  }

  /**
   * Returns the project identified by id and tenant.
   *
   * @param projectId the project identifier
   * @param tenantId  the owning tenant (for isolation enforcement)
   * @return the matching project
   * @throws ResourceNotFoundException if no matching project is found
   */
  @Transactional(readOnly = true)
  public Project getByIdAndTenant(UUID projectId, UUID tenantId) {
    return projectRepository.findByIdAndTenantId(projectId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Project " + projectId + " not found for tenant " + tenantId));
  }

  /**
   * Updates the name and/or constitution of an existing project.
   *
   * @param projectId           the project to update
   * @param tenantId            the owning tenant (for isolation enforcement)
   * @param name                new project name (null = keep existing)
   * @param constitutionContent new constitution content (null = keep existing)
   * @return the updated project
   * @throws ResourceNotFoundException if the project is not found for the given tenant
   */
  @Transactional
  public Project updateProject(UUID projectId, UUID tenantId, String name,
      String constitutionContent) {
    Project project = getByIdAndTenant(projectId, tenantId);
    if (name != null) {
      project.updateName(name);
    }
    if (constitutionContent != null) {
      project.updateConstitution(constitutionContent);
    }
    return projectRepository.save(project);
  }

  /**
   * Returns the project associated with the given API key.
   *
   * @param apiKey the raw project API key
   * @return the matching project
   * @throws ResourceNotFoundException if the key is not associated with any project
   */
  @Transactional(readOnly = true)
  public Project getByApiKey(String apiKey) {
    return projectRepository.findByApiKey(apiKey)
        .orElseThrow(() -> new ResourceNotFoundException(
            "No project found for the given API key"));
  }
}
