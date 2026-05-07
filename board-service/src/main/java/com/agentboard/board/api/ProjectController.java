package com.agentboard.board.api;

import com.agentboard.board.domain.Project;
import com.agentboard.board.security.ProjectPrincipal;
import com.agentboard.board.service.ProjectService;
import com.agentboard.commons.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for Project management.
 *
 * <ul>
 *   <li>{@code POST /api/v1/projects} — creates a project for the authenticated tenant</li>
 *   <li>{@code GET /api/v1/projects} — lists all projects for the authenticated tenant</li>
 *   <li>{@code GET /api/v1/projects/me} — returns the project associated with the API key</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

  private final ProjectService projectService;

  /** Creates the controller backed by the given service. */
  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  /** Creates a new project for the currently authenticated tenant. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
    UUID tenantId = TenantContext.get();
    Project project = projectService.createProject(tenantId, request.name(),
        request.constitutionContent());
    return ProjectResponse.from(project);
  }

  /** Lists all projects belonging to the authenticated tenant. */
  @GetMapping
  public List<ProjectResponse> listProjects() {
    UUID tenantId = TenantContext.get();
    return projectService.listByTenant(tenantId).stream()
        .map(ProjectResponse::from)
        .toList();
  }

  /** Returns a single project by ID, scoped to the authenticated tenant. */
  @GetMapping("/{id}")
  public ProjectResponse getProject(@PathVariable UUID id) {
    UUID tenantId = TenantContext.get();
    Project project = projectService.getByIdAndTenant(id, tenantId);
    return ProjectResponse.from(project);
  }

  /** Updates the name and/or constitution of an existing project. */
  @PutMapping("/{id}")
  public ProjectResponse updateProject(
      @PathVariable UUID id,
      @RequestBody UpdateProjectRequest request) {
    UUID tenantId = TenantContext.get();
    Project project = projectService.updateProject(
        id, tenantId, request.name(), request.constitutionContent());
    return ProjectResponse.from(project);
  }

  /**
   * Returns the project associated with the project API key used in the current request.
   *
   * <p>This endpoint is only accessible to callers authenticated with a project API key
   * ({@code Authorization: Bearer agb_...}). Used by the {@code get_constitution} MCP tool.
   */
  @GetMapping("/me")
  public ProjectResponse getMyProject(
      @AuthenticationPrincipal ProjectPrincipal principal) {
    UUID tenantId = TenantContext.get();
    List<Project> projects = projectService.listByTenant(tenantId);
    Project project = projects.stream()
        .filter(p -> p.getId().equals(principal.projectId()))
        .findFirst()
        .orElseThrow(() -> new com.agentboard.commons.exceptions.ResourceNotFoundException(
            "Project not found"));
    return ProjectResponse.from(project);
  }

  /** Request body for project creation. */
  public record CreateProjectRequest(
      @NotBlank @Size(max = 255) String name,
      String constitutionContent
  ) {}

  /** Request body for project update (all fields optional — omit to keep current value). */
  public record UpdateProjectRequest(String name, String constitutionContent) {}

  /** Response DTO for a project. */
  public record ProjectResponse(
      UUID id,
      UUID tenantId,
      String name,
      String constitutionContent,
      String apiKey,
      String createdAt,
      String updatedAt
  ) {
    static ProjectResponse from(Project p) {
      return new ProjectResponse(
          p.getId(),
          p.getTenantId(),
          p.getName(),
          p.getConstitutionContent(),
          p.getApiKey(),
          p.getCreatedAt().toString(),
          p.getUpdatedAt().toString()
      );
    }
  }
}
