package com.agentboard.board.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentboard.board.domain.Artifact;
import com.agentboard.board.repository.ArtifactRepository;
import com.agentboard.board.service.ArtifactService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtifactServiceTest {

  @Mock
  private ArtifactRepository artifactRepository;

  private ArtifactService artifactService;

  @BeforeEach
  void setUp() {
    artifactService = new ArtifactService(artifactRepository);
  }

  @Test
  void shouldSaveArtifact_whenSaveArtifactCalled() {
    UUID workItemId = UUID.randomUUID();
    Artifact artifact = new Artifact(workItemId, "specify", "# Spec content");
    when(artifactRepository.save(any(Artifact.class))).thenReturn(artifact);

    Artifact result = artifactService.saveArtifact(workItemId, "specify", "# Spec content");

    assertThat(result.getWorkItemId()).isEqualTo(workItemId);
    assertThat(result.getCommand()).isEqualTo("specify");
    assertThat(result.getContent()).isEqualTo("# Spec content");
    verify(artifactRepository).save(any(Artifact.class));
  }

  @Test
  void shouldReturnArtifactList_whenListByWorkItemCalled() {
    UUID workItemId = UUID.randomUUID();
    Artifact artifact1 = new Artifact(workItemId, "specify", "content1");
    Artifact artifact2 = new Artifact(workItemId, "plan", "content2");
    when(artifactRepository.findByWorkItemIdOrderByCreatedAtAsc(workItemId))
        .thenReturn(List.of(artifact1, artifact2));

    List<Artifact> result = artifactService.listByWorkItem(workItemId);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCommand()).isEqualTo("specify");
    assertThat(result.get(1).getCommand()).isEqualTo("plan");
  }

  @Test
  void shouldReturnOptionalArtifact_whenGetLatestByCommandFound() {
    UUID workItemId = UUID.randomUUID();
    Artifact artifact = new Artifact(workItemId, "plan", "# Plan content");
    when(artifactRepository.findLatestByWorkItemIdAndCommand(workItemId, "plan"))
        .thenReturn(Optional.of(artifact));

    Optional<Artifact> result = artifactService.getLatestByCommand(workItemId, "plan");

    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEqualTo("# Plan content");
  }

  @Test
  void shouldReturnEmptyOptional_whenGetLatestByCommandNotFound() {
    UUID workItemId = UUID.randomUUID();
    when(artifactRepository.findLatestByWorkItemIdAndCommand(workItemId, "tasks"))
        .thenReturn(Optional.empty());

    Optional<Artifact> result = artifactService.getLatestByCommand(workItemId, "tasks");

    assertThat(result).isEmpty();
  }
}
