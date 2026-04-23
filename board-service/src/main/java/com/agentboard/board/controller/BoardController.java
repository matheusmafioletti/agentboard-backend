package com.agentboard.board.controller;

import com.agentboard.board.domain.Board;
import com.agentboard.board.domain.ColumnDef;
import com.agentboard.board.domain.FeatureCard;
import com.agentboard.board.dto.BoardCreatedResponse;
import com.agentboard.board.dto.BoardResponse;
import com.agentboard.board.dto.ColumnResponse;
import com.agentboard.board.dto.CreateBoardInternalRequest;
import com.agentboard.board.repository.BoardRepository;
import com.agentboard.board.repository.ColumnDefRepository;
import com.agentboard.board.repository.FeatureCardRepository;
import com.agentboard.commons.exceptions.ResourceNotFoundException;
import com.agentboard.commons.security.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Handles board retrieval and internal board creation. */
@RestController
public class BoardController {

  private final BoardRepository boardRepository;
  private final ColumnDefRepository columnDefRepository;
  private final FeatureCardRepository featureCardRepository;

  /**
   * Creates the controller with the given repositories.
   */
  public BoardController(
      BoardRepository boardRepository,
      ColumnDefRepository columnDefRepository,
      FeatureCardRepository featureCardRepository) {
    this.boardRepository = boardRepository;
    this.columnDefRepository = columnDefRepository;
    this.featureCardRepository = featureCardRepository;
  }

  /**
   * Returns the authenticated tenant's board with all columns and feature card summaries.
   */
  @GetMapping("/api/boards/current")
  public BoardResponse getCurrentBoard() {
    UUID tenantId = TenantContext.get();
    Board board = boardRepository.findByTenantId(tenantId)
        .orElseThrow(() -> ResourceNotFoundException.forId("Board", tenantId));

    List<ColumnDef> columns = columnDefRepository
        .findByBoardIdOrderByDisplayOrderAsc(board.getId());
    List<ColumnResponse> columnResponses = columns.stream()
        .map(col -> {
          List<FeatureCard> cards = featureCardRepository
              .findByColumnIdAndTenantIdOrderByDisplayOrderAsc(col.getId(), tenantId);
          List<ColumnResponse.FeatureCardSummary> summaries = cards.stream()
              .map(card -> new ColumnResponse.FeatureCardSummary(
                  card.getId(),
                  card.getTitle(),
                  card.getDescription(),
                  card.isReExecutionPending(),
                  0,
                  0,
                  card.getDisplayOrder(),
                  card.getCreatedAt() != null ? card.getCreatedAt().toString() : null,
                  card.getUpdatedAt() != null ? card.getUpdatedAt().toString() : null))
              .toList();
          return new ColumnResponse(
              col.getId(), col.getName(), col.getStage(), col.getDisplayOrder(), summaries);
        })
        .toList();

    return new BoardResponse(
        board.getId(), board.getName(), board.getTenantId(), columnResponses);
  }

  /**
   * Creates a new board for the given tenant; called only by auth-service on registration.
   */
  @PostMapping("/internal/boards")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardCreatedResponse createBoardInternal(
      @Valid @RequestBody CreateBoardInternalRequest request) {
    Board board = boardRepository.save(new Board(request.tenantId(), request.name()));
    return new BoardCreatedResponse(board.getId(), board.getName());
  }
}
