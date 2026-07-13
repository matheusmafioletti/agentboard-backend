package com.agentboard.auth.repository;

import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.UserAccount;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for auth-service repository slice tests.
 *
 * <p>Runs against a Flyway-migrated PostgreSQL 16 TestContainer instead of an embedded
 * database so that partial unique indexes and FK constraints behave exactly as in production.
 * Each test method is transactional and rolled back by {@link DataJpaTest}.
 *
 * <p>NOTE: the container is started once per JVM (singleton pattern) instead of using
 * {@code @Testcontainers}, because the cached slice context is shared by several test classes
 * and a per-class container restart would leave it pointing at a dead database.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractRepositoryTest {

  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("agentboard")
          .withUsername("agentboard")
          .withPassword("agentboard");

  static {
    postgres.start();
  }

  @Autowired
  TenantRepository tenantRepository;

  @Autowired
  UserAccountRepository userAccountRepository;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  /**
   * Persists a tenant row so dependent rows satisfy the {@code tenant_id} FK.
   *
   * @return the persisted tenant
   */
  protected Tenant persistTenant() {
    return tenantRepository.save(new Tenant("Tenant " + UUID.randomUUID()));
  }

  /**
   * Persists a user row so dependent rows satisfy the {@code user_id}/{@code invited_by} FKs.
   *
   * @return the persisted user
   */
  protected UserAccount persistUser() {
    return userAccountRepository.save(new UserAccount(
        "Repo User", "repo-" + UUID.randomUUID() + "@example.com", "irrelevant-hash"));
  }
}
