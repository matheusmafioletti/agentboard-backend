package com.agentboard.commons.exceptions;

/**
 * Thrown when an operation is attempted on a resource belonging to a different tenant.
 */
public class TenantMismatchException extends RuntimeException {

  /**
   * Creates an exception with the given detail message.
   */
  public TenantMismatchException(String message) {
    super(message);
  }
}
