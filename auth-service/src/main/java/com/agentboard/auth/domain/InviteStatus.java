package com.agentboard.auth.domain;

/** Lifecycle state of a tenant invitation. */
public enum InviteStatus {
  PENDING,
  ACCEPTED,
  CANCELLED,
  EXPIRED
}
