package com.agentboard.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Read-only projection of the {@code user_account} table owned by auth-service.
 *
 * <p>NOTE: Both services share the same PostgreSQL schema in the MVP deployment. This entity
 * is intentionally read-only ({@link Immutable}) and must never be persisted from board-service.
 */
@Entity
@Immutable
@Table(name = "user_account")
public class TenantUser {

  @Id
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false)
  private String email;

  /** Required by JPA. */
  protected TenantUser() {}

  /** Returns the user's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the tenant this user belongs to. */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns the user's email address. */
  public String getEmail() {
    return email;
  }
}
