package com.agentboard.board.repository;

import com.agentboard.board.domain.CommandExecution;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link CommandExecution} audit records.
 *
 * <p>Records are immutable after reaching a terminal status (COMPLETED or FAILED).
 */
public interface CommandExecutionRepository extends JpaRepository<CommandExecution, UUID> {

  /**
   * Returns all executions for a work item ordered newest-first.
   *
   * @param workItemId the scoped work item
   * @return ordered list
   */
  @Query("SELECT ce FROM CommandExecution ce WHERE ce.workItemId = :workItemId "
      + "ORDER BY ce.startedAt DESC")
  List<CommandExecution> findByWorkItemIdOrderByStartedAtDesc(
      @Param("workItemId") UUID workItemId);

  /**
   * Returns all RUNNING executions for a project.
   *
   * @param projectId the owning project
   * @return list of running executions
   */
  @Query("SELECT ce FROM CommandExecution ce WHERE ce.projectId = :projectId "
      + "AND ce.status = 'RUNNING'")
  List<CommandExecution> findRunningByProjectId(@Param("projectId") UUID projectId);
}
