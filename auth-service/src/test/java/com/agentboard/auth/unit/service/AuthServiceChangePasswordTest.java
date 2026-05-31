package com.agentboard.auth.unit.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.ChangePasswordRequest;
import com.agentboard.auth.exception.InvalidCredentialsException;
import com.agentboard.auth.repository.TenantApiKeyRepository;
import com.agentboard.auth.repository.TenantRepository;
import com.agentboard.auth.repository.UserAccountRepository;
import com.agentboard.auth.service.AuthService;
import com.agentboard.auth.service.MembershipService;
import com.agentboard.auth.service.SessionFactory;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for {@link AuthService#changePassword}. */
@ExtendWith(MockitoExtension.class)
class AuthServiceChangePasswordTest {

  @Mock private TenantRepository tenantRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private TenantApiKeyRepository tenantApiKeyRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private MembershipService membershipService;
  @Mock private SessionFactory sessionFactory;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(
        tenantRepository,
        userAccountRepository,
        tenantApiKeyRepository,
        passwordEncoder,
        membershipService,
        sessionFactory);
  }

  @Test
  void changePassword_correctCurrentPassword_updatesHash() {
    UUID userId = UUID.randomUUID();
    UserAccount user = new UserAccount("Test User", "user@example.com", "hashedOld");
    ChangePasswordRequest request = new ChangePasswordRequest(
        userId, "currentPass", "newPass123", "newPass123");

    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("currentPass", "hashedOld")).thenReturn(true);
    when(passwordEncoder.encode("newPass123")).thenReturn("hashedNew");

    authService.changePassword(request);

    verify(userAccountRepository).save(user);
    verify(passwordEncoder).encode("newPass123");
  }

  @Test
  void changePassword_wrongCurrentPassword_throwsInvalidCredentials() {
    UUID userId = UUID.randomUUID();
    UserAccount user = new UserAccount("Test User", "user@example.com", "hashedOld");
    ChangePasswordRequest request = new ChangePasswordRequest(
        userId, "wrongPass", "newPass123", "newPass123");

    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongPass", "hashedOld")).thenReturn(false);

    assertThrows(InvalidCredentialsException.class, () -> authService.changePassword(request));
    verify(userAccountRepository, never()).save(any());
  }

  @Test
  void changePassword_passwordMismatch_throwsIllegalArgumentException() {
    UUID userId = UUID.randomUUID();
    ChangePasswordRequest request = new ChangePasswordRequest(
        userId, "currentPass", "newPass123", "differentPass");

    assertThrows(IllegalArgumentException.class, () -> authService.changePassword(request));
    verify(userAccountRepository, never()).save(any());
  }

  @Test
  void changePassword_unknownUser_throwsInvalidCredentials() {
    UUID userId = UUID.randomUUID();
    ChangePasswordRequest request = new ChangePasswordRequest(
        userId, "currentPass", "newPass123", "newPass123");

    when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

    assertThrows(InvalidCredentialsException.class, () -> authService.changePassword(request));
  }
}
