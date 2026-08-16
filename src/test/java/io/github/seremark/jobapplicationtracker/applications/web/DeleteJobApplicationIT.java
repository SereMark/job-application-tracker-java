package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.APPLIED;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.INTERVIEW;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.SAVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.support.PostgreSqlIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class DeleteJobApplicationIT extends PostgreSqlIntegrationTest {

  private static final Instant CREATED_AT = Instant.parse("2026-08-15T08:00:00Z");
  private static final UUID UNKNOWN_ID = UUID.fromString("0198b8c4-8a04-7000-8000-000000000004");

  @Test
  void deleteExistingApplicationReturnsNoContentAndCascadesHistory() throws Exception {
    UUID applicationId = persistApplicationWithHistory();

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT delete_rule
                FROM information_schema.referential_constraints
                WHERE constraint_name = 'fk_status_changes_job_application'
                """,
                String.class))
        .isEqualTo("CASCADE");
    assertThat(statusChangeCount(applicationId)).isEqualTo(3L);

    mockMvc
        .perform(delete("/api/applications/{id}", applicationId))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    assertThat(jobApplicationRepository.existsById(applicationId)).isFalse();
    assertThat(statusChangeCount(applicationId)).isZero();
  }

  @Test
  void deleteUnknownApplicationReturnsProblemDetailWithoutDeletingOtherData() throws Exception {
    UUID existingApplicationId = persistApplication();

    mockMvc
        .perform(delete("/api/applications/{id}", UNKNOWN_ID))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Job application not found"))
        .andExpect(
            jsonPath("$.detail").value("No job application with id '" + UNKNOWN_ID + "' exists."));

    assertThat(jobApplicationRepository.existsById(existingApplicationId)).isTrue();
    assertThat(statusChangeCount(existingApplicationId)).isEqualTo(1L);
  }

  @Test
  void openApiDocumentsDeleteResponses() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/applications/{id}'].delete.responses['204']").exists())
        .andExpect(
            jsonPath("$.paths['/api/applications/{id}'].delete.responses['204'].content")
                .doesNotExist())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}'].delete.responses['404'].content['application/problem+json'].schema")
                .exists());
  }

  private UUID persistApplicationWithHistory() {
    testClock.setInstant(CREATED_AT);
    JobApplication application = createApplication();
    testClock.setInstant(CREATED_AT.plusSeconds(3_600));
    application.changeStatus(APPLIED, "Application submitted", testClock);
    testClock.setInstant(CREATED_AT.plusSeconds(7_200));
    application.changeStatus(INTERVIEW, "Interview arranged", testClock);
    return jobApplicationRepository.saveAndFlush(application).getId();
  }

  private UUID persistApplication() {
    testClock.setInstant(CREATED_AT);
    return jobApplicationRepository.saveAndFlush(createApplication()).getId();
  }

  private JobApplication createApplication() {
    return JobApplication.create(
        new JobApplicationDetails("Example Ltd.", "Java Developer"), SAVED, testClock);
  }

  private Long statusChangeCount(UUID applicationId) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM status_changes WHERE job_application_id = ?",
        Long.class,
        applicationId);
  }
}
