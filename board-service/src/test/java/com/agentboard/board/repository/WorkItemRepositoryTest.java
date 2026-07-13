package com.agentboard.board.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentboard.board.domain.Project;
import com.agentboard.board.domain.WorkItem;
import com.agentboard.commons.domain.WorkItemType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Repository slice tests for {@link WorkItemRepository} against a Flyway-migrated
 * PostgreSQL 16 TestContainer.
 *
 * <p>Verifies tenant/project scoping of hierarchy queries and the V18 unique index on
 * {@code (tenant_id, project_id, display_key)}.
 *
 * <p>NOTE: the container is started once per JVM (singleton pattern) so the cached slice
 * context never points at a stopped database if further repository test classes are added.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WorkItemRepositoryTest {

  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("agentboard")
          .withUsername("agentboard")
          .withPassword("agentboard");

  static {
    postgres.start();
  }

  @Autowired
  WorkItemRepository workItemRepository;

  @Autowired
  ProjectRepository projectRepository;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Test
  void findFiltered_hierarchyQuery_isScopedToTenantAndProject() {
    UUID tenantId = UUID.randomUUID();
    Project project = persistProject(tenantId);
    WorkItem feature = workItemRepository.save(new WorkItem(
        project.getId(), tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5, "F1", null));
    WorkItem story = workItemRepository.save(new WorkItem(
        project.getId(), tenantId, WorkItemType.USER_STORY,
        "Story", null, feature.getId(), 1, "U1", null));

    assertThat(workItemRepository.findFiltered(
        project.getId(), tenantId, WorkItemType.USER_STORY, feature.getId(), null, null))
        .hasSize(1)
        .first()
        .extracting(WorkItem::getId)
        .isEqualTo(story.getId());

    assertThat(workItemRepository.findFiltered(
        project.getId(), UUID.randomUUID(), WorkItemType.USER_STORY,
        feature.getId(), null, null))
        .isEmpty();
  }

  @Test
  void findAllByProjectIdAndTenantId_tenantAdataInvisibleToTenantB() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    Project projectA = persistProject(tenantA);
    workItemRepository.save(new WorkItem(
        projectA.getId(), tenantA, WorkItemType.FEATURE,
        "Tenant A Feature", null, null, 5, "F1", null));

    assertThat(workItemRepository.findAllByProjectIdAndTenantId(projectA.getId(), tenantA))
        .hasSize(1);
    assertThat(workItemRepository.findAllByProjectIdAndTenantId(projectA.getId(), tenantB))
        .isEmpty();
  }

  @Test
  void findByIdAndTenantId_rejectsForeignTenant() {
    UUID tenantA = UUID.randomUUID();
    Project projectA = persistProject(tenantA);
    WorkItem feature = workItemRepository.save(new WorkItem(
        projectA.getId(), tenantA, WorkItemType.FEATURE,
        "Scoped Feature", null, null, 5, "F1", null));

    assertThat(workItemRepository.findByIdAndTenantId(feature.getId(), tenantA)).isPresent();
    assertThat(workItemRepository.findByIdAndTenantId(feature.getId(), UUID.randomUUID()))
        .isEmpty();
  }

  @Test
  void displayKey_duplicateWithinSameTenantAndProject_violatesUniqueIndex() {
    UUID tenantId = UUID.randomUUID();
    Project project = persistProject(tenantId);
    workItemRepository.saveAndFlush(new WorkItem(
        project.getId(), tenantId, WorkItemType.FEATURE,
        "First", null, null, 5, "F1", null));

    assertThatThrownBy(() -> workItemRepository.saveAndFlush(new WorkItem(
        project.getId(), tenantId, WorkItemType.FEATURE,
        "Duplicate Key", null, null, 5, "F1", null)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void displayKey_sameKeyInDifferentTenants_isAllowed() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    Project projectA = persistProject(tenantA);
    Project projectB = persistProject(tenantB);

    WorkItem itemA = workItemRepository.saveAndFlush(new WorkItem(
        projectA.getId(), tenantA, WorkItemType.FEATURE,
        "Tenant A F1", null, null, 5, "F1", null));
    WorkItem itemB = workItemRepository.saveAndFlush(new WorkItem(
        projectB.getId(), tenantB, WorkItemType.FEATURE,
        "Tenant B F1", null, null, 5, "F1", null));

    assertThat(itemA.getDisplayKey()).isEqualTo(itemB.getDisplayKey());
    assertThat(itemA.getTenantId()).isNotEqualTo(itemB.getTenantId());
  }

  @Test
  void findMaxDisplayKeySeq_ignoresOtherTenantsAndTypes() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    Project projectA = persistProject(tenantA);
    Project projectB = persistProject(tenantB);

    workItemRepository.saveAndFlush(new WorkItem(
        projectA.getId(), tenantA, WorkItemType.FEATURE,
        "F3 holder", null, null, 5, "F3", null));
    workItemRepository.saveAndFlush(new WorkItem(
        projectB.getId(), tenantB, WorkItemType.FEATURE,
        "Other tenant F9", null, null, 5, "F9", null));

    assertThat(workItemRepository.findMaxDisplayKeySeq(
        projectA.getId(), tenantA, WorkItemType.FEATURE.name())).isEqualTo(3);
    assertThat(workItemRepository.findMaxDisplayKeySeq(
        projectA.getId(), tenantA, WorkItemType.USER_STORY.name())).isZero();
  }

  private Project persistProject(UUID tenantId) {
    UUID suffix = UUID.randomUUID();
    return projectRepository.save(new Project(
        tenantId,
        "Repo Project " + suffix,
        null,
        "agb_repo_" + suffix.toString().replace("-", "")));
  }
}
