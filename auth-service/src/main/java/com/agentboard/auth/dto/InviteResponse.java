package com.agentboard.auth.dto;

import com.agentboard.auth.domain.InviteStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Invite summary returned by admin and public preview endpoints. */
public record InviteResponse(
    UUID id,
    String email,
    InviteStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt,
    String inviteUrl
) {}
