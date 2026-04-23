package com.agentboard.auth.service;

import com.agentboard.auth.dto.BoardInfo;
import java.util.UUID;

/** Client for calling board-service to create a tenant's initial board. */
public interface BoardServiceClient {

  /**
   * Requests board-service to create a new board for the given tenant.
   *
   * @param tenantId  the owning tenant's identifier
   * @param boardName the desired display name for the board
   * @return summary of the created board
   */
  BoardInfo createBoard(UUID tenantId, String boardName);
}
