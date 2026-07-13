package com.agentboard.commons.context;

import com.agentboard.commons.domain.DataSource;

/**
 * Thread-local holder for the current request's data source (from {@code X-Data-Source}).
 *
 * <p>Must be cleared in a {@code finally} block after every request.
 */
public final class DataSourceContext {

  private static final ThreadLocal<DataSource> source = new ThreadLocal<>();

  private DataSourceContext() {}

  /**
   * Returns the data source bound to the current thread, or {@link DataSource#MANUAL} if unset.
   */
  public static DataSource get() {
    DataSource current = source.get();
    return current != null ? current : DataSource.MANUAL;
  }

  /**
   * Binds the given data source to the current thread.
   */
  public static void set(DataSource dataSource) {
    source.set(dataSource);
  }

  /**
   * Removes the data source from the current thread.
   */
  public static void clear() {
    source.remove();
  }
}
