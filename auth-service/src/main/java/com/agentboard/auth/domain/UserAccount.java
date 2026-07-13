package com.agentboard.auth.domain;

import com.agentboard.commons.domain.DataSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Global user identity (email + credentials), independent of any tenant. */
@Entity
@Table(name = "user_account")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "data_source", nullable = false, length = 20)
  private DataSource dataSource = DataSource.MANUAL;

  /** Required by JPA. */
  protected UserAccount() {}

  /** Creates a new global user account defaulting to {@link DataSource#MANUAL}. */
  public UserAccount(String name, String email, String passwordHash) {
    this(name, email, passwordHash, DataSource.MANUAL);
  }

  /** Creates a new global user account with the given data provenance. */
  public UserAccount(String name, String email, String passwordHash, DataSource dataSource) {
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.createdAt = OffsetDateTime.now();
    this.dataSource = dataSource != null ? dataSource : DataSource.MANUAL;
  }

  /** Returns the user's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the user's display name. */
  public String getName() {
    return name;
  }

  /** Returns the user's email address. */
  public String getEmail() {
    return email;
  }

  /** Returns the BCrypt-hashed password. */
  public String getPasswordHash() {
    return passwordHash;
  }

  /** Returns the instant this account was created. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /** Returns the provenance tag for this account. */
  public DataSource getDataSource() {
    return dataSource;
  }

  /** Replaces the stored password hash with a newly encoded value. */
  public void updatePasswordHash(String newHash) {
    this.passwordHash = newHash;
  }
}
