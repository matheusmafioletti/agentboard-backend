package com.agentboard.auth.exception;

/** Thrown when a pending invite already exists for the same tenant and email. */
public class DuplicatePendingInviteException extends RuntimeException {

  /** Creates the exception with a default message. */
  public DuplicatePendingInviteException() {
    super("A pending invite already exists for this email");
  }
}
