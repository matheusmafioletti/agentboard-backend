package com.agentboard.auth.domain;

import com.agentboard.commons.domain.DataSource;
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

/** Associates a global user with a tenant workspace and role. */
@Entity
@Table(name = "tenant_membership")
public class TenantMembership {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MembershipRole role;

  @Column(name = "joined_at", nullable = false, updatable = false)
  private OffsetDateTime joinedAt;

  @Column(name = "data_source", nullable = false, length = 20)
  private DataSource dataSource = DataSource.MANUAL;

  /** Required by JPA. */
  protected TenantMembership() {}

  /** Creates a new membership for the given user and tenant. */
  public TenantMembership(UUID userId, UUID tenantId, MembershipRole role) {
    this(userId, tenantId, role, DataSource.MANUAL);
  }

  /** Creates a membership with an explicit data provenance tag. */
  public TenantMembership(UUID userId, UUID tenantId, MembershipRole role, DataSource dataSource) {
    this.userId = userId;
    this.tenantId = tenantId;
    this.role = role;
    this.joinedAt = OffsetDateTime.now();
    this.dataSource = dataSource != null ? dataSource : DataSource.MANUAL;
  }

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

  /** Returns the role within the tenant. */
  public MembershipRole getRole() {
    return role;
  }

  /** Returns when the user joined this tenant. */
  public OffsetDateTime getJoinedAt() {
    return joinedAt;
  }

  /** Returns the provenance tag for this membership. */
  public DataSource getDataSource() {
    return dataSource;
  }
}
