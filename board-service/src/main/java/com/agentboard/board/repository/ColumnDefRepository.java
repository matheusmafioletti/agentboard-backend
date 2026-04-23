package com.agentboard.board.repository;

import com.agentboard.board.domain.ColumnDef;
import com.agentboard.board.domain.Stage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for {@link ColumnDef} entities. */
@Repository
public interface ColumnDefRepository extends JpaRepository<ColumnDef, UUID> {

  /** Returns all columns for the given board, ordered by display position. */
  List<ColumnDef> findByBoardIdOrderByDisplayOrderAsc(UUID boardId);

  /**
   * Returns the column for the given tenant that maps to the given workflow stage,
   * if one exists.
   */
  Optional<ColumnDef> findByTenantIdAndStage(UUID tenantId, Stage stage);
}
