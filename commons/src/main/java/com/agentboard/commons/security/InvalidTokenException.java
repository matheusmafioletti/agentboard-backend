package com.agentboard.commons.security;

/**
 * Thrown when a JWT token is invalid, expired, or has been tampered with.
 */
public class InvalidTokenException extends RuntimeException {

  /**
   * Creates an exception with the given detail message.
   */
  public InvalidTokenException(String message) {
    super(message);
  }

  /**
   * Creates an exception with the given detail message and root cause.
   */
  public InvalidTokenException(String message, Throwable cause) {
    super(message, cause);
  }
}
