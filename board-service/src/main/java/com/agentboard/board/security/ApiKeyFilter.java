package com.agentboard.board.security;

import com.agentboard.board.domain.TenantApiKey;
import com.agentboard.board.repository.TenantApiKeyRepository;
import com.agentboard.commons.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the tenant from the {@code X-API-Key} header, sets {@link TenantContext}, and
 * populates {@link SecurityContextHolder} so that Spring Security sees the request as
 * authenticated.
 *
 * <p>Returns 401 when the header is present but the key is not found, and 403 when the key
 * is revoked.
 */
public class ApiKeyFilter extends OncePerRequestFilter {

  private final TenantApiKeyRepository tenantApiKeyRepository;

  /**
   * Creates the filter backed by the given repository.
   */
  public ApiKeyFilter(TenantApiKeyRepository tenantApiKeyRepository) {
    this.tenantApiKeyRepository = tenantApiKeyRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {

    String rawKey = request.getHeader("X-API-Key");
    if (rawKey == null) {
      chain.doFilter(request, response);
      return;
    }

    String keyHash = sha256Hex(rawKey);
    Optional<TenantApiKey> keyRecord =
        tenantApiKeyRepository.findByKeyHashAndRevokedAtIsNull(keyHash);

    if (keyRecord.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write(
          "{\"error\":\"INVALID_API_KEY\",\"message\":\"Invalid or missing API key\"}");
      return;
    }

    TenantApiKey apiKey = keyRecord.get();
    TenantContext.set(apiKey.getTenantId());
    List<SimpleGrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"));
    var auth = new UsernamePasswordAuthenticationToken(apiKey.getTenantId(), null, authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);

    try {
      chain.doFilter(request, response);
    } finally {
      TenantContext.clear();
      SecurityContextHolder.clearContext();
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
