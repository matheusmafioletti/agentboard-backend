package com.agentboard.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Read-only projection of the {@code tenant_membership} table owned by auth-service.
 *
 * <p>NOTE: Both services share the same PostgreSQL schema in the MVP deployment. This entity
 * is intentionally read-only ({@link Immutable}) and must never be persisted from board-service.
 */
@Entity
@Immutable
@Table(name = "tenant_membership")
public class TenantMembership {

  @Id
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "joined_at", nullable = false)
  private OffsetDateTime joinedAt;

  /** Required by JPA. */
  protected TenantMembership() {}

  /** Returns the membership identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the associated user id. */
  public UUID getUserId() {
    return userId;
  }

  /** Returns the associated tenant id. */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns when the user joined this tenant. */
  public OffsetDateTime getJoinedAt() {
    return joinedAt;
  }
}
