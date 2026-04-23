package com.agentboard.board.repository;

import com.agentboard.board.domain.Board;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for {@link Board} entities. */
@Repository
public interface BoardRepository extends JpaRepository<Board, UUID> {

  /** Returns the board belonging to the given tenant, if one exists. */
  Optional<Board> findByTenantId(UUID tenantId);
}
