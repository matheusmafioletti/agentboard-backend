package com.agentboard.commons.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that extracts the JWT Bearer token from the {@code Authorization} header,
 * validates it via {@link JwtValidator}, and stores the resolved tenant ID in
 * {@link TenantContext}.
 *
 * <p>{@link TenantContext} is always cleared in a {@code finally} block to prevent leakage
 * across pooled threads. Requests with an absent or invalid token proceed without a tenant
 * context; downstream security configuration enforces authentication requirements.
 */
public class TenantFilter extends OncePerRequestFilter {

  private final JwtValidator jwtValidator;

  /**
   * Creates a filter backed by the given {@link JwtValidator}.
   */
  public TenantFilter(JwtValidator jwtValidator) {
    this.jwtValidator = jwtValidator;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      try {
        ParsedToken parsed = jwtValidator.validate(token);
        TenantContext.set(parsed.tenantId());
      } catch (InvalidTokenException expected) {
        // NOTE: invalid tokens leave TenantContext unset; Spring Security enforces auth
      }
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }
}
