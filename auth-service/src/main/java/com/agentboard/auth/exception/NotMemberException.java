package com.agentboard.auth.exception;

/** Thrown when a user is not a member of the requested tenant. */
public class NotMemberException extends RuntimeException {

  /** Creates the exception with a default message. */
  public NotMemberException() {
    super("User is not a member of this workspace");
  }
}
