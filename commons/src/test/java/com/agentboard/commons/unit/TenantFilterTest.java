package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.agentboard.commons.security.InvalidTokenException;
import com.agentboard.commons.security.JwtValidator;
import com.agentboard.commons.security.ParsedToken;
import com.agentboard.commons.security.TenantContext;
import com.agentboard.commons.security.TenantFilter;
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

@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

  @Mock
  private JwtValidator jwtValidator;

  private TenantFilter tenantFilter;

  @BeforeEach
  void setUp() {
    tenantFilter = new TenantFilter(jwtValidator);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void shouldSetTenantContext_whenValidBearerTokenProvided() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ParsedToken parsedToken = new ParsedToken(userId, tenantId, List.of("USER"));
    when(jwtValidator.validate("valid-token")).thenReturn(parsedToken);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
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

    tenantFilter.doFilter(request, response, chain);

    assertThat(capturedTenantId.get()).isEqualTo(tenantId);
    assertThat(TenantContext.get()).isNull();
  }

  @Test
  void shouldNotSetTenantContext_whenInvalidBearerTokenProvided() throws Exception {
    when(jwtValidator.validate("invalid-token")).thenThrow(new InvalidTokenException("bad token"));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");
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

    tenantFilter.doFilter(request, response, chain);

    assertThat(capturedTenantId.get()).isNull();
    assertThat(TenantContext.get()).isNull();
  }

  @Test
  void shouldProceedWithoutSettingContext_whenNoAuthorizationHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
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

    tenantFilter.doFilter(request, response, chain);

    assertThat(capturedTenantId.get()).isNull();
  }

  @Test
  void shouldClearContextAfterChain_whenValidTokenProvided() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ParsedToken parsedToken = new ParsedToken(userId, tenantId, List.of("USER"));
    when(jwtValidator.validate("valid-token")).thenReturn(parsedToken);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    tenantFilter.doFilter(request, response, new MockFilterChain());

    assertThat(TenantContext.get()).isNull();
  }
}
