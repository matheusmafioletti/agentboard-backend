package com.agentboard.commons.security;

import java.util.UUID;

/**
 * Thread-local holder for the current request's tenant identifier.
 *
 * <p>Must be cleared in a {@code finally} block after every request to prevent the tenant ID
 * from leaking across pooled threads.
 */
public final class TenantContext {

  private static final ThreadLocal<UUID> tenant = new ThreadLocal<>();

  private TenantContext() {}

  /**
   * Returns the tenant ID bound to the current thread, or {@code null} if none is set.
   */
  public static UUID get() {
    return tenant.get();
  }

  /**
   * Binds the given tenant ID to the current thread.
   */
  public static void set(UUID tenantId) {
    tenant.set(tenantId);
  }

  /**
   * Removes the tenant ID from the current thread.
   */
  public static void clear() {
    tenant.remove();
  }
}
