package com.agentboard.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.commons.security.JwtValidator;
import com.agentboard.commons.security.ParsedToken;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

  private static final String SECRET =
      "test-secret-key-that-is-long-enough-for-hmac-sha256-minimum-32b";
  private static final long EXPIRATION_MS = 3_600_000L;

  private JwtTokenService jwtTokenService;
  private JwtValidator jwtValidator;

  @BeforeEach
  void setUp() {
    jwtTokenService = new JwtTokenService(SECRET, EXPIRATION_MS);
    jwtValidator = new JwtValidator(SECRET);
  }

  @Test
  void shouldGenerateValidToken_whenGenerateWithRolesList() {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();

    String token = jwtTokenService.generate(userId, tenantId, List.of("ADMIN"));

    assertThat(token).isNotBlank();
    ParsedToken parsed = jwtValidator.validate(token);
    assertThat(parsed.userId()).isEqualTo(userId);
    assertThat(parsed.tenantId()).isEqualTo(tenantId);
    assertThat(parsed.roles()).containsExactly("ADMIN");
  }

  @Test
  void shouldGenerateValidToken_whenGenerateWithMembershipRole() {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();

    String token = jwtTokenService.generate(userId, tenantId, MembershipRole.USER);

    assertThat(token).isNotBlank();
    ParsedToken parsed = jwtValidator.validate(token);
    assertThat(parsed.userId()).isEqualTo(userId);
    assertThat(parsed.tenantId()).isEqualTo(tenantId);
    assertThat(parsed.roles()).containsExactly("USER");
  }

  @Test
  void shouldGenerateTokenWithMultipleRoles_whenGenerateWithRolesList() {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();

    String token = jwtTokenService.generate(userId, tenantId, List.of("ADMIN", "USER"));

    assertThat(token).isNotBlank();
    ParsedToken parsed = jwtValidator.validate(token);
    assertThat(parsed.roles()).containsExactly("ADMIN", "USER");
  }
}
