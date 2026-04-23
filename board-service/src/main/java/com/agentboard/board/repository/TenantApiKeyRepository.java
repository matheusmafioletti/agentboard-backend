package com.agentboard.board.repository;

import com.agentboard.board.domain.TenantApiKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Read-only data access for tenant API keys, used by
 * {@link com.agentboard.board.security.ApiKeyFilter}.
 */
@Repository
public interface TenantApiKeyRepository extends JpaRepository<TenantApiKey, UUID> {

  /**
   * Finds an active (non-revoked) API key record by its SHA-256 hash.
   */
  Optional<TenantApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);
}
