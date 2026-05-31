package com.agentboard.auth.exception;

/** Thrown when inviting a user who is already a member. */
public class AlreadyMemberException extends RuntimeException {

  /** Creates the exception with a default message. */
  public AlreadyMemberException() {
    super("User is already a member of this workspace");
  }
}
