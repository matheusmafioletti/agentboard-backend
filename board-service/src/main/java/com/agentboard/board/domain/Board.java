package com.agentboard.board.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The Kanban board for a single tenant, containing exactly six columns in MVP. */
@Entity
@Table(name = "board")
public class Board {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @OrderBy("displayOrder ASC")
  private List<ColumnDef> columns = new ArrayList<>();

  /** Required by JPA. */
  protected Board() {}

  /**
   * Creates a new board for the given tenant.
   */
  public Board(UUID tenantId, String name) {
    this.tenantId = tenantId;
    this.name = name;
    this.createdAt = OffsetDateTime.now();
    this.updatedAt = OffsetDateTime.now();
  }

  /** Returns the board's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the tenant this board belongs to. */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns the board's display name. */
  public String getName() {
    return name;
  }

  /** Returns the instant this board was created. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /** Returns the instant this board was last updated. */
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  /** Returns the columns belonging to this board, ordered by display order. */
  public List<ColumnDef> getColumns() {
    return columns;
  }

  /** Updates the last-modified timestamp. */
  public void touch() {
    this.updatedAt = OffsetDateTime.now();
  }
}
