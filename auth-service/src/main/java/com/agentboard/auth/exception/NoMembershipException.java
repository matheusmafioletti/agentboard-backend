package com.agentboard.auth.exception;

/** Thrown when a user has no active tenant memberships. */
public class NoMembershipException extends RuntimeException {

  /** Creates the exception with a default message. */
  public NoMembershipException() {
    super("User has no workspace memberships");
  }
}
