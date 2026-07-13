package com.agentboard.commons.security;

import com.agentboard.commons.context.DataSourceContext;
import com.agentboard.commons.domain.DataSource;
import com.agentboard.commons.exceptions.InvalidDataSourceException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Parses optional {@code X-Data-Source} and binds {@link DataSourceContext} for the request.
 */
public class DataSourceHeaderFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Data-Source";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    DataSource parsed;
    try {
      parsed = DataSource.fromHeader(request.getHeader(HEADER_NAME));
    } catch (InvalidDataSourceException ex) {
      writeError(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          "INVALID_DATA_SOURCE",
          ex.getMessage());
      return;
    }

    DataSourceContext.set(parsed);
    try {
      filterChain.doFilter(request, response);
    } finally {
      DataSourceContext.clear();
    }
  }

  private void writeError(
      HttpServletResponse response, int status, String error, String message) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    String timestamp = OffsetDateTime.now().toString();
    String body = "{\"error\":\"" + escapeJson(error)
        + "\",\"message\":\"" + escapeJson(message)
        + "\",\"timestamp\":\"" + escapeJson(timestamp) + "\"}";
    response.getWriter().write(body);
  }

  private static String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
