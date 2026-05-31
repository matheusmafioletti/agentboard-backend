package com.agentboard.auth.controller;

import com.agentboard.auth.dto.ChangePasswordRequest;
import com.agentboard.auth.dto.CreateTenantRequest;
import com.agentboard.auth.dto.CreateTenantResponse;
import com.agentboard.auth.dto.LoginRequest;
import com.agentboard.auth.dto.MembershipListResponse;
import com.agentboard.auth.dto.RegisterRequest;
import com.agentboard.auth.dto.RegisterResponse;
import com.agentboard.auth.dto.SelectTenantRequest;
import com.agentboard.auth.dto.SessionResponse;
import com.agentboard.auth.dto.SwitchTenantRequest;
import com.agentboard.auth.service.AuthService;
import com.agentboard.auth.service.MembershipService;
import com.agentboard.auth.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Exposes registration, login, tenant creation, and password endpoints. */
@Tag(name = "Authentication")
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;
  private final TenantService tenantService;
  private final MembershipService membershipService;

  /**
   * Creates the controller with required services.
   */
  public AuthController(
      AuthService authService,
      TenantService tenantService,
      MembershipService membershipService) {
    this.authService = authService;
    this.tenantService = tenantService;
    this.membershipService = membershipService;
  }

  /** Registers a new global user and workspace. */
  @Operation(summary = "Register a new tenant and user")
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  /** Authenticates credentials; may return session or tenant selection list. */
  @Operation(summary = "Authenticate with email and password")
  @PostMapping("/login")
  public Object login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  /** Completes login by selecting a workspace. */
  @Operation(summary = "Select workspace after multi-tenant login")
  @PostMapping("/select-tenant")
  public SessionResponse selectTenant(@Valid @RequestBody SelectTenantRequest request) {
    return authService.selectTenant(request);
  }

  /** Creates an additional workspace for the authenticated user. */
  @Operation(summary = "Create a new workspace for the authenticated user")
  @PostMapping("/tenants")
  @ResponseStatus(HttpStatus.CREATED)
  public CreateTenantResponse createTenant(
      Authentication authentication,
      @Valid @RequestBody CreateTenantRequest request) {
    UUID userId = (UUID) authentication.getPrincipal();
    return tenantService.createTenantForUser(userId, request.tenantName());
  }

  /** Lists workspaces for the authenticated user. */
  @Operation(summary = "List memberships for the authenticated user")
  @GetMapping("/me/memberships")
  public MembershipListResponse listMyMemberships(Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    return new MembershipListResponse(membershipService.listByUserId(userId));
  }

  /** Switches the active workspace for the authenticated user. */
  @Operation(summary = "Switch active workspace without re-authentication")
  @PostMapping("/switch-tenant")
  public SessionResponse switchTenant(
      Authentication authentication,
      @Valid @RequestBody SwitchTenantRequest request) {
    UUID userId = (UUID) authentication.getPrincipal();
    return authService.switchTenant(userId, request.tenantId());
  }

  /** Changes the password for the user identified in the request body. */
  @Operation(summary = "Change password for an existing user")
  @PutMapping("/change-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    authService.changePassword(request);
  }
}
