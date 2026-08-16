package io.github.seremark.jobapplicationtracker.applications.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class UriAttributeConverterTests {

  private final UriAttributeConverter converter = new UriAttributeConverter();

  @Test
  void convertsUriToDatabaseTextAndBack() {
    URI uri = URI.create("https://example.com/jobs/42?source=java");

    String databaseValue = converter.convertToDatabaseColumn(uri);

    assertThat(databaseValue).isEqualTo(uri.toString());
    assertThat(converter.convertToEntityAttribute(databaseValue)).isEqualTo(uri);
  }

  @Test
  void preservesNullValues() {
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToEntityAttribute(null)).isNull();
  }
}
