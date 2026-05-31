package com.agentboard.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Invitation for a user to join a tenant workspace. */
@Entity
@Table(name = "tenant_invite")
public class TenantInvite {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(name = "token_hash", nullable = false, unique = true, length = 255)
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InviteStatus status;

  @Column(name = "invited_by", nullable = false)
  private UUID invitedBy;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "accepted_at")
  private OffsetDateTime acceptedAt;

  /** Required by JPA. */
  protected TenantInvite() {}

  /** Creates a pending invite for the given tenant and email. */
  public TenantInvite(
      UUID tenantId,
      String email,
      String tokenHash,
      UUID invitedBy,
      OffsetDateTime expiresAt) {
    this.tenantId = tenantId;
    this.email = email.toLowerCase();
    this.tokenHash = tokenHash;
    this.status = InviteStatus.PENDING;
    this.invitedBy = invitedBy;
    this.expiresAt = expiresAt;
    this.createdAt = OffsetDateTime.now();
  }

  /** Returns the invite identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the tenant this invite targets. */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns the invited email (normalized). */
  public String getEmail() {
    return email;
  }

  /** Returns the SHA-256 hash of the raw invite token. */
  public String getTokenHash() {
    return tokenHash;
  }

  /** Returns the current invite status. */
  public InviteStatus getStatus() {
    return status;
  }

  /** Returns the admin user who created the invite. */
  public UUID getInvitedBy() {
    return invitedBy;
  }

  /** Returns when the invite expires. */
  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  /** Returns when the invite was created. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /** Returns when the invite was accepted, if applicable. */
  public OffsetDateTime getAcceptedAt() {
    return acceptedAt;
  }

  /** Marks the invite as accepted at the current instant. */
  public void markAccepted() {
    this.status = InviteStatus.ACCEPTED;
    this.acceptedAt = OffsetDateTime.now();
  }

  /** Marks the invite as cancelled. */
  public void markCancelled() {
    this.status = InviteStatus.CANCELLED;
  }

  /** Marks the invite as expired. */
  public void markExpired() {
    this.status = InviteStatus.EXPIRED;
  }

  /** Returns whether the invite is past its expiry and still pending. */
  public boolean isExpiredPending() {
    return status == InviteStatus.PENDING && OffsetDateTime.now().isAfter(expiresAt);
  }
}
