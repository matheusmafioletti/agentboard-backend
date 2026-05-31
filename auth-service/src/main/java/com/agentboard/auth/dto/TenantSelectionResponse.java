package com.agentboard.auth.dto;

import java.util.List;
import java.util.UUID;

/** Returned when login succeeds but the user must pick a workspace. */
public record TenantSelectionResponse(
    boolean requiresTenantSelection,
    UUID userId,
    String email,
    String name,
    List<TenantMembershipSummary> memberships
) {

  /** Creates a tenant selection response for the given user and memberships. */
  public TenantSelectionResponse(UUID userId, String email, String name,
      List<TenantMembershipSummary> memberships) {
    this(true, userId, email, name, memberships);
  }
}
