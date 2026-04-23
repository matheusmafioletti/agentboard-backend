package com.agentboard.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates JJWT-signed tokens carrying {@code userId}, {@code tenantId}, and {@code roles}
 * claims.
 */
@Service
public class JwtTokenService {

  private final SecretKey secretKey;
  private final long expirationMs;

  /**
   * Creates the service, reading the signing secret and expiration window from application
   * properties.
   */
  public JwtTokenService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration-ms}") long expirationMs) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  /**
   * Generates a compact JWT for the given principal.
   *
   * @param userId   the user's unique identifier, used as the JWT subject
   * @param tenantId the tenant the user belongs to
   * @param roles    the list of role names to embed as a claim
   * @return a signed, compact JWT string
   */
  public String generate(UUID userId, UUID tenantId, List<String> roles) {
    return Jwts.builder()
        .subject(userId.toString())
        .claim("tenantId", tenantId.toString())
        .claim("roles", roles)
        .expiration(new Date(System.currentTimeMillis() + expirationMs))
        .signWith(secretKey)
        .compact();
  }
}
