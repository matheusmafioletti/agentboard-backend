package com.agentboard.auth.exception;

/** Thrown when the caller lacks permission for an operation. */
public class ForbiddenOperationException extends RuntimeException {

  /** Creates the exception with the given message. */
  public ForbiddenOperationException(String message) {
    super(message);
  }
}
