package com.agentboard.board.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentboard.board.event.WorkItemMovedEvent;

import com.agentboard.board.domain.Project;
import com.agentboard.board.domain.WorkItem;
import com.agentboard.board.repository.ProjectRepository;
import com.agentboard.board.repository.WorkItemRepository;
import com.agentboard.board.service.WorkItemService;
import com.agentboard.commons.domain.WorkItemType;
import com.agentboard.commons.exceptions.ResourceNotFoundException;
import com.agentboard.commons.policy.DataSourcePolicy;
import com.agentboard.commons.tenant.TenantTestFlagReader;
import java.util.List;
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
class WorkItemServiceAdditionalTest {

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
  void shouldReturnFilteredList_whenListWorkItemsCalled() {
    UUID tenantId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    WorkItem feature = new WorkItem(projectId, tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5, "F1", null);
    when(workItemRepository.findFiltered(projectId, tenantId, WorkItemType.FEATURE,
        null, null, null)).thenReturn(List.of(feature));

    List<WorkItem> result = workItemService.listWorkItems(
        tenantId, projectId, WorkItemType.FEATURE, null, null, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getType()).isEqualTo(WorkItemType.FEATURE);
    verify(workItemRepository).findFiltered(projectId, tenantId, WorkItemType.FEATURE,
        null, null, null);
  }

  @Test
  void shouldReturnWorkItem_whenGetWorkItemDetailFound() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    WorkItem feature = new WorkItem(projectId, tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5, "F1", null);
    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));

    WorkItem result = workItemService.getWorkItemDetail(tenantId, workItemId);

    assertThat(result).isSameAs(feature);
  }

  @Test
  void shouldThrowResourceNotFoundException_whenGetWorkItemDetailNotFound() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> workItemService.getWorkItemDetail(tenantId, workItemId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining(workItemId.toString());
  }

  @Test
  void shouldPatchTitle_whenPatchWorkItemWithNewTitle() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    WorkItem feature = new WorkItem(projectId, tenantId, WorkItemType.FEATURE,
        "Old Title", null, null, 5, "F1", null);
    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));
    when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

    WorkItem result = workItemService.patchWorkItem(
        tenantId, workItemId, "New Title", null, null);

    assertThat(result.getTitle()).isEqualTo("New Title");
    verify(workItemRepository).save(feature);
  }

  @Test
  void shouldPatchAssignee_whenPatchWorkItemWithNewAssignee() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID assigneeId = UUID.randomUUID();
    WorkItem task = new WorkItem(projectId, tenantId, WorkItemType.TASK,
        "Task", null, UUID.randomUUID(), 5, "T1", null);
    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(task));
    when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

    WorkItem result = workItemService.patchWorkItem(
        tenantId, workItemId, null, null, Optional.of(assigneeId));

    assertThat(result.getAssigneeId()).isEqualTo(assigneeId);
  }

  @Test
  void shouldUnassign_whenPatchWorkItemWithEmptyOptional() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID originalAssignee = UUID.randomUUID();
    WorkItem task = new WorkItem(projectId, tenantId, WorkItemType.TASK,
        "Task", null, UUID.randomUUID(), 5, "T1", originalAssignee);
    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(task));
    when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

    WorkItem result = workItemService.patchWorkItem(
        tenantId, workItemId, null, null, Optional.empty());

    assertThat(result.getAssigneeId()).isNull();
  }

  @Test
  void shouldAutoTransition_whenAutoTransitionCalled() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    WorkItem feature = new WorkItem(projectId, tenantId, WorkItemType.FEATURE,
        "Feature", null, null, 5, "F1", null);
    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(feature));
    when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));

    WorkItem result = workItemService.autoTransition(tenantId, workItemId, "IN_DEVELOPMENT");

    assertThat(result.getStatus()).isEqualTo("IN_DEVELOPMENT");
    verify(eventPublisher).publishEvent(any(WorkItemMovedEvent.class));
  }

  @Test
  void shouldThrowBadRequest_whenCreateUserStoryWithNullParent() {
    UUID tenantId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();

    assertThatThrownBy(() -> workItemService.createWorkItem(
        tenantId, projectId, WorkItemType.USER_STORY, "US", null, null, 1, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("PARENT_REQUIRED");
  }

  @Test
  void shouldThrowBadRequest_whenCreateTaskWithNullParent() {
    UUID tenantId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();

    assertThatThrownBy(() -> workItemService.createWorkItem(
        tenantId, projectId, WorkItemType.TASK, "Task", null, null, 1, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("PARENT_REQUIRED");
  }

  @Test
  void shouldThrowResourceNotFoundException_whenParentNotFound() {
    UUID tenantId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID nonExistentParentId = UUID.randomUUID();
    when(workItemRepository.findByIdAndTenantId(nonExistentParentId, tenantId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> workItemService.createWorkItem(
        tenantId, projectId, WorkItemType.USER_STORY, "US",
        null, nonExistentParentId, 1, null))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining(nonExistentParentId.toString());
  }

  @Test
  void shouldThrowBadRequest_whenMoveStatusWithInvalidUserStoryStatus() {
    UUID tenantId = UUID.randomUUID();
    UUID workItemId = UUID.randomUUID();
    WorkItem us = new WorkItem(UUID.randomUUID(), tenantId, WorkItemType.USER_STORY,
        "US", null, UUID.randomUUID(), 1, "U1", null);
    when(workItemRepository.findByIdAndTenantId(workItemId, tenantId))
        .thenReturn(Optional.of(us));

    assertThatThrownBy(() -> workItemService.moveStatus(tenantId, workItemId, "INVALID_STATUS"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("INVALID_STATUS");
  }
}
