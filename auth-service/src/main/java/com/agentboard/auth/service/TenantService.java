package com.agentboard.auth.service;

import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantApiKey;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.CreateTenantResponse;
import com.agentboard.auth.dto.SessionResponse;
import com.agentboard.auth.exception.DuplicateTenantNameException;
import com.agentboard.auth.repository.TenantApiKeyRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates additional workspaces for authenticated users. */
@Service
public class TenantService {

  private final TenantRepository tenantRepository;
  private final UserAccountRepository userAccountRepository;
  private final TenantApiKeyRepository tenantApiKeyRepository;
  private final MembershipService membershipService;
  private final SessionFactory sessionFactory;

  /**
   * Creates the service with required collaborators.
   */
  public TenantService(
      TenantRepository tenantRepository,
      UserAccountRepository userAccountRepository,
      TenantApiKeyRepository tenantApiKeyRepository,
      MembershipService membershipService,
      SessionFactory sessionFactory) {
    this.tenantRepository = tenantRepository;
    this.userAccountRepository = userAccountRepository;
    this.tenantApiKeyRepository = tenantApiKeyRepository;
    this.membershipService = membershipService;
    this.sessionFactory = sessionFactory;
  }

  /**
   * Creates a new tenant for the user and switches the session to it.
   *
   * @throws DuplicateTenantNameException if the tenant name is taken
   */
  @Transactional
  public CreateTenantResponse createTenantForUser(UUID userId, String tenantName) {
    if (tenantRepository.existsByName(tenantName)) {
      throw new DuplicateTenantNameException(tenantName);
    }

    UserAccount user = userAccountRepository.findById(userId).orElseThrow();
    Tenant tenant = tenantRepository.save(new Tenant(tenantName));
    var membership = membershipService.createAdminMembership(user.getId(), tenant.getId());

    String rawApiKey = UUID.randomUUID().toString();
    tenantApiKeyRepository.save(new TenantApiKey(tenant.getId(), sha256Hex(rawApiKey)));

    SessionResponse session = sessionFactory.buildSession(user, tenant, membership);

    return new CreateTenantResponse(session, rawApiKey);
  }

  private static String sha256Hex(String input) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
