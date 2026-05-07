package com.agentboard.board.domain;

import com.agentboard.commons.domain.WorkItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Unified work item that replaces the separate Feature, UserStory, and Task entities.
 *
 * <p>The {@link #type} field discriminates lifecycle and hierarchy rules:
 * <ul>
 *   <li>FEATURE — no parent; advances through 9 FeatureStage values</li>
 *   <li>USER_STORY — parent must be a FEATURE; advances through 3 UserStoryStage values</li>
 *   <li>TASK — parent must be a USER_STORY; advances through NEW/ACTIVE/CLOSED</li>
 * </ul>
 *
 * <p>IMPORTANT: Status validation and hierarchy constraints are enforced at the service layer,
 * not via DB constraints, to keep migration logic simple and reversible.
 */
@Entity
@Table(name = "work_item")
public class WorkItem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WorkItemType type;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false, length = 50)
  private String status;

  @Column(name = "parent_id")
  private UUID parentId;

  @Column(nullable = false)
  private int priority;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Required by JPA. */
  protected WorkItem() {}

  /**
   * Creates a new WorkItem with the given parameters.
   *
   * @param projectId   the owning project
   * @param tenantId    the owning tenant (denormalized for isolation queries)
   * @param type        the item type (FEATURE, USER_STORY, or TASK)
   * @param title       the item title
   * @param description optional description
   * @param parentId    null for FEATURE; required for USER_STORY and TASK
   * @param priority    positive integer; lower value = higher priority; defaults to 5
   */
  public WorkItem(UUID projectId, UUID tenantId, WorkItemType type,
      String title, String description, UUID parentId, int priority) {
    this.projectId = projectId;
    this.tenantId = tenantId;
    this.type = type;
    this.title = title;
    this.description = description;
    this.parentId = parentId;
    this.priority = priority;
    this.displayOrder = 0;
    this.status = initialStatus(type);
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }

  /** Returns the work item's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the owning tenant identifier (denormalized). */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns the owning project identifier. */
  public UUID getProjectId() {
    return projectId;
  }

  /** Returns the item type. */
  public WorkItemType getType() {
    return type;
  }

  /** Returns the item title. */
  public String getTitle() {
    return title;
  }

  /** Returns the optional description. */
  public String getDescription() {
    return description;
  }

  /** Returns the current status string. */
  public String getStatus() {
    return status;
  }

  /** Returns the parent item identifier, or null for FEATURE-type items. */
  public UUID getParentId() {
    return parentId;
  }

  /** Returns the priority (lower = higher priority). */
  public int getPriority() {
    return priority;
  }

  /** Returns the display order within a board column. */
  public int getDisplayOrder() {
    return displayOrder;
  }

  /** Returns the instant this item was created. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the instant this item was last modified. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Transitions this item to the given status.
   *
   * <p>Callers are responsible for validating that the transition is permitted before
   * invoking this method.
   *
   * @param newStatus the target status string
   */
  public void transitionTo(String newStatus) {
    this.status = newStatus;
  }

  /**
   * Updates mutable text fields; null values leave the field unchanged.
   *
   * @param newTitle       optional new title
   * @param newDescription optional new description
   */
  public void patch(String newTitle, String newDescription) {
    if (newTitle != null) {
      this.title = newTitle;
    }
    if (newDescription != null) {
      this.description = newDescription;
    }
  }

  private static String initialStatus(WorkItemType type) {
    return switch (type) {
      case FEATURE -> "BACKLOG";
      case USER_STORY -> "READY";
      case TASK -> "NEW";
    };
  }
}
