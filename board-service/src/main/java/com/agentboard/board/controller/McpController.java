package com.agentboard.board.controller;

import com.agentboard.board.domain.Stage;
import com.agentboard.board.dto.ArtifactResponse;
import com.agentboard.board.dto.CompleteTaskResponse;
import com.agentboard.board.dto.CreateArtifactRequest;
import com.agentboard.board.dto.CreateTasksRequest;
import com.agentboard.board.dto.CreateTasksResponse;
import com.agentboard.board.dto.FailTaskRequest;
import com.agentboard.board.dto.FeatureCardResponse;
import com.agentboard.board.dto.FeaturesListResponse;
import com.agentboard.board.dto.MoveStageRequest;
import com.agentboard.board.dto.TaskResponse;
import com.agentboard.board.service.ArtifactService;
import com.agentboard.board.service.FeatureCardService;
import com.agentboard.board.service.TaskService;
import com.agentboard.commons.security.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * MCP-specific endpoints consumed by the MCP server, authenticated via API key.
 *
 * <p>NOTE: {@code POST /api/features} and {@code GET /api/features/{id}} are handled by
 * {@link FeatureCardController} and accept both JWT and API key authentication.
 */
@RestController
@RequestMapping("/api")
public class McpController {

  private final FeatureCardService featureCardService;
  private final TaskService taskService;
  private final ArtifactService artifactService;

  /** Creates the controller with the required services. */
  public McpController(
      FeatureCardService featureCardService,
      TaskService taskService,
      ArtifactService artifactService) {
    this.featureCardService = featureCardService;
    this.taskService = taskService;
    this.artifactService = artifactService;
  }

  /**
   * Returns all feature cards, optionally filtered by stage (MCP {@code list_features}).
   */
  @GetMapping("/features")
  public FeaturesListResponse listFeatures(
      @RequestParam(required = false) String stage) {
    UUID tenantId = TenantContext.get();
    Stage stageEnum = parseStage(stage);
    List<FeatureCardResponse> features = featureCardService.listByTenant(tenantId, stageEnum);
    return new FeaturesListResponse(features);
  }

  /**
   * Moves a feature card to a different workflow stage (MCP {@code move_feature}).
   */
  @PatchMapping("/features/{featureId}/stage")
  public FeatureCardResponse moveFeature(
      @PathVariable UUID featureId,
      @Valid @RequestBody MoveStageRequest request) {
    UUID tenantId = TenantContext.get();
    Stage stage = parseStageStrict(request.stage());
    return featureCardService.moveByStage(tenantId, featureId, stage);
  }

  /**
   * Adds a SpecKit artifact to a feature card (MCP {@code add_artifact}).
   */
  @PostMapping("/features/{featureId}/artifacts")
  @ResponseStatus(HttpStatus.CREATED)
  public ArtifactResponse addArtifact(
      @PathVariable UUID featureId,
      @Valid @RequestBody CreateArtifactRequest request) {
    UUID tenantId = TenantContext.get();
    return artifactService.addArtifact(
        tenantId, featureId, request.command(), request.content(), request.agentIdentifier());
  }

  /**
   * Creates a task checklist for a feature card, moving it to IN_PROGRESS (MCP
   * {@code create_tasks}).
   */
  @PostMapping("/features/{featureId}/tasks")
  @ResponseStatus(HttpStatus.CREATED)
  public CreateTasksResponse createTasks(
      @PathVariable UUID featureId,
      @Valid @RequestBody CreateTasksRequest request) {
    UUID tenantId = TenantContext.get();
    return taskService.createAll(tenantId, featureId, request.tasks());
  }

  /**
   * Marks a task as completed (MCP {@code complete_task}). Auto-moves card to REVIEW if no
   * pending tasks remain.
   */
  @PatchMapping("/features/{featureId}/tasks/{taskId}/complete")
  public CompleteTaskResponse completeTask(
      @PathVariable UUID featureId,
      @PathVariable UUID taskId) {
    UUID tenantId = TenantContext.get();
    return taskService.complete(tenantId, featureId, taskId);
  }

  /**
   * Marks a task as blocked (MCP {@code fail_task}).
   */
  @PatchMapping("/features/{featureId}/tasks/{taskId}/fail")
  public TaskResponse failTask(
      @PathVariable UUID featureId,
      @PathVariable UUID taskId,
      @Valid @RequestBody FailTaskRequest request) {
    UUID tenantId = TenantContext.get();
    return taskService.fail(tenantId, featureId, taskId, request.reason());
  }

  private Stage parseStage(String stage) {
    if (stage == null) {
      return null;
    }
    return parseStageStrict(stage);
  }

  private Stage parseStageStrict(String stage) {
    try {
      return Stage.valueOf(stage);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid stage value: " + stage + ". Must be one of: BACKLOG, SPECIFY, PLAN, "
              + "IN_PROGRESS, REVIEW, DONE");
    }
  }
}
