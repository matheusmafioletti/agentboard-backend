package com.agentboard.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.agentboard.commons.security.InvalidTokenException;
import com.agentboard.commons.security.JwtValidator;
import com.agentboard.commons.security.ParsedToken;
import com.agentboard.commons.security.TenantContext;
import java.util.List;
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
class JwtAuthFilterTest {

  @Mock
  private JwtValidator jwtValidator;

  private JwtAuthFilter jwtAuthFilter;

  @BeforeEach
  void setUp() {
    jwtAuthFilter = new JwtAuthFilter(jwtValidator);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldSetSecurityContext_whenValidBearerTokenProvided() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    ParsedToken parsedToken = new ParsedToken(userId, tenantId, List.of("ADMIN"));
    when(jwtValidator.validate("valid-token")).thenReturn(parsedToken);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<Object> capturedPrincipal = new AtomicReference<>();
    MockFilterChain chain = new MockFilterChain() {
      @Override
      public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
          throws java.io.IOException, jakarta.servlet.ServletException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
          capturedPrincipal.set(auth.getPrincipal());
        }
        super.doFilter(req, res);
      }
    };

    jwtAuthFilter.doFilter(request, response, chain);

    assertThat(capturedPrincipal.get()).isEqualTo(userId);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(TenantContext.get()).isNull();
  }

  @Test
  void shouldNotSetSecurityContext_whenInvalidTokenProvided() throws Exception {
    when(jwtValidator.validate("bad-token")).thenThrow(new InvalidTokenException("bad"));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer bad-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<Object> capturedPrincipal = new AtomicReference<>();
    MockFilterChain chain = new MockFilterChain() {
      @Override
      public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
          throws java.io.IOException, jakarta.servlet.ServletException {
        capturedPrincipal.set(SecurityContextHolder.getContext().getAuthentication());
        super.doFilter(req, res);
      }
    };

    jwtAuthFilter.doFilter(request, response, chain);

    assertThat(capturedPrincipal.get()).isNull();
  }

  @Test
  void shouldProceedWithoutAuth_whenNoAuthorizationHeaderPresent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    jwtAuthFilter.doFilter(request, response, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(TenantContext.get()).isNull();
  }

  @Test
  void shouldClearContextAfterChain_whenValidTokenUsed() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    ParsedToken parsedToken = new ParsedToken(userId, tenantId, List.of("USER"));
    when(jwtValidator.validate("valid-token")).thenReturn(parsedToken);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    jwtAuthFilter.doFilter(request, response, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(TenantContext.get()).isNull();
  }
}
