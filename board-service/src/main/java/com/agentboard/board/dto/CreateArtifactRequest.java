package com.agentboard.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for adding a SpecKit artifact to a feature card. */
public record CreateArtifactRequest(
    @NotBlank(message = "command is required")
    @Pattern(regexp = "specify|clarify|plan|tasks|implement",
        message = "command must be one of: specify, clarify, plan, tasks, implement")
    String command,

    @NotBlank(message = "content is required")
    String content,

    String agentIdentifier
) {}
