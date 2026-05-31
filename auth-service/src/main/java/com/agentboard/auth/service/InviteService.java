package com.agentboard.auth.service;

import com.agentboard.auth.domain.InviteStatus;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantInvite;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.AcceptInviteRequest;
import com.agentboard.auth.dto.IdentifyInviteResponse;
import com.agentboard.auth.dto.InvitePreviewResponse;
import com.agentboard.auth.dto.InviteResponse;
import com.agentboard.auth.dto.SessionResponse;
import com.agentboard.auth.dto.VerifyInviteCredentialsResponse;
import com.agentboard.auth.exception.AlreadyMemberException;
import com.agentboard.auth.exception.DuplicatePendingInviteException;
import com.agentboard.auth.exception.ForbiddenOperationException;
import com.agentboard.auth.exception.InviteGoneException;
import com.agentboard.auth.exception.InvalidCredentialsException;
import com.agentboard.auth.repository.TenantInviteRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates, validates, and accepts tenant invitations. */
@Service
public class InviteService {

  private static final int INVITE_VALIDITY_DAYS = 7;

  private final TenantInviteRepository inviteRepository;
  private final TenantRepository tenantRepository;
  private final UserAccountRepository userAccountRepository;
  private final MembershipService membershipService;
  private final SessionFactory sessionFactory;
  private final PasswordEncoder passwordEncoder;
  private final String inviteBaseUrl;

  /**
   * Creates the invite service.
   */
  public InviteService(
      TenantInviteRepository inviteRepository,
      TenantRepository tenantRepository,
      UserAccountRepository userAccountRepository,
      MembershipService membershipService,
      SessionFactory sessionFactory,
      PasswordEncoder passwordEncoder,
      @Value("${app.invite-base-url:http://localhost:5173}") String inviteBaseUrl) {
    this.inviteRepository = inviteRepository;
    this.tenantRepository = tenantRepository;
    this.userAccountRepository = userAccountRepository;
    this.membershipService = membershipService;
    this.sessionFactory = sessionFactory;
    this.passwordEncoder = passwordEncoder;
    this.inviteBaseUrl = inviteBaseUrl.endsWith("/")
        ? inviteBaseUrl.substring(0, inviteBaseUrl.length() - 1)
        : inviteBaseUrl;
  }

  /** Creates a pending invite and returns the response including the one-time URL. */
  @Transactional
  public InviteResponse createInvite(UUID tenantId, UUID invitedBy, String email) {
    String normalizedEmail = email.trim().toLowerCase();

    userAccountRepository.findByEmail(normalizedEmail).ifPresent(user -> {
      if (membershipService.isMember(user.getId(), tenantId)) {
        throw new AlreadyMemberException();
      }
    });

    if (inviteRepository.findByTenantIdAndEmailAndStatus(
        tenantId, normalizedEmail, InviteStatus.PENDING).isPresent()) {
      throw new DuplicatePendingInviteException();
    }

    String rawToken = UUID.randomUUID().toString();
    String tokenHash = sha256Hex(rawToken);
    OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(INVITE_VALIDITY_DAYS);

    TenantInvite invite = inviteRepository.save(new TenantInvite(
        tenantId, normalizedEmail, tokenHash, invitedBy, expiresAt));

    return toResponse(invite, rawToken);
  }

  /** Lists invites for a tenant (admin). */
  public List<InviteResponse> listInvites(UUID tenantId) {
    return inviteRepository.findByTenantId(tenantId).stream()
        .map(inv -> toResponse(resolveExpiry(inv), null))
        .toList();
  }

  /** Cancels a pending invite. */
  @Transactional
  public void cancelInvite(UUID tenantId, UUID inviteId) {
    TenantInvite invite = inviteRepository.findById(inviteId)
        .filter(i -> i.getTenantId().equals(tenantId))
        .orElseThrow(() -> new InviteGoneException("Invite not found"));
    if (invite.getStatus() != InviteStatus.PENDING) {
      throw new InviteGoneException("Invite is no longer pending");
    }
    invite.markCancelled();
    inviteRepository.save(invite);
  }

  /** Validates the invite email and reports whether a global account already exists. */
  public IdentifyInviteResponse identifyInvite(String rawToken, String email) {
    TenantInvite invite = findActiveInvite(rawToken);
    Tenant tenant = tenantRepository.findById(invite.getTenantId()).orElseThrow();
    String normalizedEmail = normalizeEmail(email);
    assertEmailMatchesInvite(invite, normalizedEmail);
    boolean accountExists = userAccountRepository.findByEmail(normalizedEmail).isPresent();
    return new IdentifyInviteResponse(tenant.getName(), invite.getEmail(), accountExists);
  }

  /** Verifies credentials for an existing user without accepting the invite. */
  public VerifyInviteCredentialsResponse verifyCredentials(
      String rawToken, String email, String password) {
    TenantInvite invite = findActiveInvite(rawToken);
    String normalizedEmail = normalizeEmail(email);
    assertEmailMatchesInvite(invite, normalizedEmail);

    UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
        .orElseThrow(InvalidCredentialsException::new);
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }
    return new VerifyInviteCredentialsResponse(user.getName(), user.getEmail());
  }

  /** Returns a public preview for the raw token. */
  public InvitePreviewResponse getInvitePreview(String rawToken) {
    TenantInvite invite = findActiveInvite(rawToken);
    Tenant tenant = tenantRepository.findById(invite.getTenantId()).orElseThrow();
    boolean requiresRegistration = userAccountRepository.findByEmail(invite.getEmail()).isEmpty();
    return new InvitePreviewResponse(
        tenant.getName(),
        invite.getEmail(),
        invite.getStatus(),
        invite.getExpiresAt(),
        requiresRegistration);
  }

  /**
   * Accepts an invite for a new or existing user.
   */
  @Transactional
  public SessionResponse acceptInvite(String rawToken, AcceptInviteRequest request) {
    TenantInvite invite = findActiveInvite(rawToken);
    Tenant tenant = tenantRepository.findById(invite.getTenantId()).orElseThrow();

    UserAccount user = resolveUserForAccept(invite, request);

    if (membershipService.isMember(user.getId(), tenant.getId())) {
      throw new AlreadyMemberException();
    }

    membershipService.createUserMembership(user.getId(), tenant.getId());
    invite.markAccepted();
    inviteRepository.save(invite);

    var membership = membershipService.getMembership(user.getId(), tenant.getId());
    return sessionFactory.buildSession(user, tenant, membership);
  }

  private UserAccount resolveUserForAccept(TenantInvite invite, AcceptInviteRequest request) {
    var existing = userAccountRepository.findByEmail(invite.getEmail());

    if (existing.isEmpty()) {
      if (request.name() == null || request.name().isBlank()
          || request.password() == null || request.password().isBlank()) {
        throw new IllegalArgumentException("name and password are required for new users");
      }
      return userAccountRepository.save(new UserAccount(
          request.name(),
          invite.getEmail(),
          passwordEncoder.encode(request.password())));
    }

    if (request.email() == null || request.password() == null) {
      throw new IllegalArgumentException("email and password are required for existing users");
    }
    String normalizedEmail = normalizeEmail(request.email());
    assertEmailMatchesInvite(invite, normalizedEmail);

    UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
        .orElseThrow(InvalidCredentialsException::new);
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }
    return user;
  }

  private TenantInvite findActiveInvite(String rawToken) {
    TenantInvite invite = inviteRepository.findByTokenHash(sha256Hex(rawToken))
        .orElseThrow(() -> new InviteGoneException("Invalid invite token"));
    invite = resolveExpiry(invite);
    if (invite.getStatus() != InviteStatus.PENDING) {
      throw new InviteGoneException("Invite is no longer valid");
    }
    return invite;
  }

  private TenantInvite resolveExpiry(TenantInvite invite) {
    if (invite.isExpiredPending()) {
      invite.markExpired();
      inviteRepository.save(invite);
    }
    return invite;
  }

  private InviteResponse toResponse(TenantInvite invite, String rawToken) {
    String inviteUrl = rawToken != null
        ? inviteBaseUrl + "/invite/" + rawToken
        : null;
    return new InviteResponse(
        invite.getId(),
        invite.getEmail(),
        invite.getStatus(),
        invite.getCreatedAt(),
        invite.getExpiresAt(),
        inviteUrl);
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }

  private static void assertEmailMatchesInvite(TenantInvite invite, String normalizedEmail) {
    if (!invite.getEmail().equals(normalizedEmail)) {
      throw new ForbiddenOperationException("Email does not match the invitation");
    }
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
