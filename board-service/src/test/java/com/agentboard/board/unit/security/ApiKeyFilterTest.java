package com.agentboard.board.unit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.agentboard.board.domain.TenantApiKey;
import com.agentboard.board.repository.TenantApiKeyRepository;
import com.agentboard.board.security.ApiKeyFilter;
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
class ApiKeyFilterTest {

  @Mock
  private TenantApiKeyRepository tenantApiKeyRepository;

  private ApiKeyFilter apiKeyFilter;

  @BeforeEach
  void setUp() {
    apiKeyFilter = new ApiKeyFilter(tenantApiKeyRepository);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldProceedWithoutAuth_whenNoApiKeyHeaderPresent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    apiKeyFilter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldReturnUnauthorized_whenApiKeyNotFound() throws Exception {
    when(tenantApiKeyRepository.findByKeyHashAndRevokedAtIsNull(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Optional.empty());

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-API-Key", "invalid-key");
    MockHttpServletResponse response = new MockHttpServletResponse();

    apiKeyFilter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  void shouldSetTenantContext_whenValidApiKeyProvided() throws Exception {
    UUID tenantId = UUID.randomUUID();
    TenantApiKey apiKey = new TenantApiKey(tenantId, "hashed-key");
    when(tenantApiKeyRepository.findByKeyHashAndRevokedAtIsNull(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Optional.of(apiKey));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-API-Key", "raw-api-key-value");
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

    apiKeyFilter.doFilter(request, response, chain);

    assertThat(capturedTenantId.get()).isEqualTo(tenantId);
    assertThat(TenantContext.get()).isNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
