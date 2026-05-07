package com.agentboard.board.service;

import com.agentboard.board.domain.Artifact;
import com.agentboard.board.repository.ArtifactRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for Artifact persistence.
 *
 * <p>Artifacts are append-only — this service only supports creation and retrieval.
 */
@Service
public class ArtifactService {

  private final ArtifactRepository artifactRepository;

  /** Creates the service backed by the given repository. */
  public ArtifactService(ArtifactRepository artifactRepository) {
    this.artifactRepository = artifactRepository;
  }

  /**
   * Persists a new artifact for the given work item.
   *
   * @param workItemId the owning work item
   * @param command    the SpecKit command (e.g., "specify", "plan")
   * @param content    the artifact text content
   * @return the saved artifact
   */
  @Transactional
  public Artifact saveArtifact(UUID workItemId, String command, String content) {
    Artifact artifact = new Artifact(workItemId, command, content);
    return artifactRepository.save(artifact);
  }

  /**
   * Returns all artifacts for a work item in chronological order.
   *
   * @param workItemId the owning work item
   * @return ordered list of artifacts
   */
  @Transactional(readOnly = true)
  public List<Artifact> listByWorkItem(UUID workItemId) {
    return artifactRepository.findByWorkItemIdOrderByCreatedAtAsc(workItemId);
  }

  /**
   * Returns the most recently created artifact for the given command, if any.
   *
   * @param workItemId the owning work item
   * @param command    the SpecKit command
   * @return the latest artifact for that command
   */
  @Transactional(readOnly = true)
  public Optional<Artifact> getLatestByCommand(UUID workItemId, String command) {
    return artifactRepository.findLatestByWorkItemIdAndCommand(workItemId, command);
  }
}
