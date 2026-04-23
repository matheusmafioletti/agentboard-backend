package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentboard.commons.security.InvalidTokenException;
import com.agentboard.commons.security.JwtValidator;
import com.agentboard.commons.security.ParsedToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JwtValidator} covering valid tokens, expiry, tampering,
 * and missing claims.
 */
class JwtValidatorTest {

  private static final String secret =
      "test-secret-key-that-is-long-enough-for-hmac-sha256-min-32b";

  private JwtValidator validator;

  @BeforeEach
  void setUp() {
    validator = new JwtValidator(secret);
  }

  @Test
  void validTokenReturnsParsedToken() {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    String token = buildToken(userId, tenantId, List.of("USER"),
        System.currentTimeMillis() + 3_600_000L);
    ParsedToken result = validator.validate(token);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.tenantId()).isEqualTo(tenantId);
    assertThat(result.roles()).containsExactly("USER");
  }

  @Test
  void expiredTokenThrowsInvalidTokenException() {
    String token = buildToken(UUID.randomUUID(), UUID.randomUUID(), List.of("USER"),
        System.currentTimeMillis() - 1_000L);
    assertThatThrownBy(() -> validator.validate(token))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void tamperedSignatureThrowsInvalidTokenException() {
    String differentSecret =
        "different-secret-key-that-is-long-enough-for-hmac-sha-256-32b";
    String tamperedToken = buildTokenWithSecret(UUID.randomUUID(), UUID.randomUUID(),
        List.of("USER"), System.currentTimeMillis() + 3_600_000L, differentSecret);
    assertThatThrownBy(() -> validator.validate(tamperedToken))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void missingTenantIdClaimThrowsInvalidTokenException() {
    String token = Jwts.builder()
        .subject(UUID.randomUUID().toString())
        .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
        .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
        .compact();
    assertThatThrownBy(() -> validator.validate(token))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void missingSubjectClaimThrowsInvalidTokenException() {
    String token = Jwts.builder()
        .claim("tenantId", UUID.randomUUID().toString())
        .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
        .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
        .compact();
    assertThatThrownBy(() -> validator.validate(token))
        .isInstanceOf(InvalidTokenException.class);
  }

  private String buildToken(
      UUID userId, UUID tenantId, List<String> roles, long expirationMs) {
    return buildTokenWithSecret(userId, tenantId, roles, expirationMs, secret);
  }

  private String buildTokenWithSecret(
      UUID userId, UUID tenantId, List<String> roles, long expirationMs, String signingSecret) {
    return Jwts.builder()
        .subject(userId.toString())
        .claim("tenantId", tenantId.toString())
        .claim("roles", roles)
        .expiration(new Date(expirationMs))
        .signWith(Keys.hmacShaKeyFor(signingSecret.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }
}
