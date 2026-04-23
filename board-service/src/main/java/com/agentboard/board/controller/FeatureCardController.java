package com.agentboard.board.controller;

import com.agentboard.board.dto.CreateFeatureRequest;
import com.agentboard.board.dto.FeatureCardResponse;
import com.agentboard.board.dto.MoveFeatureRequest;
import com.agentboard.board.dto.PatchFeatureRequest;
import com.agentboard.board.service.FeatureCardService;
import com.agentboard.commons.security.TenantContext;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD endpoints for Feature Cards, authenticated via JWT. */
@RestController
@RequestMapping("/api/features")
public class FeatureCardController {

  private final FeatureCardService featureCardService;

  /** Creates the controller with the required service. */
  public FeatureCardController(FeatureCardService featureCardService) {
    this.featureCardService = featureCardService;
  }

  /**
   * Creates a new Feature Card in the tenant's Backlog column.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FeatureCardResponse create(@Valid @RequestBody CreateFeatureRequest request) {
    UUID tenantId = TenantContext.get();
    return featureCardService.create(tenantId, request.title(), request.description());
  }

  /**
   * Returns the full detail of a Feature Card.
   */
  @GetMapping("/{id}")
  public FeatureCardResponse get(@PathVariable UUID id) {
    UUID tenantId = TenantContext.get();
    return featureCardService.getById(tenantId, id);
  }

  /**
   * Partially updates an existing Feature Card.
   */
  @PatchMapping("/{id}")
  public FeatureCardResponse patch(
      @PathVariable UUID id,
      @Valid @RequestBody PatchFeatureRequest request) {
    UUID tenantId = TenantContext.get();
    return featureCardService.update(
        tenantId, id, request.title(), request.description(), request.reExecutionPending());
  }

  /**
   * Moves a Feature Card to a different column at the specified position.
   */
  @PatchMapping("/{id}/move")
  public FeatureCardResponse move(
      @PathVariable UUID id,
      @Valid @RequestBody MoveFeatureRequest request) {
    UUID tenantId = TenantContext.get();
    return featureCardService.move(tenantId, id, request.targetColumnId(), request.displayOrder());
  }

  /**
   * Deletes a Feature Card.
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    UUID tenantId = TenantContext.get();
    featureCardService.delete(tenantId, id);
  }
}
