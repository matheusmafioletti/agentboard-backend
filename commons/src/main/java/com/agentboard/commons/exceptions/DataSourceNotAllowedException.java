package com.agentboard.commons.exceptions;

/**
 * Thrown when automation or seed data sources are used without a {@code test_tenant} workspace.
 */
public class DataSourceNotAllowedException extends RuntimeException {

  /**
   * Creates an exception with the given detail message.
   */
  public DataSourceNotAllowedException(String message) {
    super(message);
  }
}
