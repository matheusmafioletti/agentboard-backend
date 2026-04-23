package com.agentboard.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only projection of the {@code tenant_api_key} table owned by auth-service.
 *
 * <p>NOTE: board-service reads this table directly because both services share a single
 * PostgreSQL database in MVP. No writes are performed from this service.
 */
@Entity
@Table(name = "tenant_api_key")
public class TenantApiKey {

  @Id
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "key_hash", nullable = false, unique = true, length = 255)
  private String keyHash;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  /** Required by JPA. */
  protected TenantApiKey() {}

  /** Creates an API key record for the given tenant with a pre-computed key hash. */
  public TenantApiKey(UUID tenantId, String keyHash) {
    this.id = UUID.randomUUID();
    this.tenantId = tenantId;
    this.keyHash = keyHash;
    this.createdAt = java.time.OffsetDateTime.now();
  }

  /** Returns the tenant this key is associated with. */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns the SHA-256 hex digest of the raw API key. */
  public String getKeyHash() {
    return keyHash;
  }

  /** Returns the instant this key was revoked, or {@code null} if still active. */
  public OffsetDateTime getRevokedAt() {
    return revokedAt;
  }
}
