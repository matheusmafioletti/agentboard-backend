package com.agentboard.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A hashed API key granting MCP server access for a tenant. */
@Entity
@Table(name = "tenant_api_key")
public class TenantApiKey {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "key_hash", nullable = false, unique = true, length = 255)
  private String keyHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  /** Required by JPA. */
  protected TenantApiKey() {}

  /**
   * Creates a new API key record for the given tenant.
   *
   * @param tenantId the tenant this key belongs to
   * @param keyHash  SHA-256 hex digest of the raw API key
   */
  public TenantApiKey(UUID tenantId, String keyHash) {
    this.tenantId = tenantId;
    this.keyHash = keyHash;
    this.createdAt = OffsetDateTime.now();
  }

  /** Returns the unique identifier of this key record. */
  public UUID getId() {
    return id;
  }

  /** Returns the tenant this key is associated with. */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns the SHA-256 hex digest of the raw API key. */
  public String getKeyHash() {
    return keyHash;
  }

  /** Returns the instant this key was created. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /** Returns the instant this key was revoked, or {@code null} if active. */
  public OffsetDateTime getRevokedAt() {
    return revokedAt;
  }
}
