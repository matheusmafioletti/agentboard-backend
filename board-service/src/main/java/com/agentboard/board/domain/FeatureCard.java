package com.agentboard.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A spec-driven Feature Card placed in exactly one board column. */
@Entity
@Table(name = "feature_card")
public class FeatureCard {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "column_id", nullable = false)
  private UUID columnId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "re_execution_pending", nullable = false)
  private boolean reExecutionPending = false;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  /** Required by JPA. */
  protected FeatureCard() {}

  /** Creates a new card in the specified column for the given tenant. */
  public FeatureCard(UUID tenantId, UUID columnId, String title, String description,
      int displayOrder) {
    this.tenantId = tenantId;
    this.columnId = columnId;
    this.title = title;
    this.description = description;
    this.displayOrder = displayOrder;
  }

  @PrePersist
  void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }

  /** Returns the card's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the column this card currently belongs to. */
  public UUID getColumnId() {
    return columnId;
  }

  /** Returns the tenant that owns this card. */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns the card title. */
  public String getTitle() {
    return title;
  }

  /** Returns the optional description. */
  public String getDescription() {
    return description;
  }

  /** Returns whether the agent should re-execute the SpecKit stage for this card. */
  public boolean isReExecutionPending() {
    return reExecutionPending;
  }

  /** Returns the zero-based ordering position within the column. */
  public int getDisplayOrder() {
    return displayOrder;
  }

  /** Returns the instant this card was created. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /** Returns the instant this card was last modified. */
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  /** Moves this card to a new column and updates its display position. */
  public void moveTo(UUID newColumnId, int newDisplayOrder) {
    this.columnId = newColumnId;
    this.displayOrder = newDisplayOrder;
  }

  /** Updates the card's editable fields; null values leave the field unchanged. */
  public void patch(String newTitle, String newDescription) {
    if (newTitle != null) {
      this.title = newTitle;
    }
    if (newDescription != null) {
      this.description = newDescription;
    }
  }

  /** Sets the re-execution pending flag. */
  public void setReExecutionPending(boolean pending) {
    this.reExecutionPending = pending;
  }
}
