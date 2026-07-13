package com.agentboard.commons.domain;

import com.agentboard.commons.exceptions.InvalidDataSourceException;

/**
 * Provenance tag for records created via the API ({@code X-Data-Source} header).
 */
public enum DataSource {

  MANUAL("manual"),
  AUTOMATION("automation"),
  SEED("seed");

  private final String dbValue;

  DataSource(String dbValue) {
    this.dbValue = dbValue;
  }

  /**
   * Returns the lowercase value stored in {@code data_source} columns.
   */
  public String dbValue() {
    return dbValue;
  }

  /**
   * Parses the optional {@code X-Data-Source} header value (case-insensitive).
   *
   * @param raw header value; absent or blank maps to {@link #MANUAL}
   * @throws InvalidDataSourceException when the value is not manual, automation, or seed
   */
  public static DataSource fromHeader(String raw) {
    if (raw == null || raw.isBlank()) {
      return MANUAL;
    }
    String normalized = raw.trim();
    for (DataSource source : values()) {
      if (source.dbValue.equalsIgnoreCase(normalized)) {
        return source;
      }
    }
    throw new InvalidDataSourceException(
        "Invalid X-Data-Source value: '" + raw + "' (expected manual, automation, or seed)");
  }

  /**
   * Parses a database column value; unknown values default to {@link #MANUAL}.
   */
  public static DataSource fromDbValue(String dbValue) {
    if (dbValue == null || dbValue.isBlank()) {
      return MANUAL;
    }
    for (DataSource source : values()) {
      if (source.dbValue.equalsIgnoreCase(dbValue.trim())) {
        return source;
      }
    }
    return MANUAL;
  }
}
