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
 * An audit record for a single SpecKit command execution against a Feature Card.
 *
 * <p>IMPORTANT: {@code startedAt} and initial {@code status} are set at creation and must not be
 * changed. Only {@code finishedAt}, {@code durationMs}, and {@code errorMessage} may be updated
 * on completion.
 */
@Entity
@Table(name = "command_execution")
public class CommandExecution {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "feature_card_id", nullable = false)
  private UUID featureCardId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 50)
  private String command;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "agent_identifier", length = 255)
  private String agentIdentifier;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "started_at", nullable = false, updatable = false)
  private OffsetDateTime startedAt;

  @Column(name = "finished_at")
  private OffsetDateTime finishedAt;

  @Column(name = "duration_ms")
  private Long durationMs;

  /** Required by JPA. */
  protected CommandExecution() {}

  /** Creates a new execution record in SUCCESS status. */
  public CommandExecution(UUID tenantId, UUID featureCardId, String command,
      String agentIdentifier) {
    this.tenantId = tenantId;
    this.featureCardId = featureCardId;
    this.command = command;
    this.status = "SUCCESS";
    this.agentIdentifier = agentIdentifier;
  }

  @PrePersist
  void onCreate() {
    this.startedAt = OffsetDateTime.now();
    this.finishedAt = OffsetDateTime.now();
    this.durationMs = 0L;
  }

  /** Returns this execution's unique identifier. */
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

  /** Returns the SpecKit command name. */
  public String getCommand() {
    return command;
  }

  /** Returns the execution status (RUNNING, SUCCESS, or ERROR). */
  public String getStatus() {
    return status;
  }

  /** Returns the identifier of the agent that ran the command, or null. */
  public String getAgentIdentifier() {
    return agentIdentifier;
  }

  /** Returns the error message if status is ERROR, or null. */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** Returns when this execution started. */
  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  /** Returns when this execution finished, or null if still running. */
  public OffsetDateTime getFinishedAt() {
    return finishedAt;
  }

  /** Returns the execution duration in milliseconds, or null. */
  public Long getDurationMs() {
    return durationMs;
  }
}
