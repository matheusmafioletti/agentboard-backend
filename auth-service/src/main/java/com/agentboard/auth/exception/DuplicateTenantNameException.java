package com.agentboard.auth.exception;

/** Thrown when a tenant name is already registered globally. */
public class DuplicateTenantNameException extends RuntimeException {

  /** Creates the exception for the conflicting tenant name. */
  public DuplicateTenantNameException(String tenantName) {
    super("Tenant name already taken: " + tenantName);
  }
}
