package com.agentboard.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** One of the six fixed workflow columns on a board. */
@Entity
@Table(name = "column_def")
public class ColumnDef {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 50)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Stage stage;

  @Column(name = "display_order", nullable = false)
  private short displayOrder;

  /** Required by JPA. */
  protected ColumnDef() {}

  /**
   * Creates a column definition for the given board.
   */
  public ColumnDef(Board board, UUID tenantId, String name, Stage stage, short displayOrder) {
    this.board = board;
    this.tenantId = tenantId;
    this.name = name;
    this.stage = stage;
    this.displayOrder = displayOrder;
  }

  /** Returns the column's unique identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the board this column belongs to. */
  public Board getBoard() {
    return board;
  }

  /** Returns the tenant this column is associated with. */
  public UUID getTenantId() {
    return tenantId;
  }

  /** Returns the column's display name. */
  public String getName() {
    return name;
  }

  /** Returns the workflow stage this column represents. */
  public Stage getStage() {
    return stage;
  }

  /** Returns the zero-based display position of this column. */
  public short getDisplayOrder() {
    return displayOrder;
  }
}
