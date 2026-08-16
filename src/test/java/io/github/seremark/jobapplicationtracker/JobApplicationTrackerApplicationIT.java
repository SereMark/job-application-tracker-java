package io.github.seremark.jobapplicationtracker;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.APPLIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.support.PostgreSqlIntegrationTest;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.postgresql.PostgreSQLContainer;

class JobApplicationTrackerApplicationIT extends PostgreSqlIntegrationTest {

  @Autowired private Flyway flyway;

  @Autowired private PostgreSQLContainer postgresContainer;

  @Autowired private Clock clock;

  @Test
  void fullContextUsesPostgreSqlContainerAndAppliesFlywayMigration() {
    MigrationInfo currentMigration = flyway.info().current();

    assertThat(postgresContainer.isRunning()).isTrue();
    assertThat(jdbcTemplate.queryForObject("SELECT current_database()", String.class))
        .isEqualTo(postgresContainer.getDatabaseName());
    assertThat(
            jdbcTemplate.queryForObject("SELECT current_setting('server_version')", String.class))
        .isEqualTo("18.4");
    assertThat(currentMigration).isNotNull();
    assertThat(currentMigration.getVersion().getVersion()).isEqualTo("2");
    assertThat(
            jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'", String.class))
        .containsAll(
            List.of(
                "ix_job_applications_updated_at",
                "ix_job_applications_source_updated_at",
                "ix_job_applications_applied_on"));
    assertThat(clock).isSameAs(testClock);
    assertThat(clock.instant()).isEqualTo(DEFAULT_TEST_INSTANT);

    Instant adjustedInstant = DEFAULT_TEST_INSTANT.plusSeconds(60);
    testClock.setInstant(adjustedInstant);
    assertThat(clock.instant()).isEqualTo(adjustedInstant);

    assertThat(jobApplicationRepository.count()).isZero();
  }

  @Test
  void mockMvcReportsLiveAndReadyApplication() throws Exception {
    mockMvc
        .perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mockMvc
        .perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void repositoryPersistsAggregateWithUuidVersionSevenIdentifiers() {
    assertThat(jobApplicationRepository.count()).isZero();

    var details =
        new JobApplicationDetails(
            "Example Ltd.",
            "Java Developer",
            URI.create("https://example.com/jobs/42"),
            "LinkedIn",
            "Budapest",
            LocalDate.of(2026, 8, 16),
            "Integration test application.",
            null,
            null);
    JobApplication application = JobApplication.create(details, APPLIED, testClock);

    jobApplicationRepository.saveAndFlush(application);

    assertThat(application.getId()).isNotNull();
    assertThat(application.getId().version()).isEqualTo(7);
    assertThat(application.getStatusHistory())
        .singleElement()
        .satisfies(
            statusChange -> {
              assertThat(statusChange.getId()).isNotNull();
              assertThat(statusChange.getId().version()).isEqualTo(7);
            });
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM job_applications WHERE id = ?",
                String.class,
                application.getId()))
        .isEqualTo("APPLIED");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM status_changes WHERE job_application_id = ?",
                Long.class,
                application.getId()))
        .isEqualTo(1L);
  }
}
