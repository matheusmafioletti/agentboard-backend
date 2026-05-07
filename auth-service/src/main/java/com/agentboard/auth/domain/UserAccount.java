package com.agentboard.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A registered user belonging to a single tenant. */
@Entity
@Table(name = "user_account")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(nullable = false, columnDefinition = "varchar(50)[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private String[] roles = {"USER"};

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  /** Required by JPA. */
  protected UserAccount() {}

  /**
   * Creates a new user account with the given credentials and tenant association.
   */
  public UserAccount(UUID tenantId, String email, String passwordHash) {
    this.tenantId = tenantId;
    this.email = email;
    this.passwordHash = passwordHash;
    this.roles = new String[]{"USER"};
    this.createdAt = OffsetDateTime.now();
  }

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

  /** Returns the BCrypt-hashed password. */
  public String getPasswordHash() {
    return passwordHash;
  }

  /** Returns the roles assigned to this user. */
  public List<String> getRoles() {
    return roles == null ? List.of() : List.of(roles);
  }

  /** Returns the instant this account was created. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /** Replaces the stored password hash with a newly encoded value. */
  public void updatePasswordHash(String newHash) {
    this.passwordHash = newHash;
  }
}
