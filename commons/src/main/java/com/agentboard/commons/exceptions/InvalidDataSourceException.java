package com.agentboard.commons.exceptions;

/**
 * Thrown when {@code X-Data-Source} carries a value other than manual, automation, or seed.
 */
public class InvalidDataSourceException extends RuntimeException {

  /**
   * Creates an exception with the given detail message.
   */
  public InvalidDataSourceException(String message) {
    super(message);
  }
}
