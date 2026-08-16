package io.github.seremark.jobapplicationtracker.applications.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

class JobApplicationStatusTests {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @ParameterizedTest
  @CsvSource({
    "SAVED, Saved",
    "APPLIED, Applied",
    "SCREENING, Screening",
    "INTERVIEW, Interview",
    "OFFER, Offer",
    "REJECTED, Rejected",
    "WITHDRAWN, Withdrawn"
  })
  void keepsUppercaseDatabaseValueAndUsesTitleCaseJsonValue(String databaseValue, String jsonValue)
      throws Exception {
    JobApplicationStatus status = JobApplicationStatus.valueOf(databaseValue);

    assertThat(status.name()).isEqualTo(databaseValue);
    assertThat(jsonMapper.writeValueAsString(status)).isEqualTo('"' + jsonValue + '"');
    assertThat(jsonMapper.readValue('"' + jsonValue + '"', JobApplicationStatus.class))
        .isEqualTo(status);
  }
}
