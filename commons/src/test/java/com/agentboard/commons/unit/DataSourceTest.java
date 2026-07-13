package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentboard.commons.domain.DataSource;
import com.agentboard.commons.exceptions.InvalidDataSourceException;
import org.junit.jupiter.api.Test;

class DataSourceTest {

  @Test
  void fromHeader_absentOrBlank_returnsManual() {
    assertThat(DataSource.fromHeader(null)).isEqualTo(DataSource.MANUAL);
    assertThat(DataSource.fromHeader("")).isEqualTo(DataSource.MANUAL);
    assertThat(DataSource.fromHeader("   ")).isEqualTo(DataSource.MANUAL);
  }

  @Test
  void fromHeader_validValues_caseInsensitive() {
    assertThat(DataSource.fromHeader("manual")).isEqualTo(DataSource.MANUAL);
    assertThat(DataSource.fromHeader("MANUAL")).isEqualTo(DataSource.MANUAL);
    assertThat(DataSource.fromHeader("automation")).isEqualTo(DataSource.AUTOMATION);
    assertThat(DataSource.fromHeader("Automation")).isEqualTo(DataSource.AUTOMATION);
    assertThat(DataSource.fromHeader("seed")).isEqualTo(DataSource.SEED);
    assertThat(DataSource.fromHeader("SEED")).isEqualTo(DataSource.SEED);
  }

  @Test
  void fromHeader_invalidValue_throwsInvalidDataSourceException() {
    assertThatThrownBy(() -> DataSource.fromHeader("bot"))
        .isInstanceOf(InvalidDataSourceException.class)
        .hasMessageContaining("bot");
  }

  @Test
  void fromDbValue_unknownDefaultsToManual() {
    assertThat(DataSource.fromDbValue(null)).isEqualTo(DataSource.MANUAL);
    assertThat(DataSource.fromDbValue("unknown")).isEqualTo(DataSource.MANUAL);
    assertThat(DataSource.fromDbValue("automation")).isEqualTo(DataSource.AUTOMATION);
  }

  @Test
  void dbValue_returnsLowercaseToken() {
    assertThat(DataSource.MANUAL.dbValue()).isEqualTo("manual");
    assertThat(DataSource.AUTOMATION.dbValue()).isEqualTo("automation");
    assertThat(DataSource.SEED.dbValue()).isEqualTo("seed");
  }
}
