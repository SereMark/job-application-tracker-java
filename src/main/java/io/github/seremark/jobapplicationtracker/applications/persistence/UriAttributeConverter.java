package io.github.seremark.jobapplicationtracker.applications.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.net.URI;

@Converter(autoApply = true)
public final class UriAttributeConverter implements AttributeConverter<URI, String> {

  @Override
  public String convertToDatabaseColumn(URI attribute) {
    return attribute == null ? null : attribute.toString();
  }

  @Override
  public URI convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : URI.create(databaseValue);
  }
}
