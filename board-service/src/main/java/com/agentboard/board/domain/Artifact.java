package com.agentboard.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * An immutable SpecKit artifact produced by an agent command.
 *
 * <p>IMPORTANT: Artifacts are append-only. No UPDATE or DELETE operations are permitted
 * on this entity after creation. Multiple artifacts for the same (workItemId, command)
 * pair are valid — they represent versioned history.
 */
@Entity
@Table(name = "artifact")
public class Artifact {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "work_item_id", nullable = false)
  private UUID workItemId;

  @Column(nullable = false, length = 100)
  private String command;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Required by JPA. */
  protected Artifact() {}

  /**
   * Creates a new artifact. All fields are immutable after construction.
   *
   * @param workItemId the owning work item's identifier
   * @param command    the SpecKit command that produced this artifact
   * @param content    the full artifact text
   */
  public Artifact(UUID workItemId, String command, String content) {
    this.workItemId = workItemId;
    this.command = command;
    this.content = content;
  }

  @PrePersist
  void onCreate() {
    this.createdAt = Instant.now();
  }

  /** Returns this artifact's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the owning work item's identifier. */
  public UUID getWorkItemId() {
    return workItemId;
  }

  /** Returns the SpecKit command that produced this artifact. */
  public String getCommand() {
    return command;
  }

  /** Returns the full text content of this artifact. */
  public String getContent() {
    return content;
  }

  /** Returns the instant this artifact was created. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
