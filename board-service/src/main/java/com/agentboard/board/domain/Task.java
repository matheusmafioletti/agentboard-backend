package com.agentboard.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A checklist item belonging to a Feature Card. */
@Entity
@Table(name = "task")
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "feature_card_id", nullable = false)
  private UUID featureCardId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false, length = 5)
  private String priority;

  @Column(nullable = false)
  private boolean completed = false;

  @Column(nullable = false)
  private boolean blocked = false;

  @Column(name = "blocked_reason", columnDefinition = "TEXT")
  private String blockedReason;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  /** Required by JPA. */
  protected Task() {}

  /** Creates a new task for the given feature card. */
  public Task(UUID tenantId, UUID featureCardId, String title, String description,
      String priority) {
    this.tenantId = tenantId;
    this.featureCardId = featureCardId;
    this.title = title;
    this.description = description;
    this.priority = priority;
  }

  @PrePersist
  void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  /** Returns the task's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the owning feature card's identifier. */
  public UUID getFeatureCardId() {
    return featureCardId;
  }

  /** Returns the owning tenant's identifier. */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns the task title. */
  public String getTitle() {
    return title;
  }

  /** Returns the optional task description. */
  public String getDescription() {
    return description;
  }

  /** Returns the task priority (P1, P2, or P3). */
  public String getPriority() {
    return priority;
  }

  /** Returns whether this task has been completed. */
  public boolean isCompleted() {
    return completed;
  }

  /** Returns whether this task is currently blocked. */
  public boolean isBlocked() {
    return blocked;
  }

  /** Returns the reason this task is blocked, or null. */
  public String getBlockedReason() {
    return blockedReason;
  }

  /** Returns when this task was completed, or null. */
  public OffsetDateTime getCompletedAt() {
    return completedAt;
  }

  /** Returns when this task was created. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /** Marks this task as completed. */
  public void complete() {
    this.completed = true;
    this.completedAt = OffsetDateTime.now();
  }

  /** Marks this task as blocked with the given reason. */
  public void fail(String reason) {
    this.blocked = true;
    this.blockedReason = reason;
  }
}
