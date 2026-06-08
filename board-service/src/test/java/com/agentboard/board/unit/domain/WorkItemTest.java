package com.agentboard.board.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.board.domain.WorkItem;
import com.agentboard.commons.domain.WorkItemType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkItemTest {

  @Test
  void shouldSetInitialStatusToBacklog_whenFeatureCreated() {
    WorkItem feature = new WorkItem(UUID.randomUUID(), UUID.randomUUID(),
        WorkItemType.FEATURE, "Feature", null, null, 5, "F1", null);

    assertThat(feature.getStatus()).isEqualTo("BACKLOG");
  }

  @Test
  void shouldSetInitialStatusToReady_whenUserStoryCreated() {
    WorkItem us = new WorkItem(UUID.randomUUID(), UUID.randomUUID(),
        WorkItemType.USER_STORY, "US", null, UUID.randomUUID(), 3, "U1", null);

    assertThat(us.getStatus()).isEqualTo("READY");
  }

  @Test
  void shouldSetInitialStatusToNew_whenTaskCreated() {
    WorkItem task = new WorkItem(UUID.randomUUID(), UUID.randomUUID(),
        WorkItemType.TASK, "Task", null, UUID.randomUUID(), 5, "T1", null);

    assertThat(task.getStatus()).isEqualTo("NEW");
  }

  @Test
  void shouldTransitionStatus_whenTransitionToCalled() {
    WorkItem feature = new WorkItem(UUID.randomUUID(), UUID.randomUUID(),
        WorkItemType.FEATURE, "Feature", null, null, 5, "F1", null);

    feature.transitionTo("SPECIFY");

    assertThat(feature.getStatus()).isEqualTo("SPECIFY");
  }

  @Test
  void shouldUpdateTitle_whenPatchCalledWithNewTitle() {
    WorkItem feature = new WorkItem(UUID.randomUUID(), UUID.randomUUID(),
        WorkItemType.FEATURE, "Old Title", "Old desc", null, 5, "F1", null);

    feature.patch("New Title", null, null);

    assertThat(feature.getTitle()).isEqualTo("New Title");
    assertThat(feature.getDescription()).isEqualTo("Old desc");
  }

  @Test
  void shouldUpdateDescription_whenPatchCalledWithNewDescription() {
    WorkItem feature = new WorkItem(UUID.randomUUID(), UUID.randomUUID(),
        WorkItemType.FEATURE, "Title", "Old desc", null, 5, "F1", null);

    feature.patch(null, "New desc", null);

    assertThat(feature.getTitle()).isEqualTo("Title");
    assertThat(feature.getDescription()).isEqualTo("New desc");
  }

  @Test
  void shouldSetAssignee_whenPatchCalledWithAssigneeId() {
    UUID assigneeId = UUID.randomUUID();
    WorkItem task = new WorkItem(UUID.randomUUID(), UUID.randomUUID(),
        WorkItemType.TASK, "Task", null, UUID.randomUUID(), 5, "T1", null);

    task.patch(null, null, Optional.of(assigneeId));

    assertThat(task.getAssigneeId()).isEqualTo(assigneeId);
  }

  @Test
  void shouldClearAssignee_whenPatchCalledWithEmptyOptional() {
    UUID assigneeId = UUID.randomUUID();
    WorkItem task = new WorkItem(UUID.randomUUID(), UUID.randomUUID(),
        WorkItemType.TASK, "Task", null, UUID.randomUUID(), 5, "T1", assigneeId);

    task.patch(null, null, Optional.empty());

    assertThat(task.getAssigneeId()).isNull();
  }

  @Test
  void shouldNotChangeAssignee_whenPatchCalledWithNullOptional() {
    UUID assigneeId = UUID.randomUUID();
    WorkItem task = new WorkItem(UUID.randomUUID(), UUID.randomUUID(),
        WorkItemType.TASK, "Task", null, UUID.randomUUID(), 5, "T1", assigneeId);

    task.patch(null, null, null);

    assertThat(task.getAssigneeId()).isEqualTo(assigneeId);
  }

  @Test
  void shouldReturnStoredFields_whenGettersInvoked() {
    UUID projectId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    WorkItem task = new WorkItem(projectId, tenantId, WorkItemType.TASK,
        "Task Title", "Description", parentId, 3, "T5", null);

    assertThat(task.getProjectId()).isEqualTo(projectId);
    assertThat(task.getTenantId()).isEqualTo(tenantId);
    assertThat(task.getType()).isEqualTo(WorkItemType.TASK);
    assertThat(task.getTitle()).isEqualTo("Task Title");
    assertThat(task.getDescription()).isEqualTo("Description");
    assertThat(task.getParentId()).isEqualTo(parentId);
    assertThat(task.getPriority()).isEqualTo(3);
    assertThat(task.getDisplayKey()).isEqualTo("T5");
    assertThat(task.getDisplayOrder()).isZero();
  }
}
