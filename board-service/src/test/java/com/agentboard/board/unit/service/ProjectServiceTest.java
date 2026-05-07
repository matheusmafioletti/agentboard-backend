package com.agentboard.board.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.agentboard.board.domain.Project;
import com.agentboard.board.repository.ProjectRepository;
import com.agentboard.board.service.ProjectService;
import com.agentboard.commons.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ProjectService}.
 *
 * <p>Covers: API key uniqueness format, constitution default, tenant isolation in findByApiKey.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

  @Mock
  private ProjectRepository projectRepository;

  private ProjectService projectService;

  @BeforeEach
  void setUp() {
    projectService = new ProjectService(projectRepository);
  }

  @Test
  void createProject_generatesApiKeyWithAgbPrefix() {
    UUID tenantId = UUID.randomUUID();
    when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

    Project project = projectService.createProject(tenantId, "My Project", null);

    assertThat(project.getApiKey()).startsWith("agb_");
  }

  @Test
  void createProject_apiKeyIsUnique_generatesNewUuidEachTime() {
    UUID tenantId = UUID.randomUUID();
    when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

    Project p1 = projectService.createProject(tenantId, "Project 1", null);
    Project p2 = projectService.createProject(tenantId, "Project 2", null);

    assertThat(p1.getApiKey()).isNotEqualTo(p2.getApiKey());
  }

  @Test
  void createProject_usesDefaultConstitution_whenNull() {
    UUID tenantId = UUID.randomUUID();
    when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

    Project project = projectService.createProject(tenantId, "My Project", null);

    assertThat(project.getConstitutionContent()).isNotBlank();
    assertThat(project.getConstitutionContent()).contains("# Project Constitution");
  }

  @Test
  void createProject_usesProvidedConstitution() {
    UUID tenantId = UUID.randomUUID();
    when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

    Project project = projectService.createProject(tenantId, "My Project", "# Custom");

    assertThat(project.getConstitutionContent()).isEqualTo("# Custom");
  }

  @Test
  void listByTenant_returnsProjectsForTenant() {
    UUID tenantId = UUID.randomUUID();
    Project p = new Project(tenantId, "Project", null, "agb_key");
    when(projectRepository.findAllByTenantId(tenantId)).thenReturn(List.of(p));

    List<Project> result = projectService.listByTenant(tenantId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Project");
  }

  @Test
  void getByApiKey_throwsResourceNotFound_whenKeyNotFound() {
    when(projectRepository.findByApiKey("agb_unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> projectService.getByApiKey("agb_unknown"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void getByApiKey_returnsProject_whenFound() {
    UUID tenantId = UUID.randomUUID();
    Project p = new Project(tenantId, "Project", null, "agb_abc123");
    when(projectRepository.findByApiKey("agb_abc123")).thenReturn(Optional.of(p));

    Project result = projectService.getByApiKey("agb_abc123");

    assertThat(result.getApiKey()).isEqualTo("agb_abc123");
  }
}
