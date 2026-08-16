package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.APPLIED;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.SAVED;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.SCREENING;
import static io.github.seremark.jobapplicationtracker.applications.domain.StatusChange.NOTE_MAX_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.applications.service.JobApplicationService;
import io.github.seremark.jobapplicationtracker.support.PostgreSqlIntegrationTest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class JobApplicationStatusIT extends PostgreSqlIntegrationTest {

  private static final Instant CREATED_AT = Instant.parse("2026-08-15T08:00:00Z");
  private static final Instant FIRST_CHANGE_AT = Instant.parse("2026-08-16T10:30:00Z");
  private static final UUID UNKNOWN_ID = UUID.fromString("0198b8c4-8a04-7000-8000-000000000003");

  @Autowired private JobApplicationService jobApplicationService;

  @Test
  void changeStatusUpdatesApplicationAndAddsHistoryEntry() throws Exception {
    UUID applicationId = persistApplication(SAVED);

    mockMvc
        .perform(
            patch("/api/applications/{id}/status", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"Screening","note":"  Recruiter call arranged.  "}
                    """))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(applicationId.toString()))
        .andExpect(jsonPath("$.status").value("Screening"))
        .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
        .andExpect(jsonPath("$.updatedAt").value(FIRST_CHANGE_AT.toString()));

    JobApplication persisted = jobApplicationRepository.findById(applicationId).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(SCREENING);
    assertThat(persisted.getUpdatedAt()).isEqualTo(FIRST_CHANGE_AT);
    assertThat(loadHistory(applicationId))
        .containsExactly(
            new StatusChangeSnapshot(null, "SAVED", CREATED_AT, null),
            new StatusChangeSnapshot(
                "SAVED", "SCREENING", FIRST_CHANGE_AT, "Recruiter call arranged."));
  }

  @Test
  void changeToCurrentStatusReturnsConflictWithoutAddingHistory() throws Exception {
    UUID applicationId = persistApplication(APPLIED);

    mockMvc
        .perform(
            patch("/api/applications/{id}/status", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"Applied","note":"Duplicate status"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Job application status conflict"))
        .andExpect(
            jsonPath("$.detail")
                .value("Job application '" + applicationId + "' already has status 'Applied'."));

    JobApplication persisted = jobApplicationRepository.findById(applicationId).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(APPLIED);
    assertThat(persisted.getUpdatedAt()).isEqualTo(CREATED_AT);
    assertThat(statusChangeCount(applicationId)).isEqualTo(1L);
  }

  @Test
  void changeStatusForUnknownApplicationReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(
            patch("/api/applications/{id}/status", UNKNOWN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"Screening\"}"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Job application not found"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidStatusRequests")
  void changeStatusWithInvalidRequestReturnsProblemDetailWithoutChanges(String requestJson)
      throws Exception {
    UUID applicationId = persistApplication(SAVED);

    mockMvc
        .perform(
            patch("/api/applications/{id}/status", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400));

    JobApplication persisted = jobApplicationRepository.findById(applicationId).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(SAVED);
    assertThat(persisted.getUpdatedAt()).isEqualTo(CREATED_AT);
    assertThat(statusChangeCount(applicationId)).isEqualTo(1L);
  }

  @Test
  void getStatusHistoryReturnsChangesInChronologicalOrder() throws Exception {
    UUID applicationId = persistApplication(SAVED);

    changeStatus(applicationId, "Applied", "Application submitted");
    testClock.setInstant(FIRST_CHANGE_AT.plusSeconds(7_200));
    changeStatus(applicationId, "Interview", null);

    mockMvc
        .perform(get("/api/applications/{id}/status-history", applicationId))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].id").isNotEmpty())
        .andExpect(jsonPath("$[0].previousStatus").value(nullValue()))
        .andExpect(jsonPath("$[0].newStatus").value("Saved"))
        .andExpect(jsonPath("$[0].changedAt").value(CREATED_AT.toString()))
        .andExpect(jsonPath("$[0].note").value(nullValue()))
        .andExpect(jsonPath("$[1].previousStatus").value("Saved"))
        .andExpect(jsonPath("$[1].newStatus").value("Applied"))
        .andExpect(jsonPath("$[1].changedAt").value(FIRST_CHANGE_AT.toString()))
        .andExpect(jsonPath("$[1].note").value("Application submitted"))
        .andExpect(jsonPath("$[2].previousStatus").value("Applied"))
        .andExpect(jsonPath("$[2].newStatus").value("Interview"))
        .andExpect(jsonPath("$[2].changedAt").value(FIRST_CHANGE_AT.plusSeconds(7_200).toString()))
        .andExpect(jsonPath("$[2].note").value(nullValue()));
  }

  @Test
  void getStatusHistoryForUnknownApplicationReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(get("/api/applications/{id}/status-history", UNKNOWN_ID))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Job application not found"));
  }

  @Test
  void failedHistoryInsertRollsBackCurrentStatusUpdate() {
    UUID applicationId = persistApplication(SAVED);

    try {
      createDeferredFailingHistoryTrigger();

      Throwable failure =
          catchThrowable(() -> jobApplicationService.changeStatus(applicationId, SCREENING, null));

      assertThat(failure).isInstanceOf(RuntimeException.class);
      assertThat(rootCause(failure)).hasMessageContaining("Forced status history failure");

      JobApplication persisted = jobApplicationRepository.findById(applicationId).orElseThrow();
      assertThat(persisted.getStatus()).isEqualTo(SAVED);
      assertThat(persisted.getUpdatedAt()).isEqualTo(CREATED_AT);
      assertThat(statusChangeCount(applicationId)).isEqualTo(1L);
    } finally {
      dropFailingHistoryTrigger();
    }
  }

  @Test
  void openApiDocumentsStatusEndpoints() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}/status'].patch.requestBody.content['application/json'].schema['$ref']")
                .value("#/components/schemas/ChangeJobApplicationStatusRequest"))
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}/status'].patch.responses['200'].content['application/json'].schema")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}/status'].patch.responses['400'].content['application/problem+json'].schema")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}/status'].patch.responses['404'].content['application/problem+json'].schema")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}/status'].patch.responses['409'].content['application/problem+json'].schema")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}/status-history'].get.responses['200'].content['application/json'].schema.items['$ref']")
                .value("#/components/schemas/StatusChangeResponse"))
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}/status-history'].get.responses['404'].content['application/problem+json'].schema")
                .exists());
  }

  static Stream<Named<String>> invalidStatusRequests() {
    return Stream.of(
        Named.of("missing status", "{}"),
        Named.of("unknown status", "{\"status\":\"Unknown\"}"),
        Named.of("numeric status", "{\"status\":1}"),
        Named.of(
            "note too long",
            """
            {"status":"Screening","note":"%s"}
            """
                .formatted("x".repeat(NOTE_MAX_LENGTH + 1))));
  }

  private UUID persistApplication(JobApplicationStatus status) {
    testClock.setInstant(CREATED_AT);
    JobApplication application =
        JobApplication.create(
            new JobApplicationDetails("Example Ltd.", "Java Developer"), status, testClock);
    UUID applicationId = jobApplicationRepository.saveAndFlush(application).getId();
    testClock.setInstant(FIRST_CHANGE_AT);
    return applicationId;
  }

  private void changeStatus(UUID applicationId, String newStatus, String note) throws Exception {
    String noteJson = note == null ? "null" : "\"" + note + "\"";
    String requestJson =
        """
        {"status":"%s","note":%s}
        """
            .formatted(newStatus, noteJson);

    mockMvc
        .perform(
            patch("/api/applications/{id}/status", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isOk());
  }

  private List<StatusChangeSnapshot> loadHistory(UUID applicationId) {
    return jdbcTemplate.query(
        """
        SELECT previous_status, new_status, changed_at, note
        FROM status_changes
        WHERE job_application_id = ?
        ORDER BY changed_at, id
        """,
        (resultSet, ignored) ->
            new StatusChangeSnapshot(
                resultSet.getString("previous_status"),
                resultSet.getString("new_status"),
                resultSet.getObject("changed_at", OffsetDateTime.class).toInstant(),
                resultSet.getString("note")),
        applicationId);
  }

  private Long statusChangeCount(UUID applicationId) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM status_changes WHERE job_application_id = ?",
        Long.class,
        applicationId);
  }

  private void createDeferredFailingHistoryTrigger() {
    jdbcTemplate.execute(
        """
        CREATE FUNCTION fail_status_change_insert()
        RETURNS trigger
        LANGUAGE plpgsql
        AS $function$
        BEGIN
          RAISE EXCEPTION 'Forced status history failure.';
        END;
        $function$
        """);
    jdbcTemplate.execute(
        """
        CREATE CONSTRAINT TRIGGER tr_status_changes_force_failure
        AFTER INSERT ON status_changes
        DEFERRABLE INITIALLY DEFERRED
        FOR EACH ROW
        EXECUTE FUNCTION fail_status_change_insert()
        """);
  }

  private void dropFailingHistoryTrigger() {
    jdbcTemplate.execute(
        "DROP TRIGGER IF EXISTS tr_status_changes_force_failure ON status_changes");
    jdbcTemplate.execute("DROP FUNCTION IF EXISTS fail_status_change_insert()");
  }

  private static Throwable rootCause(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current;
  }

  private record StatusChangeSnapshot(
      String previousStatus, String newStatus, Instant changedAt, String note) {}
}
