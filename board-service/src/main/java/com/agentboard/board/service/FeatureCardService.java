package com.agentboard.board.service;

import com.agentboard.board.domain.ColumnDef;
import com.agentboard.board.domain.FeatureCard;
import com.agentboard.board.domain.Stage;
import com.agentboard.board.dto.ArtifactResponse;
import com.agentboard.board.dto.CommandExecutionResponse;
import com.agentboard.board.dto.FeatureCardResponse;
import com.agentboard.board.dto.TaskResponse;
import com.agentboard.board.repository.ArtifactRepository;
import com.agentboard.board.repository.ColumnDefRepository;
import com.agentboard.board.repository.CommandExecutionRepository;
import com.agentboard.board.repository.FeatureCardRepository;
import com.agentboard.board.repository.TaskRepository;
import com.agentboard.commons.exceptions.ResourceNotFoundException;
import com.agentboard.commons.exceptions.TenantMismatchException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for creating and managing Feature Cards within a tenant's board. */
@Service
@Transactional
public class FeatureCardService {

  private final FeatureCardRepository featureCardRepository;
  private final ColumnDefRepository columnDefRepository;
  private final BoardEventPublisher boardEventPublisher;
  private final TaskRepository taskRepository;
  private final ArtifactRepository artifactRepository;
  private final CommandExecutionRepository commandExecutionRepository;

  /** Creates the service with all required repositories and the event publisher. */
  public FeatureCardService(
      FeatureCardRepository featureCardRepository,
      ColumnDefRepository columnDefRepository,
      BoardEventPublisher boardEventPublisher,
      TaskRepository taskRepository,
      ArtifactRepository artifactRepository,
      CommandExecutionRepository commandExecutionRepository) {
    this.featureCardRepository = featureCardRepository;
    this.columnDefRepository = columnDefRepository;
    this.boardEventPublisher = boardEventPublisher;
    this.taskRepository = taskRepository;
    this.artifactRepository = artifactRepository;
    this.commandExecutionRepository = commandExecutionRepository;
  }

  /**
   * Creates a new Feature Card in the tenant's Backlog column.
   *
   * @throws ResourceNotFoundException if the tenant has no Backlog column
   */
  public FeatureCardResponse create(UUID tenantId, String title, String description) {
    ColumnDef backlog = columnDefRepository.findByTenantIdAndStage(tenantId, Stage.BACKLOG)
        .orElseThrow(() -> new ResourceNotFoundException("Backlog column not found for tenant"));

    int nextOrder = featureCardRepository
        .findMaxDisplayOrderByColumnIdAndTenantId(backlog.getId(), tenantId)
        .map(max -> max + 1)
        .orElse(0);

    FeatureCard card = new FeatureCard(tenantId, backlog.getId(), title, description, nextOrder);
    FeatureCardResponse response = toResponseWithRelations(featureCardRepository.save(card));
    boardEventPublisher.publishCardCreated(tenantId, response);
    return response;
  }

  /**
   * Returns the full detail of a Feature Card including tasks, artifacts, and execution history.
   *
   * @throws ResourceNotFoundException if not found
   */
  @Transactional(readOnly = true)
  public FeatureCardResponse getById(UUID tenantId, UUID id) {
    return toResponseWithRelations(requireOwned(tenantId, id));
  }

  /**
   * Partially updates an existing Feature Card.
   *
   * @throws ResourceNotFoundException if not found
   * @throws TenantMismatchException if the card belongs to another tenant
   */
  public FeatureCardResponse update(UUID tenantId, UUID id, String newTitle, String newDescription,
      Boolean newReExecutionPending) {
    FeatureCard card = requireOwned(tenantId, id);
    card.patch(newTitle, newDescription);
    if (newReExecutionPending != null) {
      card.setReExecutionPending(newReExecutionPending);
    }
    FeatureCardResponse response = toResponseWithRelations(featureCardRepository.save(card));
    boardEventPublisher.publishCardUpdated(tenantId, response);
    return response;
  }

  /**
   * Sets the re-execution pending flag on a Feature Card.
   *
   * @throws ResourceNotFoundException if not found
   * @throws TenantMismatchException if the card belongs to another tenant
   */
  public FeatureCardResponse setReExecutionPending(UUID tenantId, UUID id, boolean pending) {
    return update(tenantId, id, null, null, pending);
  }

  /**
   * Deletes a Feature Card.
   *
   * @throws ResourceNotFoundException if not found
   * @throws TenantMismatchException if the card belongs to another tenant
   */
  public void delete(UUID tenantId, UUID id) {
    FeatureCard card = requireOwned(tenantId, id);
    featureCardRepository.delete(card);
    boardEventPublisher.publishCardDeleted(tenantId, id);
  }

  /**
   * Returns all cards in a column for the given tenant, ordered by display position.
   */
  @Transactional(readOnly = true)
  public List<FeatureCardResponse> listByColumn(UUID tenantId, UUID columnId) {
    return featureCardRepository
        .findByColumnIdAndTenantIdOrderByDisplayOrderAsc(columnId, tenantId)
        .stream()
        .map(this::toResponseShallow)
        .toList();
  }

  /**
   * Returns all cards for the given tenant, optionally filtered by workflow stage.
   */
  @Transactional(readOnly = true)
  public List<FeatureCardResponse> listByTenant(UUID tenantId, Stage stage) {
    if (stage != null) {
      ColumnDef column = columnDefRepository.findByTenantIdAndStage(tenantId, stage)
          .orElseThrow(() -> new ResourceNotFoundException(
              "Column not found for stage: " + stage));
      return featureCardRepository
          .findByColumnIdAndTenantIdOrderByDisplayOrderAsc(column.getId(), tenantId)
          .stream()
          .map(this::toResponseWithRelations)
          .toList();
    }
    return featureCardRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
        .stream()
        .map(this::toResponseWithRelations)
        .toList();
  }

  /**
   * Moves a Feature Card to a different column and updates its display position.
   *
   * @throws ResourceNotFoundException if the card or target column does not exist
   * @throws TenantMismatchException if the card or the target column belongs to another tenant
   */
  public FeatureCardResponse move(UUID tenantId, UUID cardId, UUID targetColumnId,
      int displayOrder) {
    FeatureCard card = requireOwned(tenantId, cardId);
    ColumnDef targetColumn = columnDefRepository.findById(targetColumnId)
        .orElseThrow(() -> ResourceNotFoundException.forId("ColumnDef", targetColumnId));
    if (!targetColumn.getTenantId().equals(tenantId)) {
      throw new TenantMismatchException(
          "Column " + targetColumnId + " does not belong to tenant " + tenantId);
    }
    UUID previousColumnId = card.getColumnId();
    card.moveTo(targetColumnId, displayOrder);
    FeatureCard saved = featureCardRepository.save(card);
    boardEventPublisher.publishCardMoved(
        tenantId, cardId, previousColumnId, targetColumnId, targetColumn.getStage(), displayOrder);
    return toResponseWithRelations(saved);
  }

  /**
   * Moves a Feature Card to the column that corresponds to the given workflow stage.
   *
   * @throws ResourceNotFoundException if no column exists for that stage or the card is not found
   * @throws TenantMismatchException if the card belongs to another tenant
   */
  public FeatureCardResponse moveByStage(UUID tenantId, UUID cardId, Stage targetStage) {
    FeatureCard card = requireOwned(tenantId, cardId);
    ColumnDef targetColumn = columnDefRepository.findByTenantIdAndStage(tenantId, targetStage)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Column not found for stage: " + targetStage));
    UUID previousColumnId = card.getColumnId();
    int nextOrder = featureCardRepository
        .findMaxDisplayOrderByColumnIdAndTenantId(targetColumn.getId(), tenantId)
        .map(max -> max + 1)
        .orElse(0);
    card.moveTo(targetColumn.getId(), nextOrder);
    FeatureCard saved = featureCardRepository.save(card);
    boardEventPublisher.publishCardMoved(
        tenantId, cardId, previousColumnId, targetColumn.getId(), targetStage, nextOrder);
    return toResponseWithRelations(saved);
  }

  private FeatureCard requireOwned(UUID tenantId, UUID id) {
    FeatureCard card = featureCardRepository.findById(id)
        .orElseThrow(() -> ResourceNotFoundException.forId("FeatureCard", id));
    if (!card.getTenantId().equals(tenantId)) {
      throw new TenantMismatchException(
          "FeatureCard " + id + " does not belong to tenant " + tenantId);
    }
    return card;
  }

  private FeatureCardResponse toResponseWithRelations(FeatureCard card) {
    List<TaskResponse> tasks = taskRepository
        .findByFeatureCardIdAndTenantId(card.getId(), card.getTenantId())
        .stream()
        .map(t -> new TaskResponse(
            t.getId(), t.getFeatureCardId(), t.getTitle(), t.getDescription(),
            t.getPriority(), t.isCompleted(), t.isBlocked(), t.getBlockedReason(),
            t.getCompletedAt() != null ? t.getCompletedAt().toString() : null,
            t.getCreatedAt() != null ? t.getCreatedAt().toString() : null))
        .toList();

    List<ArtifactResponse> artifacts = artifactRepository
        .findByFeatureCardIdAndTenantIdOrderByCreatedAtAsc(card.getId(), card.getTenantId())
        .stream()
        .map(a -> new ArtifactResponse(
            a.getId(), a.getFeatureCardId(), a.getCommand(), a.getContent(),
            a.getAgentIdentifier(),
            a.getCreatedAt() != null ? a.getCreatedAt().toString() : null))
        .toList();

    List<CommandExecutionResponse> executions = commandExecutionRepository
        .findByFeatureCardIdAndTenantIdOrderByStartedAtDesc(card.getId(), card.getTenantId())
        .stream()
        .map(e -> new CommandExecutionResponse(
            e.getId(), e.getFeatureCardId(), e.getCommand(), e.getStatus(),
            e.getAgentIdentifier(), e.getErrorMessage(),
            e.getStartedAt() != null ? e.getStartedAt().toString() : null,
            e.getFinishedAt() != null ? e.getFinishedAt().toString() : null,
            e.getDurationMs()))
        .toList();

    return new FeatureCardResponse(
        card.getId(), card.getColumnId(), card.getTenantId(),
        card.getTitle(), card.getDescription(), card.isReExecutionPending(),
        card.getDisplayOrder(),
        card.getCreatedAt() != null ? card.getCreatedAt().toString() : null,
        card.getUpdatedAt() != null ? card.getUpdatedAt().toString() : null,
        tasks, artifacts, executions);
  }

  private FeatureCardResponse toResponseShallow(FeatureCard card) {
    return new FeatureCardResponse(
        card.getId(), card.getColumnId(), card.getTenantId(),
        card.getTitle(), card.getDescription(), card.isReExecutionPending(),
        card.getDisplayOrder(),
        card.getCreatedAt() != null ? card.getCreatedAt().toString() : null,
        card.getUpdatedAt() != null ? card.getUpdatedAt().toString() : null,
        List.of(), List.of(), List.of());
  }
}
