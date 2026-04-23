package com.agentboard.auth.exception;

/** Thrown when a login attempt fails due to unrecognized credentials. */
public class InvalidCredentialsException extends RuntimeException {

  /** Creates the exception with a generic message to avoid leaking account existence. */
  public InvalidCredentialsException() {
    super("Invalid email or password");
  }
}
