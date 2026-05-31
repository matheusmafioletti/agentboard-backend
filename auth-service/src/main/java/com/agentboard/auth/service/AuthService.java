package com.agentboard.auth.service;

import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantApiKey;
import com.agentboard.auth.domain.TenantMembership;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.ChangePasswordRequest;
import com.agentboard.auth.dto.LoginRequest;
import com.agentboard.auth.dto.RegisterRequest;
import com.agentboard.auth.dto.RegisterResponse;
import com.agentboard.auth.dto.SelectTenantRequest;
import com.agentboard.auth.dto.SessionResponse;
import com.agentboard.auth.dto.TenantSelectionResponse;
import com.agentboard.auth.exception.DuplicateEmailException;
import com.agentboard.auth.exception.DuplicateTenantNameException;
import com.agentboard.auth.exception.InvalidCredentialsException;
import com.agentboard.auth.exception.NoMembershipException;
import com.agentboard.auth.exception.NotMemberException;
import com.agentboard.auth.repository.TenantApiKeyRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles registration, login, and password changes. */
@Service
public class AuthService {

  private final TenantRepository tenantRepository;
  private final UserAccountRepository userAccountRepository;
  private final TenantApiKeyRepository tenantApiKeyRepository;
  private final PasswordEncoder passwordEncoder;
  private final MembershipService membershipService;
  private final SessionFactory sessionFactory;

  /**
   * Creates the service with all required collaborators.
   */
  public AuthService(
      TenantRepository tenantRepository,
      UserAccountRepository userAccountRepository,
      TenantApiKeyRepository tenantApiKeyRepository,
      PasswordEncoder passwordEncoder,
      MembershipService membershipService,
      SessionFactory sessionFactory) {
    this.tenantRepository = tenantRepository;
    this.userAccountRepository = userAccountRepository;
    this.tenantApiKeyRepository = tenantApiKeyRepository;
    this.passwordEncoder = passwordEncoder;
    this.membershipService = membershipService;
    this.sessionFactory = sessionFactory;
  }

  /**
   * Registers a new global user, tenant, and admin membership.
   *
   * @throws DuplicateEmailException if the email is already in use
   * @throws DuplicateTenantNameException if the tenant name is taken
   */
  @Transactional
  public RegisterResponse register(RegisterRequest request) {
    if (userAccountRepository.existsByEmail(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    if (tenantRepository.existsByName(request.tenantName())) {
      throw new DuplicateTenantNameException(request.tenantName());
    }

    Tenant tenant = tenantRepository.save(new Tenant(request.tenantName()));
    String passwordHash = passwordEncoder.encode(request.password());
    UserAccount user = userAccountRepository.save(
        new UserAccount(request.name(), request.email(), passwordHash));
    TenantMembership membership = membershipService.createAdminMembership(
        user.getId(), tenant.getId());

    String rawApiKey = UUID.randomUUID().toString();
    tenantApiKeyRepository.save(new TenantApiKey(tenant.getId(), sha256Hex(rawApiKey)));

    SessionResponse session = sessionFactory.buildSession(user, tenant, membership);

    return new RegisterResponse(
        user.getId(),
        tenant.getId(),
        tenant.getName(),
        session.token(),
        MembershipRole.ADMIN,
        rawApiKey);
  }

  /**
   * Authenticates credentials and returns either a session or tenant selection list.
   */
  public Object login(LoginRequest request) {
    UserAccount user = authenticate(request.email(), request.password());
    List<com.agentboard.auth.dto.TenantMembershipSummary> memberships =
        membershipService.listByUserId(user.getId());

    if (memberships.isEmpty()) {
      throw new NoMembershipException();
    }
    if (memberships.size() == 1) {
      return buildSessionForMembership(user, memberships.getFirst());
    }
    return new TenantSelectionResponse(
        user.getId(), user.getEmail(), user.getName(), memberships);
  }

  /**
   * Switches the active workspace for an authenticated user without re-entering credentials.
   */
  public SessionResponse switchTenant(UUID userId, UUID tenantId) {
    UserAccount user = userAccountRepository.findById(userId)
        .orElseThrow(InvalidCredentialsException::new);
    TenantMembership membership = membershipService.getMembership(userId, tenantId);
    Tenant tenant = tenantRepository.findById(membership.getTenantId()).orElseThrow();
    return sessionFactory.buildSession(user, tenant, membership);
  }

  /**
   * Completes login by issuing a JWT for the selected tenant.
   */
  public SessionResponse selectTenant(SelectTenantRequest request) {
    UserAccount user = authenticate(request.email(), request.password());
    TenantMembership membership = membershipService.getMembership(
        user.getId(), request.tenantId());
    Tenant tenant = tenantRepository.findById(membership.getTenantId()).orElseThrow();
    return sessionFactory.buildSession(user, tenant, membership);
  }

  private SessionResponse buildSessionForMembership(
      UserAccount user,
      com.agentboard.auth.dto.TenantMembershipSummary summary) {
    Tenant tenant = tenantRepository.findById(summary.tenantId()).orElseThrow();
    TenantMembership membership = membershipService.getMembership(user.getId(), tenant.getId());
    return sessionFactory.buildSession(user, tenant, membership);
  }

  /**
   * Changes the password for the user identified by {@code request.userId()}.
   */
  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    if (!request.newPassword().equals(request.confirmNewPassword())) {
      throw new IllegalArgumentException("newPassword and confirmNewPassword do not match");
    }

    UserAccount user = userAccountRepository.findById(request.userId())
        .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
    userAccountRepository.save(user);
  }

  private UserAccount authenticate(String email, String password) {
    UserAccount user = userAccountRepository.findByEmail(email)
        .orElseThrow(InvalidCredentialsException::new);
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }
    return user;
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
