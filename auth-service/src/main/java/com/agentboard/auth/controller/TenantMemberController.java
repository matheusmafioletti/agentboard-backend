package com.agentboard.auth.controller;

import com.agentboard.auth.dto.MemberListResponse;
import com.agentboard.auth.service.MembershipService;
import com.agentboard.commons.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tenant member listing and revocation (admin). */
@Tag(name = "Members")
@RestController
@RequestMapping("/auth/tenants/{tenantId}/members")
public class TenantMemberController {

  private final MembershipService membershipService;

  /** Creates the controller. */
  public TenantMemberController(MembershipService membershipService) {
    this.membershipService = membershipService;
  }

  /** Lists all members in the tenant. */
  @Operation(summary = "List tenant members (admin)")
  @GetMapping
  public MemberListResponse listMembers(
      Authentication authentication,
      @PathVariable UUID tenantId) {
    UUID userId = (UUID) authentication.getPrincipal();
    membershipService.assertAdminOfActiveTenant(userId, TenantContext.get(), tenantId);
    return new MemberListResponse(membershipService.listMembers(tenantId));
  }

  /** Revokes a user's membership in the tenant. */
  @Operation(summary = "Revoke tenant membership (admin)")
  @DeleteMapping("/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeMember(
      Authentication authentication,
      @PathVariable UUID tenantId,
      @PathVariable UUID userId) {
    UUID callerId = (UUID) authentication.getPrincipal();
    membershipService.assertAdminOfActiveTenant(callerId, TenantContext.get(), tenantId);
    membershipService.revokeMembership(tenantId, userId);
  }
}
