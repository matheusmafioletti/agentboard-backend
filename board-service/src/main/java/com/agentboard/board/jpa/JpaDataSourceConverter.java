package com.agentboard.board.jpa;

import com.agentboard.commons.domain.DataSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps {@link DataSource} to lowercase {@code data_source} column values. */
@Converter(autoApply = true)
public class JpaDataSourceConverter implements AttributeConverter<DataSource, String> {

  @Override
  public String convertToDatabaseColumn(DataSource attribute) {
    return attribute == null ? DataSource.MANUAL.dbValue() : attribute.dbValue();
  }

  @Override
  public DataSource convertToEntityAttribute(String dbData) {
    return DataSource.fromDbValue(dbData);
  }
}
