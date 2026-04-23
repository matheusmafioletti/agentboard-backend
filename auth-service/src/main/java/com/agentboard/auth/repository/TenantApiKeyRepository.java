package com.agentboard.auth.repository;

import com.agentboard.auth.domain.TenantApiKey;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for {@link TenantApiKey} entities. */
@Repository
public interface TenantApiKeyRepository extends JpaRepository<TenantApiKey, UUID> {
}
