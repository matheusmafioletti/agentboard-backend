package com.agentboard.auth.exception;

/** Thrown when revoking the last admin of a tenant. */
public class LastAdminException extends RuntimeException {

  /** Creates the exception with a default message. */
  public LastAdminException() {
    super("Cannot remove the last administrator of this workspace");
  }
}
