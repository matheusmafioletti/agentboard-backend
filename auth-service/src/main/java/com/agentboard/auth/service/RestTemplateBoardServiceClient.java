package com.agentboard.auth.service;

import com.agentboard.auth.dto.BoardInfo;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/** Calls board-service over HTTP to create the tenant's initial board. */
@Service
public class RestTemplateBoardServiceClient implements BoardServiceClient {

  private final RestTemplate restTemplate;
  private final String boardServiceUrl;

  /**
   * Creates the client with the given HTTP template and configured board-service base URL.
   */
  public RestTemplateBoardServiceClient(
      RestTemplate restTemplate,
      @Value("${board-service.url}") String boardServiceUrl) {
    this.restTemplate = restTemplate;
    this.boardServiceUrl = boardServiceUrl;
  }

  @Override
  public BoardInfo createBoard(UUID tenantId, String boardName) {
    var request = Map.of("tenantId", tenantId.toString(), "name", boardName);
    return restTemplate.postForObject(
        boardServiceUrl + "/internal/boards", request, BoardInfo.class);
  }
}
