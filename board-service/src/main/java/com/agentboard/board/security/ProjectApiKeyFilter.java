package com.agentboard.board.security;

import com.agentboard.board.domain.Project;
import com.agentboard.board.repository.ProjectRepository;
import com.agentboard.commons.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the active Project and its Tenant from an {@code Authorization: Bearer <api-key>}
 * header where the api-key is a project-scoped key (prefixed {@code agb_}).
 *
 * <p>On a valid key, sets {@link TenantContext} with the project's tenantId and populates
 * {@link SecurityContextHolder} with the projectId as the principal so downstream code can
 * retrieve both tenantId and projectId from the security context.
 *
 * <p>The filter skips non-{@code agb_} Bearer tokens so that JWT-based user auth is unaffected.
 */
public class ProjectApiKeyFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String API_KEY_PREFIX = "agb_";

  private final ProjectRepository projectRepository;

  /** Creates the filter backed by the given project repository. */
  public ProjectApiKeyFilter(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      chain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(BEARER_PREFIX.length()).strip();
    if (!token.startsWith(API_KEY_PREFIX)) {
      chain.doFilter(request, response);
      return;
    }

    Optional<Project> projectOpt = projectRepository.findByApiKey(token);
    if (projectOpt.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write(
          "{\"error\":\"PROJECT_NOT_FOUND\","
              + "\"message\":\"API key not associated with any project\"}");
      return;
    }

    Project project = projectOpt.get();
    TenantContext.set(project.getTenantId());

    var principal = new ProjectPrincipal(project.getId(), project.getTenantId());
    var auth = new UsernamePasswordAuthenticationToken(
        principal, null, List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    try {
      chain.doFilter(request, response);
    } finally {
      TenantContext.clear();
      SecurityContextHolder.clearContext();
    }
  }
}
