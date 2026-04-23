package com.agentboard.auth.exception;

/** Thrown when registration is attempted with an email address that is already registered. */
public class DuplicateEmailException extends RuntimeException {

  /**
   * Creates the exception for the given email address.
   */
  public DuplicateEmailException(String email) {
    super("Email already registered: " + email);
  }
}
