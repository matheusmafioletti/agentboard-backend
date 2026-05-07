package com.agentboard.board.repository;

import com.agentboard.board.domain.Artifact;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Artifact} entities.
 *
 * <p>Artifacts are append-only — this repository only supports reads and inserts.
 */
public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {

  /**
   * Returns all artifacts for the given work item, ordered by creation time ascending.
   *
   * @param workItemId the owning work item
   * @return ordered artifact list (oldest first)
   */
  List<Artifact> findByWorkItemIdOrderByCreatedAtAsc(UUID workItemId);

  /**
   * Returns the most recently created artifact for the given (work item, command) pair.
   *
   * @param workItemId the owning work item
   * @param command    the SpecKit command
   * @return the latest artifact for that command, or empty if none exists
   */
  @Query("SELECT a FROM Artifact a WHERE a.workItemId = :workItemId AND a.command = :command "
      + "ORDER BY a.createdAt DESC LIMIT 1")
  Optional<Artifact> findLatestByWorkItemIdAndCommand(
      @Param("workItemId") UUID workItemId,
      @Param("command") String command);
}
