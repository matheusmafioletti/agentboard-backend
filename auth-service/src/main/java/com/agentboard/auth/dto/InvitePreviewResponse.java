package com.agentboard.auth.dto;

import com.agentboard.auth.domain.InviteStatus;
import java.time.OffsetDateTime;

/** Public preview of an invite before acceptance. */
public record InvitePreviewResponse(
    String tenantName,
    String email,
    InviteStatus status,
    OffsetDateTime expiresAt,
    boolean requiresRegistration
) {}
