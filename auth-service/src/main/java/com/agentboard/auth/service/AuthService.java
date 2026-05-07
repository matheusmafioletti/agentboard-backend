package com.agentboard.auth.service;

import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantApiKey;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.BoardInfo;
import com.agentboard.auth.dto.ChangePasswordRequest;
import com.agentboard.auth.dto.LoginRequest;
import com.agentboard.auth.dto.LoginResponse;
import com.agentboard.auth.dto.RegisterRequest;
import com.agentboard.auth.dto.RegisterResponse;
import com.agentboard.auth.exception.DuplicateEmailException;
import com.agentboard.auth.exception.InvalidCredentialsException;
import com.agentboard.auth.repository.TenantApiKeyRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import com.agentboard.auth.security.JwtTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles tenant registration and user authentication. */
@Service
public class AuthService {

  private final TenantRepository tenantRepository;
  private final UserAccountRepository userAccountRepository;
  private final TenantApiKeyRepository tenantApiKeyRepository;
  private final JwtTokenService jwtTokenService;
  private final PasswordEncoder passwordEncoder;
  private final BoardServiceClient boardServiceClient;

  /**
   * Creates the service with all required collaborators.
   */
  public AuthService(
      TenantRepository tenantRepository,
      UserAccountRepository userAccountRepository,
      TenantApiKeyRepository tenantApiKeyRepository,
      JwtTokenService jwtTokenService,
      PasswordEncoder passwordEncoder,
      BoardServiceClient boardServiceClient) {
    this.tenantRepository = tenantRepository;
    this.userAccountRepository = userAccountRepository;
    this.tenantApiKeyRepository = tenantApiKeyRepository;
    this.jwtTokenService = jwtTokenService;
    this.passwordEncoder = passwordEncoder;
    this.boardServiceClient = boardServiceClient;
  }

  /**
   * Registers a new tenant and user, generating a JWT and an API key.
   *
   * @throws DuplicateEmailException if the email is already in use
   */
  @Transactional
  public RegisterResponse register(RegisterRequest request) {
    if (userAccountRepository.existsByEmail(request.email())) {
      throw new DuplicateEmailException(request.email());
    }

    Tenant tenant = tenantRepository.save(new Tenant(request.tenantName()));
    String passwordHash = passwordEncoder.encode(request.password());
    UserAccount user = userAccountRepository.save(
        new UserAccount(tenant.getId(), request.email(), passwordHash));

    String rawApiKey = UUID.randomUUID().toString();
    String keyHash = sha256Hex(rawApiKey);
    tenantApiKeyRepository.save(new TenantApiKey(tenant.getId(), keyHash));

    String token = jwtTokenService.generate(user.getId(), tenant.getId(), user.getRoles());
    BoardInfo board = boardServiceClient.createBoard(
        tenant.getId(), request.tenantName() + " Board");

    return new RegisterResponse(user.getId(), tenant.getId(), token, rawApiKey, board);
  }

  /**
   * Authenticates a user with email and password, returning a JWT on success.
   *
   * @throws InvalidCredentialsException if the credentials do not match any active account
   */
  public LoginResponse login(LoginRequest request) {
    UserAccount user = userAccountRepository.findByEmail(request.email())
        .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    String token = jwtTokenService.generate(user.getId(), user.getTenantId(), user.getRoles());
    return new LoginResponse(token, user.getId(), user.getTenantId(), user.getEmail());
  }

  /**
   * Changes the password for the user identified by {@code request.userId()}.
   *
   * @throws InvalidCredentialsException if the user is not found or currentPassword is wrong
   * @throws IllegalArgumentException if newPassword and confirmNewPassword do not match
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
