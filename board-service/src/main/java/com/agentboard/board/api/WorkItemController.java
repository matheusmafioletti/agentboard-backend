package com.agentboard.board.api;

import com.agentboard.board.api.dto.BatchCreateRequest;
import com.agentboard.board.api.dto.BatchCreateResponse;
import com.agentboard.board.api.dto.CreateWorkItemRequest;
import com.agentboard.board.api.dto.MoveStatusRequest;
import com.agentboard.board.api.dto.ParentPreviewResponse;
import com.agentboard.board.api.dto.PatchWorkItemRequest;
import com.agentboard.board.api.dto.WorkItemDetailResponse;
import com.agentboard.board.api.dto.WorkItemResponse;
import com.agentboard.board.domain.Artifact;
import com.agentboard.board.domain.CommandExecution;
import com.agentboard.board.domain.WorkItem;
import com.agentboard.board.repository.ArtifactRepository;
import com.agentboard.board.repository.CommandExecutionRepository;
import com.agentboard.board.repository.WorkItemRepository;
import com.agentboard.board.security.ProjectPrincipal;
import com.agentboard.board.service.ArtifactService;
import com.agentboard.board.service.WorkItemService;
import com.agentboard.commons.domain.WorkItemType;
import com.agentboard.commons.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * REST endpoints for unified WorkItem lifecycle management.
 *
 * <ul>
 *   <li>{@code GET /api/v1/work-items} — list with optional type/parentId/status filters</li>
 *   <li>{@code POST /api/v1/work-items} — create a single work item</li>
 *   <li>{@code POST /api/v1/work-items/batch} — create multiple items of one type</li>
 *   <li>{@code GET /api/v1/work-items/{id}} — get detail (children + artifacts)</li>
 *   <li>{@code PATCH /api/v1/work-items/{id}} — update title/description</li>
 *   <li>{@code PATCH /api/v1/work-items/{id}/status} — move to new status</li>
 *   <li>{@code POST /api/v1/work-items/{id}/artifacts} — append an artifact</li>
 * </ul>
 *
 * <p>Supports project API key auth ({@link ProjectPrincipal}) and browser JWT auth
 * (principal is null; tenantId from {@link TenantContext}; projectId from query param or record).
 */
@RestController
@RequestMapping("/api/v1/work-items")
public class WorkItemController {

  private final WorkItemService workItemService;
  private final ArtifactService artifactService;
  private final WorkItemRepository workItemRepository;
  private final ArtifactRepository artifactRepository;
  private final CommandExecutionRepository commandExecutionRepository;

  /** Creates the controller with the required services and repositories. */
  public WorkItemController(WorkItemService workItemService, ArtifactService artifactService,
      WorkItemRepository workItemRepository, ArtifactRepository artifactRepository,
      CommandExecutionRepository commandExecutionRepository) {
    this.workItemService = workItemService;
    this.artifactService = artifactService;
    this.workItemRepository = workItemRepository;
    this.artifactRepository = artifactRepository;
    this.commandExecutionRepository = commandExecutionRepository;
  }

  /** Lists work items for a project with optional filters. */
  @GetMapping
  public List<WorkItemResponse> listWorkItems(
      @RequestParam UUID projectId,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) UUID parentId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false, defaultValue = "false") boolean includeParent,
      @RequestParam(required = false) UUID assigneeId,
      @AuthenticationPrincipal ProjectPrincipal principal) {
    UUID tenantId = resolveTenantId(principal);
    WorkItemType typeEnum = type != null ? WorkItemType.valueOf(type) : null;
    List<WorkItem> rows =
        workItemService.listWorkItems(tenantId, projectId, typeEnum, parentId, status, assigneeId);
    if (!includeParent) {
      return rows.stream().map(WorkItemResponse::from).toList();
    }
    var parentIds = rows.stream()
        .map(WorkItem::getParentId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<UUID, WorkItem> parentsById = Map.of();
    if (!parentIds.isEmpty()) {
      parentsById = workItemRepository.findAllByTenantIdAndIdIn(tenantId, parentIds).stream()
          .collect(Collectors.toMap(WorkItem::getId, Function.identity()));
    }
    final Map<UUID, WorkItem> parentMap = parentsById;
    return rows.stream()
        .map(wi -> {
          ParentPreviewResponse preview = null;
          if (wi.getParentId() != null) {
            WorkItem parent = parentMap.get(wi.getParentId());
            preview = parent != null ? ParentPreviewResponse.fromWorkItem(parent) : null;
          }
          return WorkItemResponse.from(wi, preview);
        })
        .toList();
  }

  /** Creates a single work item. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WorkItemResponse createWorkItem(
      @Valid @RequestBody CreateWorkItemRequest request,
      @RequestParam(required = false) UUID projectId,
      @AuthenticationPrincipal ProjectPrincipal principal) {
    UUID tenantId = resolveTenantId(principal);
    UUID resolvedProjectId = resolveProjectId(principal, projectId);
    WorkItemType type = WorkItemType.valueOf(request.type());
    WorkItem item = workItemService.createWorkItem(
        tenantId, resolvedProjectId, type,
        request.title(), request.description(),
        request.parentId(), request.priority() > 0 ? request.priority() : 5,
        request.assigneeId());
    return WorkItemResponse.from(item);
  }

  /** Creates multiple work items of the same type in one request. */
  @PostMapping("/batch")
  @ResponseStatus(HttpStatus.CREATED)
  public BatchCreateResponse batchCreateWorkItems(
      @Valid @RequestBody BatchCreateRequest request,
      @RequestParam(required = false) UUID projectId,
      @AuthenticationPrincipal ProjectPrincipal principal) {
    UUID tenantId = resolveTenantId(principal);
    UUID resolvedProjectId = resolveProjectId(principal, projectId);
    WorkItemType type = WorkItemType.valueOf(request.type());

    List<WorkItem> created = request.items().stream().map(item ->
        workItemService.createWorkItem(
            tenantId, resolvedProjectId, type,
            item.title(), item.description(),
            request.parentId(), item.priority() > 0 ? item.priority() : 5, null)
    ).toList();

    String parentStatus = workItemRepository.findByIdAndTenantId(request.parentId(), tenantId)
        .map(WorkItem::getStatus)
        .orElse(null);

    return new BatchCreateResponse(
        created.stream().map(WorkItemResponse::from).toList(),
        parentStatus);
  }

  /** Returns a single work item with full detail. */
  @GetMapping("/{id}")
  public WorkItemDetailResponse getWorkItem(
      @PathVariable UUID id,
      @AuthenticationPrincipal ProjectPrincipal principal) {
    UUID tenantId = resolveTenantId(principal);
    WorkItem item = workItemService.getWorkItemDetail(tenantId, id);
    ParentPreviewResponse parentPreview = null;
    if (item.getParentId() != null) {
      parentPreview =
          workItemRepository.findByIdAndTenantId(item.getParentId(), tenantId)
              .map(ParentPreviewResponse::fromWorkItem).orElse(null);
    }
    List<WorkItem> children = workItemRepository.findAllByParentId(id);
    List<Artifact> artifacts = artifactRepository.findByWorkItemIdOrderByCreatedAtAsc(id);
    List<CommandExecution> executions =
        commandExecutionRepository.findByWorkItemIdOrderByStartedAtDesc(id);
    return WorkItemDetailResponse.from(item, children, artifacts, executions, parentPreview);
  }

  /** Updates title, description, and/or assignee of a work item. */
  @PatchMapping("/{id}")
  public WorkItemResponse patchWorkItem(
      @PathVariable UUID id,
      @Valid @RequestBody PatchWorkItemRequest request,
      @AuthenticationPrincipal ProjectPrincipal principal) {
    UUID tenantId = resolveTenantId(principal);
    java.util.Optional<UUID> assigneeUpdate = resolveAssigneeUpdate(request.assignee());
    return WorkItemResponse.from(
        workItemService.patchWorkItem(tenantId, id, request.title(), request.description(),
            assigneeUpdate));
  }

  private java.util.Optional<UUID> resolveAssigneeUpdate(
      PatchWorkItemRequest.AssigneeUpdate assignee) {
    if (assignee == null) {
      return null;
    }
    if (assignee.clear()) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.ofNullable(assignee.id());
  }

  /** Moves a work item to a new status. */
  @PatchMapping("/{id}/status")
  public WorkItemResponse moveStatus(
      @PathVariable UUID id,
      @Valid @RequestBody MoveStatusRequest request,
      @AuthenticationPrincipal ProjectPrincipal principal) {
    UUID tenantId = resolveTenantId(principal);
    return WorkItemResponse.from(workItemService.moveStatus(tenantId, id, request.status()));
  }

  /** Appends an artifact to a work item. */
  @PostMapping("/{id}/artifacts")
  @ResponseStatus(HttpStatus.CREATED)
  public ArtifactResponse addArtifact(
      @PathVariable UUID id,
      @Valid @RequestBody AddArtifactRequest request,
      @AuthenticationPrincipal ProjectPrincipal principal) {
    UUID tenantId = resolveTenantId(principal);
    workItemService.getWorkItemDetail(tenantId, id);
    Artifact artifact = artifactService.saveArtifact(id, request.command(), request.content());
    return ArtifactResponse.from(artifact);
  }

  private UUID resolveTenantId(ProjectPrincipal principal) {
    return principal != null ? principal.tenantId() : TenantContext.get();
  }

  private UUID resolveProjectId(ProjectPrincipal principal, UUID projectIdParam) {
    if (principal != null) {
      return principal.projectId();
    }
    if (projectIdParam != null) {
      return projectIdParam;
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "projectId query parameter is required for JWT-authenticated requests");
  }

  /** Request body for artifact creation. */
  public record AddArtifactRequest(
      @NotBlank @Size(max = 100) String command,
      @NotBlank String content
  ) {}

  /** Response DTO for an artifact. */
  public record ArtifactResponse(
      UUID id,
      UUID workItemId,
      String command,
      String content,
      String createdAt
  ) {
    static ArtifactResponse from(Artifact a) {
      return new ArtifactResponse(
          a.getId(),
          a.getWorkItemId(),
          a.getCommand(),
          a.getContent(),
          a.getCreatedAt().toString()
      );
    }
  }
}
