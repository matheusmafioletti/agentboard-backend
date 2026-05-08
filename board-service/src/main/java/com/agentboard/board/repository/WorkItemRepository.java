package com.agentboard.board.repository;

import com.agentboard.board.domain.WorkItem;
import com.agentboard.commons.domain.WorkItemType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link WorkItem} entities.
 *
 * <p>All queries are scoped by tenantId or (projectId, tenantId) to enforce row-level
 * multi-tenancy. Cross-tenant access is not permitted.
 */
public interface WorkItemRepository extends JpaRepository<WorkItem, UUID> {

  /**
   * Returns all work items for a project, scoped by tenant.
   *
   * @param projectId the owning project
   * @param tenantId  the owning tenant
   * @return unfiltered list ordered by display_order
   */
  List<WorkItem> findAllByProjectIdAndTenantId(UUID projectId, UUID tenantId);

  /**
   * Returns all work items of a given type for a project.
   *
   * @param projectId the owning project
   * @param type      the work item type filter
   * @return list of matching items
   */
  List<WorkItem> findAllByProjectIdAndType(UUID projectId, WorkItemType type);

  /**
   * Returns all work items of a given type under a specific parent.
   *
   * @param projectId the owning project
   * @param type      the work item type filter
   * @param parentId  the parent work item
   * @return list of children
   */
  List<WorkItem> findAllByProjectIdAndTypeAndParentId(UUID projectId, WorkItemType type,
      UUID parentId);

  /**
   * Returns all work items of a given type in a given status.
   *
   * @param projectId the owning project
   * @param type      the work item type filter
   * @param status    the status filter
   * @return list of matching items
   */
  List<WorkItem> findAllByProjectIdAndTypeAndStatus(UUID projectId, WorkItemType type,
      String status);

  /**
   * Returns all direct children of a parent item with a given type.
   *
   * @param parentId the parent work item identifier
   * @param type     the child type
   * @return list of children
   */
  List<WorkItem> findByParentIdAndType(UUID parentId, WorkItemType type);

  /**
   * Returns children ordered deterministically within the board column semantics.
   *
   * @param parentId the parent work item identifier
   * @return list of all direct children
   */
  @Query("SELECT wi FROM WorkItem wi WHERE wi.parentId = :parentId "
      + "ORDER BY wi.displayOrder ASC, wi.createdAt ASC")
  List<WorkItem> findAllByParentId(@Param("parentId") UUID parentId);

  /**
   * Loads many items by tenant so parent silhouettes hydrate without repeated round-trips.
   *
   * @param tenantId owning tenant boundary
   * @param ids      identifiers excluding null collections
   * @return persisted rows respecting tenant isolation
   */
  @Query("SELECT wi FROM WorkItem wi WHERE wi.tenantId = :tenantId AND wi.id IN :ids")
  List<WorkItem> findAllByTenantIdAndIdIn(
      @Param("tenantId") UUID tenantId,
      @Param("ids") Collection<UUID> ids);

  /**
   * Counts sibling work items with a given type whose status is NOT the given value.
   *
   * @param parentId the shared parent
   * @param status   the status to exclude
   * @return count of items whose status differs
   */
  long countByParentIdAndStatusNot(UUID parentId, String status);

  /**
   * Counts sibling work items with the given status under a parent.
   *
   * @param parentId the shared parent
   * @param status   the status to match
   * @return count of items in the given status
   */
  long countByParentIdAndStatus(UUID parentId, String status);

  /**
   * Returns the highest numeric suffix among display keys that match the F/U/T pattern for a
   * given project and type, scoped by tenant. Returns 0 when no matching rows exist.
   *
   * <p>IMPORTANT: Uses a native PostgreSQL query because JPQL cannot cast and slice strings
   * with the precision needed for the {@code ^[FUT][1-9][0-9]*$} pattern.
   *
   * @param projectId the owning project
   * @param tenantId  the owning tenant
   * @param type      the work item type name (e.g. {@code "FEATURE"})
   * @return the current maximum sequence number, or 0 if none exists
   */
  @Query(
      value =
          "SELECT COALESCE(MAX(CAST(SUBSTRING(display_key FROM 2) AS INTEGER)), 0) "
              + "FROM work_item "
              + "WHERE project_id = :projectId "
              + "  AND tenant_id  = :tenantId "
              + "  AND type       = :type "
              + "  AND display_key ~ '^[FUT][1-9][0-9]*$'",
      nativeQuery = true)
  int findMaxDisplayKeySeq(
      @Param("projectId") UUID projectId,
      @Param("tenantId") UUID tenantId,
      @Param("type") String type);

  /**
   * Returns a work item by id scoped to a tenant.
   *
   * @param id       the work item identifier
   * @param tenantId the owning tenant
   * @return the item, or empty if not found or tenant mismatch
   */
  Optional<WorkItem> findByIdAndTenantId(UUID id, UUID tenantId);

  /**
   * Returns all work items matching optional type, parentId, status, and assigneeId filters.
   *
   * <p>Null parameters are treated as "no filter" for that dimension.
   *
   * @param projectId  the owning project
   * @param tenantId   the owning tenant
   * @param type       optional type filter
   * @param parentId   optional parent filter
   * @param status     optional status filter
   * @param assigneeId optional assignee filter
   * @return filtered list of work items
   */
  @Query("SELECT wi FROM WorkItem wi WHERE wi.projectId = :projectId "
      + "AND wi.tenantId = :tenantId "
      + "AND (:type IS NULL OR wi.type = :type) "
      + "AND (:parentId IS NULL OR wi.parentId = :parentId) "
      + "AND (:status IS NULL OR wi.status = :status) "
      + "AND (:assigneeId IS NULL OR wi.assigneeId = :assigneeId) "
      + "ORDER BY wi.displayOrder ASC, wi.createdAt ASC")
  List<WorkItem> findFiltered(
      @Param("projectId") UUID projectId,
      @Param("tenantId") UUID tenantId,
      @Param("type") WorkItemType type,
      @Param("parentId") UUID parentId,
      @Param("status") String status,
      @Param("assigneeId") UUID assigneeId);
}
