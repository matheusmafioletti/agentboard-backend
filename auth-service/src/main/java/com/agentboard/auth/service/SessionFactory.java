package com.agentboard.auth.service;

import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantMembership;
import com.agentboard.auth.domain.UserAccount;
import com.agentboard.auth.dto.SessionResponse;
import com.agentboard.auth.security.JwtTokenService;
import org.springframework.stereotype.Component;

/** Builds session responses with JWT tokens for an active tenant. */
@Component
public class SessionFactory {

  private final JwtTokenService jwtTokenService;

  /** Creates the factory with the JWT service. */
  public SessionFactory(JwtTokenService jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  /** Issues a session for the user in the given tenant using membership role. */
  public SessionResponse buildSession(UserAccount user, Tenant tenant, TenantMembership membership) {
    String token = jwtTokenService.generate(
        user.getId(), tenant.getId(), membership.getRole());
    return new SessionResponse(
        token,
        user.getId(),
        tenant.getId(),
        tenant.getName(),
        user.getEmail(),
        user.getName(),
        membership.getRole());
  }

  /** Issues a session using an explicit role (e.g. immediately after creating admin membership). */
  public SessionResponse buildSession(
      UserAccount user, Tenant tenant, MembershipRole role) {
    String token = jwtTokenService.generate(user.getId(), tenant.getId(), role);
    return new SessionResponse(
        token,
        user.getId(),
        tenant.getId(),
        tenant.getName(),
        user.getEmail(),
        user.getName(),
        role);
  }
}
