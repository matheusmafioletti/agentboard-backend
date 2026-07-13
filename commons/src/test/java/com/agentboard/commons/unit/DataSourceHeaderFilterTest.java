package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.commons.context.DataSourceContext;
import com.agentboard.commons.domain.DataSource;
import com.agentboard.commons.security.DataSourceHeaderFilter;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DataSourceHeaderFilterTest {

  private final DataSourceHeaderFilter filter = new DataSourceHeaderFilter();

  @AfterEach
  void tearDown() {
    DataSourceContext.clear();
  }

  @Test
  void shouldDefaultToManual_whenHeaderAbsent() throws Exception {
    AtomicReference<DataSource> captured = new AtomicReference<>();
    MockFilterChain chain = captureContext(captured);

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

    assertThat(captured.get()).isEqualTo(DataSource.MANUAL);
    assertThat(DataSourceContext.get()).isEqualTo(DataSource.MANUAL);
  }

  @Test
  void shouldParseValidHeader_andClearAfterChain() throws Exception {
    AtomicReference<DataSource> captured = new AtomicReference<>();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(DataSourceHeaderFilter.HEADER_NAME, "automation");
    MockFilterChain chain = captureContext(captured);

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(captured.get()).isEqualTo(DataSource.AUTOMATION);
    assertThat(DataSourceContext.get()).isEqualTo(DataSource.MANUAL);
  }

  @Test
  void shouldReturn400_whenHeaderInvalid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(DataSourceHeaderFilter.HEADER_NAME, "bot");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("INVALID_DATA_SOURCE");
    assertThat(DataSourceContext.get()).isEqualTo(DataSource.MANUAL);
  }

  private MockFilterChain captureContext(AtomicReference<DataSource> captured) {
    return new MockFilterChain() {
      @Override
      public void doFilter(
          jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
          throws java.io.IOException, jakarta.servlet.ServletException {
        captured.set(DataSourceContext.get());
        super.doFilter(req, res);
      }
    };
  }
}
