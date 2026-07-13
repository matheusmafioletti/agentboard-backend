package com.agentboard.board.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.agentboard.board.domain.Project;
import com.agentboard.board.domain.WorkItem;
import com.agentboard.board.domain.WorkItemDisplayKeys;
import com.agentboard.board.repository.ProjectRepository;
import com.agentboard.board.repository.WorkItemRepository;
import com.agentboard.board.service.WorkItemService;
import com.agentboard.commons.domain.WorkItemType;
import com.agentboard.commons.policy.DataSourcePolicy;
import com.agentboard.commons.tenant.TenantTestFlagReader;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class WorkItemServiceTest {

  @Mock
  private WorkItemRepository workItemRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private DataSourcePolicy dataSourcePolicy;

  @Mock
  private TenantTestFlagReader tenantTestFlagReader;

  private WorkItemService workItemService;

  @BeforeEach
  void setUp() {
    workItemService = new WorkItemService(
        workItemRepository, projectRepository, eventPublisher,
        dataSourcePolicy, tenantTestFlagReader);
    lenient().when(projectRepository.findByIdAndTenantId(any(), any()))
        .thenAnswer(inv -> Optional.of(new Project(
            inv.getArgument(1), "Test Project", null, "agb_unit_test")));
  }

  @Test
  void shouldFormatDisplayKeyWithTypePrefixAndSequence_whenFormatCalled() {
    assertThat(WorkItemDisplayKeys.format(WorkItemType.FEATURE, 1)).isEqualTo("F1");
    assertThat(WorkItemDisplayKeys.format(WorkItemType.USER_STORY, 102)).isEqualTo("U102");
    assertThat(WorkItemDisplayKeys.format(WorkItemType.TASK, 1023)).isEqualTo("T1023");
  }

  @Test
  void shouldReturnCorrectPrefixPerType_whenPrefixCalled() {
    assertThat(WorkItemDisplayKeys.prefix(WorkItemType.FEATURE)).isEqualTo("F");
    assertThat(WorkItemDisplayKeys.prefix(WorkItemType.USER_STORY)).isEqualTo("U");
    assertThat(WorkItemDisplayKeys.prefix(WorkItemType.TASK)).isEqualTo("T");
  }

  @Test
  void shouldCreateFeature_whenParentIdIsNull() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    WorkItem saved = new WorkItem(projectId, tenantId, WorkItemType.FEATURE, "Auth", null, null, 5, "F1", null);

    when(workItemRepository.findMaxDisplayKeySeq(projectId, tenantId, "FEATURE")).thenReturn(0);
    when(workItemRepository.save(any(WorkItem.class))).thenReturn(saved);

    WorkItem result = workItemService.createWorkItem(tenantId, projectId,
        WorkItemType.FEATURE, "Auth", null, null, 5, null);

    assertThat(result.getType()).isEqualTo(WorkItemType.FEATURE);
    assertThat(result.getStatus()).isEqualTo("BACKLOG");
    assertThat(result.getDisplayKey()).isEqualTo("F1");
  }

  @Test
  void shouldThrowBadRequest_whenCreateFeatureWithParentId() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID forbiddenParentId = UUID.randomUUID();

    assertThatThrownBy(() ->
        workItemService.createWorkItem(tenantId, projectId,
            WorkItemType.FEATURE, "Auth", null, forbiddenParentId, 5, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("FEATURE");
  }

  @Test
  void shouldCreateUserStory_whenParentIsFeature() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID featureId = UUID.randomUUID();
    WorkItem featureItem = new WorkItem(projectId, tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5, "F1", null);
    WorkItem savedUs = new WorkItem(projectId, tenantId, WorkItemType.USER_STORY,
        "US 1", null, featureId, 3, "U1", null);

    when(workItemRepository.findByIdAndTenantId(featureId, tenantId))
        .thenReturn(Optional.of(featureItem));
    when(workItemRepository.findMaxDisplayKeySeq(projectId, tenantId, "USER_STORY")).thenReturn(0);
    when(workItemRepository.save(any(WorkItem.class))).thenReturn(savedUs);

    WorkItem result = workItemService.createWorkItem(tenantId, projectId,
        WorkItemType.USER_STORY, "US 1", null, featureId, 3, null);

    assertThat(result.getType()).isEqualTo(WorkItemType.USER_STORY);
    assertThat(result.getStatus()).isEqualTo("READY");
  }

  @Test
  void shouldThrowInvalidParentType_whenCreateUserStoryWithUserStoryParent() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID usParentId = UUID.randomUUID();
    WorkItem usParent = new WorkItem(projectId, tenantId, WorkItemType.USER_STORY,
        "US parent", null, UUID.randomUUID(), 1, "U1", null);

    when(workItemRepository.findByIdAndTenantId(usParentId, tenantId))
        .thenReturn(Optional.of(usParent));

    assertThatThrownBy(() ->
        workItemService.createWorkItem(tenantId, projectId,
            WorkItemType.USER_STORY, "US child", null, usParentId, 1, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("INVALID_PARENT_TYPE");
  }

  @Test
  void shouldCreateTask_whenParentIsUserStory() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID usId = UUID.randomUUID();
    WorkItem usParent = new WorkItem(projectId, tenantId, WorkItemType.USER_STORY,
        "US", null, UUID.randomUUID(), 1, "U1", null);
    WorkItem savedTask = new WorkItem(projectId, tenantId, WorkItemType.TASK,
        "Task 1", null, usId, 5, "T1", null);

    when(workItemRepository.findByIdAndTenantId(usId, tenantId))
        .thenReturn(Optional.of(usParent));
    when(workItemRepository.findMaxDisplayKeySeq(projectId, tenantId, "TASK")).thenReturn(0);
    when(workItemRepository.save(any(WorkItem.class))).thenReturn(savedTask);

    WorkItem result = workItemService.createWorkItem(tenantId, projectId,
        WorkItemType.TASK, "Task 1", null, usId, 5, null);

    assertThat(result.getType()).isEqualTo(WorkItemType.TASK);
    assertThat(result.getStatus()).isEqualTo("NEW");
  }

  @Test
  void shouldThrowInvalidParentType_whenCreateTaskWithFeatureParent() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID featureParentId = UUID.randomUUID();
    WorkItem featureParent = new WorkItem(projectId, tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5, "F1", null);

    when(workItemRepository.findByIdAndTenantId(featureParentId, tenantId))
        .thenReturn(Optional.of(featureParent));

    assertThatThrownBy(() ->
        workItemService.createWorkItem(tenantId, projectId,
            WorkItemType.TASK, "Task", null, featureParentId, 5, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("INVALID_PARENT_TYPE");
  }

  @Test
  void shouldMoveFeatureStatus_whenTargetIsManualStage() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem feature = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5, "F1", null);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));
    when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

    WorkItem result = workItemService.moveStatus(tenantId, workItemId, "SPECIFY");

    assertThat(result.getStatus()).isEqualTo("SPECIFY");
  }

  @Test
  void shouldThrowAutoOnly_whenMoveFeatureToInDevelopment() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem feature = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5, "F1", null);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));

    assertThatThrownBy(() -> workItemService.moveStatus(tenantId, workItemId, "IN_DEVELOPMENT"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("auto");
  }

  @Test
  void shouldThrowAutoOnly_whenMoveFeatureToPrReview() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem feature = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5, "F1", null);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));

    assertThatThrownBy(() -> workItemService.moveStatus(tenantId, workItemId, "PR_REVIEW"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("auto");
  }

  @Test
  void shouldThrowBadRequest_whenMoveFeatureWithInvalidStatus() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem feature = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5, "F1", null);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));

    assertThatThrownBy(() -> workItemService.moveStatus(tenantId, workItemId, "CLOSED"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void shouldThrowAutoOnly_whenMoveUserStoryToDone() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem us = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.USER_STORY,
        "US", null, UUID.randomUUID(), 1, "U1", null);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(us));

    assertThatThrownBy(() -> workItemService.moveStatus(tenantId, workItemId, "DONE"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("auto");
  }

  @Test
  void shouldMoveTaskToAnyStatus_whenStatusIsValid() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem task = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.TASK,
        "Task", null, UUID.randomUUID(), 5, "T1", null);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(task));
    when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

    WorkItem result = workItemService.moveStatus(tenantId, workItemId, "ACTIVE");

    assertThat(result.getStatus()).isEqualTo("ACTIVE");
  }
}
