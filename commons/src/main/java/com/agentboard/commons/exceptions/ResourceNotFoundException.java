package com.agentboard.commons.exceptions;

/**
 * Thrown when a requested resource does not exist or is not visible to the current tenant.
 */
public class ResourceNotFoundException extends RuntimeException {

  /**
   * Creates an exception with the given detail message.
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }

  /**
   * Creates an exception with a formatted message identifying the missing resource.
   *
   * @param resourceType the type of the resource (e.g., "FeatureCard")
   * @param id the identifier that was not found
   * @return a new {@link ResourceNotFoundException}
   */
  public static ResourceNotFoundException forId(String resourceType, Object id) {
    return new ResourceNotFoundException(resourceType + " not found: " + id);
  }
}
