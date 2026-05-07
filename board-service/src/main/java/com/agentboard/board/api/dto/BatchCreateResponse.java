package com.agentboard.board.api.dto;

import java.util.List;

/** Response for batch work item creation, including the updated parent status. */
public record BatchCreateResponse(
    List<WorkItemResponse> workItems,
    String parentStatus
) {}
