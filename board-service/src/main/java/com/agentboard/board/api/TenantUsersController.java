package com.agentboard.board.api;

import com.agentboard.board.repository.TenantUserRepository;
import com.agentboard.board.security.ProjectPrincipal;
import com.agentboard.commons.security.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the list of users belonging to the authenticated tenant.
 *
 * <p>Used by the frontend to populate assignee pickers without a separate call to auth-service.
 */
@RestController
@RequestMapping("/api/v1/users")
public class TenantUsersController {

  private final TenantUserRepository tenantUserRepository;

  /** Creates the controller backed by the given repository. */
  public TenantUsersController(TenantUserRepository tenantUserRepository) {
    this.tenantUserRepository = tenantUserRepository;
  }

  /** Returns all users belonging to the caller's tenant. */
  @GetMapping
  public List<TenantUserResponse> listUsers(
      @AuthenticationPrincipal ProjectPrincipal principal) {
    UUID tenantId = principal != null ? principal.tenantId() : TenantContext.get();
    return tenantUserRepository.findAllByTenantIdOrderByEmail(tenantId).stream()
        .map(u -> new TenantUserResponse(u.getId(), u.getEmail()))
        .toList();
  }

  /** Response DTO for a tenant user. */
  public record TenantUserResponse(UUID id, String email) {}
}
