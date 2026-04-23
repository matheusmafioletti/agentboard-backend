package com.agentboard.board.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentboard.board.domain.FeatureCard;
import com.agentboard.board.domain.Stage;
import com.agentboard.board.domain.Task;
import com.agentboard.board.dto.CompleteTaskResponse;
import com.agentboard.board.dto.CreateTasksResponse;
import com.agentboard.board.dto.FeatureCardResponse;
import com.agentboard.board.dto.TaskRequest;
import com.agentboard.board.dto.TaskResponse;
import com.agentboard.board.repository.FeatureCardRepository;
import com.agentboard.board.repository.TaskRepository;
import com.agentboard.board.service.BoardEventPublisher;
import com.agentboard.board.service.FeatureCardService;
import com.agentboard.board.service.TaskService;
import com.agentboard.commons.exceptions.ResourceNotFoundException;
import com.agentboard.commons.exceptions.TenantMismatchException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link TaskService}. */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private FeatureCardRepository featureCardRepository;

  @Mock
  private FeatureCardService featureCardService;

  @Mock
  private BoardEventPublisher boardEventPublisher;

  private TaskService taskService;

  private UUID tenantId;
  private UUID featureCardId;

  @BeforeEach
  void setUp() {
    taskService = new TaskService(
        taskRepository, featureCardRepository, featureCardService, boardEventPublisher);
    tenantId = UUID.randomUUID();
    featureCardId = UUID.randomUUID();
  }

  @Test
  void createAll_persistsTasksAndMovesCardToInProgress() {
    stubCardExists(tenantId, featureCardId);
    Task savedTask = buildTask(tenantId, featureCardId, "Task 1", "P1");
    when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
    FeatureCardResponse movedCard = stubCardMoved(Stage.IN_PROGRESS);

    List<TaskRequest> requests = List.of(new TaskRequest("Task 1", null, "P1"));
    CreateTasksResponse result = taskService.createAll(tenantId, featureCardId, requests);

    assertThat(result.tasks()).hasSize(1);
    assertThat(result.featureCard()).isEqualTo(movedCard);
    verify(featureCardService).moveByStage(tenantId, featureCardId, Stage.IN_PROGRESS);
  }

  @Test
  void createAll_throwsWhenCardNotFound() {
    when(featureCardRepository.findById(featureCardId)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
        taskService.createAll(tenantId, featureCardId, List.of(new TaskRequest("T", null, "P1"))))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void complete_marksTaskCompletedAndReturnsCard() {
    UUID taskId = UUID.randomUUID();
    Task task = buildTask(tenantId, featureCardId, "My Task", "P1");
    setTaskId(task, taskId);
    when(taskRepository.findByIdAndTenantId(taskId, tenantId)).thenReturn(Optional.of(task));
    when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(taskRepository.countByFeatureCardIdAndTenantIdAndCompletedFalse(featureCardId, tenantId))
        .thenReturn(1L);
    FeatureCardResponse card = stubGetCard();

    CompleteTaskResponse result = taskService.complete(tenantId, featureCardId, taskId);

    assertThat(result.task().completed()).isTrue();
    assertThat(result.autoMovedToReview()).isFalse();
  }

  @Test
  void complete_autoMovesToReviewWhenLastTaskCompleted() {
    UUID taskId = UUID.randomUUID();
    Task task = buildTask(tenantId, featureCardId, "Last Task", "P1");
    setTaskId(task, taskId);
    when(taskRepository.findByIdAndTenantId(taskId, tenantId)).thenReturn(Optional.of(task));
    when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(taskRepository.countByFeatureCardIdAndTenantIdAndCompletedFalse(featureCardId, tenantId))
        .thenReturn(0L);
    FeatureCardResponse reviewCard = stubCardMoved(Stage.REVIEW);

    CompleteTaskResponse result = taskService.complete(tenantId, featureCardId, taskId);

    assertThat(result.autoMovedToReview()).isTrue();
    assertThat(result.featureCard()).isEqualTo(reviewCard);
    verify(featureCardService).moveByStage(tenantId, featureCardId, Stage.REVIEW);
  }

  @Test
  void complete_throwsWhenTaskAlreadyCompleted() {
    UUID taskId = UUID.randomUUID();
    Task task = buildTask(tenantId, featureCardId, "Done Task", "P1");
    setTaskId(task, taskId);
    task.complete();
    when(taskRepository.findByIdAndTenantId(taskId, tenantId)).thenReturn(Optional.of(task));

    assertThatThrownBy(() -> taskService.complete(tenantId, featureCardId, taskId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already completed");
  }

  @Test
  void complete_throwsWhenTaskBelongsToOtherCard() {
    UUID taskId = UUID.randomUUID();
    UUID otherCardId = UUID.randomUUID();
    Task task = buildTask(tenantId, otherCardId, "Mismatched", "P1");
    setTaskId(task, taskId);
    when(taskRepository.findByIdAndTenantId(taskId, tenantId)).thenReturn(Optional.of(task));

    assertThatThrownBy(() -> taskService.complete(tenantId, featureCardId, taskId))
        .isInstanceOf(TenantMismatchException.class);
  }

  @Test
  void fail_setsBlockedAndReasonOnTask() {
    UUID taskId = UUID.randomUUID();
    Task task = buildTask(tenantId, featureCardId, "Blocked Task", "P2");
    setTaskId(task, taskId);
    when(taskRepository.findByIdAndTenantId(taskId, tenantId)).thenReturn(Optional.of(task));
    when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TaskResponse result = taskService.fail(tenantId, featureCardId, taskId, "Missing dep");

    assertThat(result.blocked()).isTrue();
    assertThat(result.blockedReason()).isEqualTo("Missing dep");
  }

  private void stubCardExists(UUID tenant, UUID cardId) {
    FeatureCard card = org.mockito.Mockito.mock(FeatureCard.class);
    when(card.getTenantId()).thenReturn(tenant);
    when(featureCardRepository.findById(cardId)).thenReturn(Optional.of(card));
  }

  private FeatureCardResponse stubCardMoved(Stage stage) {
    FeatureCardResponse mock = org.mockito.Mockito.mock(FeatureCardResponse.class);
    when(featureCardService.moveByStage(tenantId, featureCardId, stage)).thenReturn(mock);
    return mock;
  }

  private FeatureCardResponse stubGetCard() {
    FeatureCardResponse mock = org.mockito.Mockito.mock(FeatureCardResponse.class);
    when(featureCardService.getById(tenantId, featureCardId)).thenReturn(mock);
    return mock;
  }

  private Task buildTask(UUID tenant, UUID cardId, String title, String priority) {
    return new Task(tenant, cardId, title, null, priority);
  }

  private void setTaskId(Task task, UUID id) {
    try {
      var field = Task.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(task, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
