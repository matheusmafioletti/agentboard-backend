package com.agentboard.auth.exception;

/** Thrown when an invite is expired, cancelled, or otherwise unusable. */
public class InviteGoneException extends RuntimeException {

  /** Creates the exception with the given message. */
  public InviteGoneException(String message) {
    super(message);
  }
}
