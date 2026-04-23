package com.agentboard.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Represents an isolated workspace for a group of users. */
@Entity
@Table(name = "tenant")
public class Tenant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  /** Required by JPA. */
  protected Tenant() {}

  /**
   * Creates a new tenant with the given name, setting {@code createdAt} to now.
   */
  public Tenant(String name) {
    this.name = name;
    this.createdAt = OffsetDateTime.now();
  }

  /** Returns the tenant's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the tenant's display name. */
  public String getName() {
    return name;
  }

  /** Returns the instant this tenant was created. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
