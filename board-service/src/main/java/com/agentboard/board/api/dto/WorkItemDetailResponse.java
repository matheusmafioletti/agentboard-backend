package com.agentboard.board.api.dto;

import com.agentboard.board.domain.Artifact;
import com.agentboard.board.domain.CommandExecution;
import com.agentboard.board.domain.WorkItem;
import java.util.List;
import java.util.UUID;

/** Full detail DTO for a WorkItem, including children, artifacts, and command executions. */
public record WorkItemDetailResponse(
    UUID id,
    UUID projectId,
    UUID tenantId,
    String type,
    String title,
    String description,
    String status,
    UUID parentId,
    int priority,
    int displayOrder,
    String createdAt,
    String updatedAt,
    List<WorkItemResponse> children,
    List<ArtifactDto> artifacts,
    List<CommandExecutionDto> commandExecutions
) {

  /** Creates a full detail response from the entity plus its nested collections. */
  public static WorkItemDetailResponse from(WorkItem wi, List<WorkItem> children,
      List<Artifact> artifacts, List<CommandExecution> executions) {
    return new WorkItemDetailResponse(
        wi.getId(),
        wi.getProjectId(),
        wi.getTenantId(),
        wi.getType().name(),
        wi.getTitle(),
        wi.getDescription(),
        wi.getStatus(),
        wi.getParentId(),
        wi.getPriority(),
        wi.getDisplayOrder(),
        wi.getCreatedAt().toString(),
        wi.getUpdatedAt().toString(),
        children.stream().map(WorkItemResponse::from).toList(),
        artifacts.stream().map(ArtifactDto::from).toList(),
        executions.stream().map(CommandExecutionDto::from).toList()
    );
  }

  /** Embedded DTO for an artifact entry. */
  public record ArtifactDto(UUID id, String command, String content, String createdAt) {
    static ArtifactDto from(Artifact a) {
      return new ArtifactDto(a.getId(), a.getCommand(), a.getContent(),
          a.getCreatedAt().toString());
    }
  }

  /** Embedded DTO for a command execution entry. */
  public record CommandExecutionDto(UUID id, String command, String agentId, String status,
      String startedAt, String finishedAt) {
    static CommandExecutionDto from(CommandExecution ce) {
      return new CommandExecutionDto(
          ce.getId(), ce.getCommand(), ce.getAgentId(), ce.getStatus(),
          ce.getStartedAt().toString(),
          ce.getFinishedAt() != null ? ce.getFinishedAt().toString() : null);
    }
  }
}
