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
 * Immutable audit record for every SpecKit command invocation.
 *
 * <p>IMPORTANT: Every CommandExecution must reference exactly one WorkItem via
 * {@link #workItemId}. Status transitions: RUNNING → COMPLETED or RUNNING → FAILED only.
 * A terminal status may not change again.
 */
@Entity
@Table(name = "command_execution")
public class CommandExecution {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "work_item_id", nullable = false)
  private UUID workItemId;

  @Column(nullable = false, length = 100)
  private String command;

  @Column(name = "agent_id", length = 255)
  private String agentId;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  /** Required by JPA. */
  protected CommandExecution() {}

  /**
   * Creates a RUNNING command execution scoped to a WorkItem.
   *
   * @param projectId  the owning project
   * @param workItemId the work item being operated on
   * @param command    the SpecKit command name
   * @param agentId    the identifier of the agent invoking the command
   */
  public static CommandExecution forWorkItem(UUID projectId, UUID workItemId,
      String command, String agentId) {
    CommandExecution ce = new CommandExecution();
    ce.projectId = projectId;
    ce.workItemId = workItemId;
    ce.command = command;
    ce.agentId = agentId;
    ce.status = "RUNNING";
    return ce;
  }

  @PrePersist
  void onCreate() {
    this.startedAt = Instant.now();
  }

  /** Returns the execution's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the owning project's identifier. */
  public UUID getProjectId() {
    return projectId;
  }

  /** Returns the work item this execution is scoped to. */
  public UUID getWorkItemId() {
    return workItemId;
  }

  /** Returns the SpecKit command name. */
  public String getCommand() {
    return command;
  }

  /** Returns the agent identifier. */
  public String getAgentId() {
    return agentId;
  }

  /** Returns the current status (RUNNING, COMPLETED, or FAILED). */
  public String getStatus() {
    return status;
  }

  /** Returns the instant this execution started. */
  public Instant getStartedAt() {
    return startedAt;
  }

  /** Returns the instant this execution finished, or null if still running. */
  public Instant getFinishedAt() {
    return finishedAt;
  }

  /** Marks this execution as COMPLETED. */
  public void complete() {
    this.status = "COMPLETED";
    this.finishedAt = Instant.now();
  }

  /** Marks this execution as FAILED. */
  public void fail() {
    this.status = "FAILED";
    this.finishedAt = Instant.now();
  }
}
