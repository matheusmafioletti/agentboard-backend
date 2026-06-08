package com.agentboard.board.unit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.agentboard.board.domain.Project;
import com.agentboard.board.repository.ProjectRepository;
import com.agentboard.board.security.ProjectApiKeyFilter;
import com.agentboard.commons.security.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ProjectApiKeyFilterTest {

  @Mock
  private ProjectRepository projectRepository;

  private ProjectApiKeyFilter projectApiKeyFilter;

  @BeforeEach
  void setUp() {
    projectApiKeyFilter = new ProjectApiKeyFilter(projectRepository);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldProceedWithoutAuth_whenNoAuthorizationHeaderPresent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    projectApiKeyFilter.doFilter(request, response, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldProceedWithoutAuth_whenBearerTokenIsNotAgbPrefixed() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer jwt-token-not-starting-with-agb");
    MockHttpServletResponse response = new MockHttpServletResponse();

    projectApiKeyFilter.doFilter(request, response, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldReturnUnauthorized_whenAgbKeyNotFoundInRepository() throws Exception {
    when(projectRepository.findByApiKey("agb_unknown-key")).thenReturn(Optional.empty());

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer agb_unknown-key");
    MockHttpServletResponse response = new MockHttpServletResponse();

    projectApiKeyFilter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  void shouldSetTenantContext_whenValidAgbKeyProvided() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    Project project = new Project(tenantId, "My Project", null, "agb_valid-key");
    org.springframework.test.util.ReflectionTestUtils.setField(project, "id", projectId);
    when(projectRepository.findByApiKey("agb_valid-key")).thenReturn(Optional.of(project));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer agb_valid-key");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<UUID> capturedTenantId = new AtomicReference<>();
    MockFilterChain chain = new MockFilterChain() {
      @Override
      public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
          throws java.io.IOException, jakarta.servlet.ServletException {
        capturedTenantId.set(TenantContext.get());
        super.doFilter(req, res);
      }
    };

    projectApiKeyFilter.doFilter(request, response, chain);

    assertThat(capturedTenantId.get()).isEqualTo(tenantId);
    assertThat(TenantContext.get()).isNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
