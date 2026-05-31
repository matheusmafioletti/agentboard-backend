package com.agentboard.auth.controller;

import com.agentboard.auth.dto.AcceptInviteRequest;
import com.agentboard.auth.dto.CreateInviteRequest;
import com.agentboard.auth.dto.IdentifyInviteRequest;
import com.agentboard.auth.dto.IdentifyInviteResponse;
import com.agentboard.auth.dto.InviteListResponse;
import com.agentboard.auth.dto.InvitePreviewResponse;
import com.agentboard.auth.dto.InviteResponse;
import com.agentboard.auth.dto.SessionResponse;
import com.agentboard.auth.dto.VerifyInviteCredentialsRequest;
import com.agentboard.auth.dto.VerifyInviteCredentialsResponse;
import com.agentboard.auth.service.InviteService;
import com.agentboard.auth.service.MembershipService;
import com.agentboard.commons.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tenant invite administration and public accept flows. */
@Tag(name = "Invites")
@RestController
@RequestMapping("/auth")
public class TenantInviteController {

  private final InviteService inviteService;
  private final MembershipService membershipService;

  /** Creates the controller. */
  public TenantInviteController(InviteService inviteService, MembershipService membershipService) {
    this.inviteService = inviteService;
    this.membershipService = membershipService;
  }

  /** Lists pending and historical invites for a tenant. */
  @Operation(summary = "List invites for a tenant (admin)")
  @GetMapping("/tenants/{tenantId}/invites")
  public InviteListResponse listInvites(
      Authentication authentication,
      @PathVariable UUID tenantId) {
    UUID userId = (UUID) authentication.getPrincipal();
    membershipService.assertAdminOfActiveTenant(userId, TenantContext.get(), tenantId);
    return new InviteListResponse(inviteService.listInvites(tenantId));
  }

  /** Creates a new invite for the given email. */
  @Operation(summary = "Create a tenant invite (admin)")
  @PostMapping("/tenants/{tenantId}/invites")
  @ResponseStatus(HttpStatus.CREATED)
  public InviteResponse createInvite(
      Authentication authentication,
      @PathVariable UUID tenantId,
      @Valid @RequestBody CreateInviteRequest request) {
    UUID userId = (UUID) authentication.getPrincipal();
    membershipService.assertAdminOfActiveTenant(userId, TenantContext.get(), tenantId);
    return inviteService.createInvite(tenantId, userId, request.email());
  }

  /** Cancels a pending invite. */
  @Operation(summary = "Cancel a tenant invite (admin)")
  @DeleteMapping("/tenants/{tenantId}/invites/{inviteId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelInvite(
      Authentication authentication,
      @PathVariable UUID tenantId,
      @PathVariable UUID inviteId) {
    UUID userId = (UUID) authentication.getPrincipal();
    membershipService.assertAdminOfActiveTenant(userId, TenantContext.get(), tenantId);
    inviteService.cancelInvite(tenantId, inviteId);
  }

  /** Returns public invite preview data. */
  @Operation(summary = "Preview invite by token")
  @GetMapping("/invites/{token}")
  public InvitePreviewResponse previewInvite(@PathVariable String token) {
    return inviteService.getInvitePreview(token);
  }

  /** Validates invite email and reports whether a global account exists. */
  @Operation(summary = "Identify invite recipient by email")
  @PostMapping("/invites/{token}/identify")
  public IdentifyInviteResponse identifyInvite(
      @PathVariable String token,
      @Valid @RequestBody IdentifyInviteRequest request) {
    return inviteService.identifyInvite(token, request.email());
  }

  /** Verifies credentials for an existing user without accepting the invite. */
  @Operation(summary = "Verify invite credentials for existing user")
  @PostMapping("/invites/{token}/verify-credentials")
  public VerifyInviteCredentialsResponse verifyCredentials(
      @PathVariable String token,
      @Valid @RequestBody VerifyInviteCredentialsRequest request) {
    return inviteService.verifyCredentials(token, request.email(), request.password());
  }

  /** Accepts an invite and returns a session for the invited tenant. */
  @Operation(summary = "Accept invite by token")
  @PostMapping("/invites/{token}/accept")
  public SessionResponse acceptInvite(
      @PathVariable String token,
      @Valid @RequestBody AcceptInviteRequest request) {
    return inviteService.acceptInvite(token, request);
  }
}
