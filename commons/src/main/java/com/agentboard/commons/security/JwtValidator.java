package com.agentboard.commons.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;

/**
 * Validates JJWT-signed tokens and extracts the {@code userId}, {@code tenantId}, and
 * {@code roles} claims.
 *
 * <p>All parse failures — expired tokens, tampered signatures, and missing required claims —
 * result in {@link InvalidTokenException}.
 */
public class JwtValidator {

  private final SecretKey secretKey;

  /**
   * Creates a validator that verifies tokens signed with the given HMAC-SHA secret.
   */
  public JwtValidator(String secret) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Parses and validates the given JWT string, returning the extracted principal data.
   *
   * @param token the compact JWT string to validate
   * @return the parsed token data
   * @throws InvalidTokenException if the token is expired, tampered, or missing required claims
   */
  public ParsedToken validate(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      UUID userId = extractUserId(claims);
      UUID tenantId = extractTenantId(claims);
      List<String> roles = extractRoles(claims);
      return new ParsedToken(userId, tenantId, roles);
    } catch (JwtException | IllegalArgumentException e) {
      throw new InvalidTokenException("Invalid or expired JWT token", e);
    }
  }

  private UUID extractUserId(Claims claims) {
    String subject = claims.getSubject();
    if (subject == null) {
      throw new InvalidTokenException("Token missing subject claim");
    }
    return UUID.fromString(subject);
  }

  private UUID extractTenantId(Claims claims) {
    String tenantIdStr = claims.get("tenantId", String.class);
    if (tenantIdStr == null) {
      throw new InvalidTokenException("Token missing tenantId claim");
    }
    return UUID.fromString(tenantIdStr);
  }

  private List<String> extractRoles(Claims claims) {
    Object rolesObj = claims.get("roles");
    return rolesObj instanceof List<?> rawList
        ? rawList.stream().map(Object::toString).toList()
        : List.of();
  }
}
