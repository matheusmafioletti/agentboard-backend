package com.agentboard.auth.repository;

import com.agentboard.auth.domain.Tenant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for {@link Tenant} entities. */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

  /** Returns {@code true} if a tenant with the given name already exists. */
  boolean existsByName(String name);
}
