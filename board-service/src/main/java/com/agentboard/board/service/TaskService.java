package com.agentboard.board.service;

import com.agentboard.board.domain.Stage;
import com.agentboard.board.domain.Task;
import com.agentboard.board.dto.CompleteTaskResponse;
import com.agentboard.board.dto.CreateTasksResponse;
import com.agentboard.board.dto.FeatureCardResponse;
import com.agentboard.board.dto.TaskRequest;
import com.agentboard.board.dto.TaskResponse;
import com.agentboard.board.repository.FeatureCardRepository;
import com.agentboard.board.repository.TaskRepository;
import com.agentboard.commons.exceptions.ResourceNotFoundException;
import com.agentboard.commons.exceptions.TenantMismatchException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for managing tasks within a Feature Card. */
@Service
@Transactional
public class TaskService {

  private final TaskRepository taskRepository;
  private final FeatureCardRepository featureCardRepository;
  private final FeatureCardService featureCardService;
  private final BoardEventPublisher boardEventPublisher;

  /** Creates the service with the required repositories and services. */
  public TaskService(
      TaskRepository taskRepository,
      FeatureCardRepository featureCardRepository,
      FeatureCardService featureCardService,
      BoardEventPublisher boardEventPublisher) {
    this.taskRepository = taskRepository;
    this.featureCardRepository = featureCardRepository;
    this.featureCardService = featureCardService;
    this.boardEventPublisher = boardEventPublisher;
  }

  /**
   * Creates all tasks for a feature card and moves the card to IN_PROGRESS.
   *
   * @throws ResourceNotFoundException if the feature card is not found
   * @throws TenantMismatchException if the card belongs to another tenant
   */
  public CreateTasksResponse createAll(UUID tenantId, UUID featureCardId,
      List<TaskRequest> requests) {
    requireCardOwned(tenantId, featureCardId);

    List<Task> saved = requests.stream()
        .map(r -> taskRepository.save(
            new Task(tenantId, featureCardId, r.title(), r.description(), r.priority())))
        .toList();

    FeatureCardResponse card = featureCardService.moveByStage(tenantId, featureCardId,
        Stage.IN_PROGRESS);

    List<TaskResponse> taskResponses = saved.stream().map(this::toResponse).toList();
    boardEventPublisher.publishTasksCreated(tenantId, featureCardId, taskResponses);
    return new CreateTasksResponse(taskResponses, card);
  }

  /**
   * Marks a task as completed. If no pending tasks remain, auto-moves the card to REVIEW.
   *
   * @throws ResourceNotFoundException if the task is not found
   * @throws TenantMismatchException if the task belongs to another tenant
   * @throws IllegalStateException if the task is already completed
   */
  public CompleteTaskResponse complete(UUID tenantId, UUID featureCardId, UUID taskId) {
    Task task = requireTaskOwned(tenantId, featureCardId, taskId);
    if (task.isCompleted()) {
      throw new IllegalStateException("Task " + taskId + " is already completed");
    }
    task.complete();
    Task saved = taskRepository.save(task);

    long remaining = taskRepository.countByFeatureCardIdAndTenantIdAndCompletedFalse(
        featureCardId, tenantId);
    boolean autoMoved = remaining == 0;
    FeatureCardResponse card = autoMoved
        ? featureCardService.moveByStage(tenantId, featureCardId, Stage.REVIEW)
        : featureCardService.getById(tenantId, featureCardId);

    TaskResponse taskResponse = toResponse(saved);
    boardEventPublisher.publishTaskUpdated(tenantId, featureCardId, taskResponse);
    return new CompleteTaskResponse(taskResponse, card, autoMoved);
  }

  /**
   * Marks a task as blocked with the given reason.
   *
   * @throws ResourceNotFoundException if the task is not found
   * @throws TenantMismatchException if the task belongs to another tenant
   */
  public TaskResponse fail(UUID tenantId, UUID featureCardId, UUID taskId, String reason) {
    Task task = requireTaskOwned(tenantId, featureCardId, taskId);
    task.fail(reason);
    Task saved = taskRepository.save(task);
    TaskResponse response = toResponse(saved);
    boardEventPublisher.publishTaskUpdated(tenantId, featureCardId, response);
    return response;
  }

  private void requireCardOwned(UUID tenantId, UUID featureCardId) {
    featureCardRepository.findById(featureCardId)
        .filter(c -> c.getTenantId().equals(tenantId))
        .orElseThrow(
            () -> new ResourceNotFoundException("FeatureCard not found: " + featureCardId));
  }

  private Task requireTaskOwned(UUID tenantId, UUID featureCardId, UUID taskId) {
    Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
        .orElseThrow(() -> ResourceNotFoundException.forId("Task", taskId));
    if (!task.getFeatureCardId().equals(featureCardId)) {
      throw new TenantMismatchException(
          "Task " + taskId + " does not belong to feature card " + featureCardId);
    }
    return task;
  }

  private TaskResponse toResponse(Task task) {
    return new TaskResponse(
        task.getId(), task.getFeatureCardId(), task.getTitle(), task.getDescription(),
        task.getPriority(), task.isCompleted(), task.isBlocked(), task.getBlockedReason(),
        task.getCompletedAt() != null ? task.getCompletedAt().toString() : null,
        task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
  }
}
