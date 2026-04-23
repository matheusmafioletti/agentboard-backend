package com.agentboard.board.service;

import com.agentboard.board.domain.Artifact;
import com.agentboard.board.domain.CommandExecution;
import com.agentboard.board.dto.ArtifactResponse;
import com.agentboard.board.repository.ArtifactRepository;
import com.agentboard.board.repository.CommandExecutionRepository;
import com.agentboard.board.repository.FeatureCardRepository;
import com.agentboard.commons.exceptions.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for adding SpecKit artifacts to a Feature Card. */
@Service
@Transactional
public class ArtifactService {

  private final ArtifactRepository artifactRepository;
  private final CommandExecutionRepository commandExecutionRepository;
  private final FeatureCardRepository featureCardRepository;
  private final BoardEventPublisher boardEventPublisher;

  /** Creates the service with the required repositories and event publisher. */
  public ArtifactService(
      ArtifactRepository artifactRepository,
      CommandExecutionRepository commandExecutionRepository,
      FeatureCardRepository featureCardRepository,
      BoardEventPublisher boardEventPublisher) {
    this.artifactRepository = artifactRepository;
    this.commandExecutionRepository = commandExecutionRepository;
    this.featureCardRepository = featureCardRepository;
    this.boardEventPublisher = boardEventPublisher;
  }

  /**
   * Appends an artifact to the feature card and records a command execution.
   *
   * @throws ResourceNotFoundException if the feature card is not found for the tenant
   */
  public ArtifactResponse addArtifact(UUID tenantId, UUID featureCardId, String command,
      String content, String agentIdentifier) {
    featureCardRepository.findById(featureCardId)
        .filter(c -> c.getTenantId().equals(tenantId))
        .orElseThrow(
            () -> new ResourceNotFoundException("FeatureCard not found: " + featureCardId));

    Artifact artifact = artifactRepository.save(
        new Artifact(tenantId, featureCardId, command, content, agentIdentifier));

    commandExecutionRepository.save(
        new CommandExecution(tenantId, featureCardId, command, agentIdentifier));

    ArtifactResponse response = toResponse(artifact);
    boardEventPublisher.publishArtifactAdded(tenantId, featureCardId, response);
    return response;
  }

  private ArtifactResponse toResponse(Artifact artifact) {
    return new ArtifactResponse(
        artifact.getId(), artifact.getFeatureCardId(), artifact.getCommand(),
        artifact.getContent(), artifact.getAgentIdentifier(),
        artifact.getCreatedAt() != null ? artifact.getCreatedAt().toString() : null);
  }
}
