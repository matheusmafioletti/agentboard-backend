package com.agentboard.board.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.agentboard.board.domain.WorkItem;
import com.agentboard.board.repository.WorkItemRepository;
import com.agentboard.board.service.WorkItemService;
import com.agentboard.commons.domain.WorkItemType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link WorkItemService}.
 *
 * <p>Covers: hierarchy validation, invalid parent type rejection, valid/invalid status values
 * per type, auto-only stage rejection for FEATURE.
 */
@ExtendWith(MockitoExtension.class)
class WorkItemServiceTest {

  @Mock
  private WorkItemRepository workItemRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private WorkItemService workItemService;

  @BeforeEach
  void setUp() {
    workItemService = new WorkItemService(workItemRepository, eventPublisher);
  }

  @Test
  void createFeature_withNullParent_succeeds() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    WorkItem saved = new WorkItem(projectId, tenantId, WorkItemType.FEATURE, "Auth", null, null, 5);

    when(workItemRepository.save(any(WorkItem.class))).thenReturn(saved);

    WorkItem result = workItemService.createWorkItem(tenantId, projectId,
        WorkItemType.FEATURE, "Auth", null, null, 5);

    assertThat(result.getType()).isEqualTo(WorkItemType.FEATURE);
    assertThat(result.getStatus()).isEqualTo("BACKLOG");
  }

  @Test
  void createFeature_withParentId_throwsBadRequest() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID forbiddenParentId = UUID.randomUUID();

    assertThatThrownBy(() ->
        workItemService.createWorkItem(tenantId, projectId,
            WorkItemType.FEATURE, "Auth", null, forbiddenParentId, 5))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("FEATURE");
  }

  @Test
  void createUserStory_withFeatureParent_succeeds() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID featureId = UUID.randomUUID();
    WorkItem featureItem = new WorkItem(projectId, tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5);
    WorkItem savedUs = new WorkItem(projectId, tenantId, WorkItemType.USER_STORY,
        "US 1", null, featureId, 3);

    when(workItemRepository.findByIdAndTenantId(featureId, tenantId))
        .thenReturn(Optional.of(featureItem));
    when(workItemRepository.save(any(WorkItem.class))).thenReturn(savedUs);

    WorkItem result = workItemService.createWorkItem(tenantId, projectId,
        WorkItemType.USER_STORY, "US 1", null, featureId, 3);

    assertThat(result.getType()).isEqualTo(WorkItemType.USER_STORY);
    assertThat(result.getStatus()).isEqualTo("READY");
  }

  @Test
  void createUserStory_withUserStoryParent_throwsInvalidParentType() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID usParentId = UUID.randomUUID();
    WorkItem usParent = new WorkItem(projectId, tenantId, WorkItemType.USER_STORY,
        "US parent", null, UUID.randomUUID(), 1);

    when(workItemRepository.findByIdAndTenantId(usParentId, tenantId))
        .thenReturn(Optional.of(usParent));

    assertThatThrownBy(() ->
        workItemService.createWorkItem(tenantId, projectId,
            WorkItemType.USER_STORY, "US child", null, usParentId, 1))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("INVALID_PARENT_TYPE");
  }

  @Test
  void createTask_withUserStoryParent_succeeds() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID usId = UUID.randomUUID();
    WorkItem usParent = new WorkItem(projectId, tenantId, WorkItemType.USER_STORY,
        "US", null, UUID.randomUUID(), 1);
    WorkItem savedTask = new WorkItem(projectId, tenantId, WorkItemType.TASK,
        "Task 1", null, usId, 5);

    when(workItemRepository.findByIdAndTenantId(usId, tenantId))
        .thenReturn(Optional.of(usParent));
    when(workItemRepository.save(any(WorkItem.class))).thenReturn(savedTask);

    WorkItem result = workItemService.createWorkItem(tenantId, projectId,
        WorkItemType.TASK, "Task 1", null, usId, 5);

    assertThat(result.getType()).isEqualTo(WorkItemType.TASK);
    assertThat(result.getStatus()).isEqualTo("NEW");
  }

  @Test
  void createTask_withFeatureParent_throwsInvalidParentType() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID featureParentId = UUID.randomUUID();
    WorkItem featureParent = new WorkItem(projectId, tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5);

    when(workItemRepository.findByIdAndTenantId(featureParentId, tenantId))
        .thenReturn(Optional.of(featureParent));

    assertThatThrownBy(() ->
        workItemService.createWorkItem(tenantId, projectId,
            WorkItemType.TASK, "Task", null, featureParentId, 5))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("INVALID_PARENT_TYPE");
  }

  @Test
  void moveStatus_featureToManualStage_succeeds() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem feature = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));
    when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

    WorkItem result = workItemService.moveStatus(tenantId, workItemId, "SPECIFY");

    assertThat(result.getStatus()).isEqualTo("SPECIFY");
  }

  @Test
  void moveStatus_featureToInDevelopment_throwsAutoOnly() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem feature = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));

    assertThatThrownBy(() -> workItemService.moveStatus(tenantId, workItemId, "IN_DEVELOPMENT"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("auto");
  }

  @Test
  void moveStatus_featureToPrReview_throwsAutoOnly() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem feature = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));

    assertThatThrownBy(() -> workItemService.moveStatus(tenantId, workItemId, "PR_REVIEW"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("auto");
  }

  @Test
  void moveStatus_featureWithInvalidStatus_throwsBadRequest() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem feature = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));

    assertThatThrownBy(() -> workItemService.moveStatus(tenantId, workItemId, "CLOSED"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void moveStatus_userStoryToDone_throwsAutoOnly() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem us = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.USER_STORY,
        "US", null, UUID.randomUUID(), 1);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(us));

    assertThatThrownBy(() -> workItemService.moveStatus(tenantId, workItemId, "DONE"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("auto");
  }

  @Test
  void moveStatus_taskToAnyStatus_succeeds() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem task = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.TASK,
        "Task", null, UUID.randomUUID(), 5);

    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(task));
    when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

    WorkItem result = workItemService.moveStatus(tenantId, workItemId, "ACTIVE");

    assertThat(result.getStatus()).isEqualTo("ACTIVE");
  }
}
