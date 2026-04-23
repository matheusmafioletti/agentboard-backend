package com.agentboard.auth.repository;

import com.agentboard.auth.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for {@link UserAccount} entities. */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

  /** Returns the user with the given email, if one exists. */
  Optional<UserAccount> findByEmail(String email);

  /** Returns {@code true} if a user with the given email already exists. */
  boolean existsByEmail(String email);
}
