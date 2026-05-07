package com.agentboard.board.security;

import com.agentboard.commons.security.InvalidTokenException;
import com.agentboard.commons.security.JwtValidator;
import com.agentboard.commons.security.ParsedToken;
import com.agentboard.commons.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates Bearer JWT tokens, sets {@link TenantContext}, and populates
 * {@link SecurityContextHolder} so that Spring Security sees the request as authenticated.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

  private final JwtValidator jwtValidator;

  /**
   * Creates the filter backed by the given {@link JwtValidator}.
   */
  public JwtAuthFilter(JwtValidator jwtValidator) {
    this.jwtValidator = jwtValidator;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      try {
        ParsedToken parsed = jwtValidator.validate(token);
        TenantContext.set(parsed.tenantId());
        List<SimpleGrantedAuthority> authorities = parsed.roles().stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
            .toList();
        var auth = new UsernamePasswordAuthenticationToken(parsed.userId(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (InvalidTokenException e) {
        // NOTE: invalid tokens leave SecurityContext empty; Spring Security enforces auth rules
        log.warn("JWT validation failed for request [{} {}]: {} — cause: {}",
            request.getMethod(), request.getRequestURI(), e.getMessage(),
            e.getCause() != null ? e.getCause().getMessage() : "no cause");
      }
    }

    try {
      chain.doFilter(request, response);
    } finally {
      TenantContext.clear();
      SecurityContextHolder.clearContext();
    }
  }
}
