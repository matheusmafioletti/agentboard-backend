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

/**
 * An immutable SpecKit artifact produced by an agent command (e.g., spec.md content).
 *
 * <p>IMPORTANT: Artifacts are append-only. No UPDATE operations are permitted on this entity
 * after creation.
 */
@Entity
@Table(name = "artifact")
public class Artifact {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "feature_card_id", nullable = false)
  private UUID featureCardId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 50)
  private String command;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "agent_identifier", length = 255)
  private String agentIdentifier;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  /** Required by JPA. */
  protected Artifact() {}

  /** Creates a new artifact; all fields are immutable after construction. */
  public Artifact(UUID tenantId, UUID featureCardId, String command, String content,
      String agentIdentifier) {
    this.tenantId = tenantId;
    this.featureCardId = featureCardId;
    this.command = command;
    this.content = content;
    this.agentIdentifier = agentIdentifier;
  }

  @PrePersist
  void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  /** Returns this artifact's unique identifier. */
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

  /** Returns the SpecKit command that produced this artifact (e.g., "specify"). */
  public String getCommand() {
    return command;
  }

  /** Returns the full text content of this artifact. */
  public String getContent() {
    return content;
  }

  /** Returns the identifier of the agent that created this artifact, or null. */
  public String getAgentIdentifier() {
    return agentIdentifier;
  }

  /** Returns when this artifact was created. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
