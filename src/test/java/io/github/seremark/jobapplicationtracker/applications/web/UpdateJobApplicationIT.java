package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.NOTES_MAX_LENGTH;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.APPLIED;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.INTERVIEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.support.PostgreSqlIntegrationTest;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class UpdateJobApplicationIT extends PostgreSqlIntegrationTest {

  private static final Instant CREATED_AT = Instant.parse("2026-08-15T08:00:00Z");
  private static final Instant FIRST_UPDATE_AT = Instant.parse("2026-08-16T09:30:00Z");
  private static final UUID UNKNOWN_ID = UUID.fromString("0198b8c4-8a04-7000-8000-000000000002");

  @Test
  void updateReplacesEditableDetailsAndIsIdempotent() throws Exception {
    UUID applicationId = persistExistingApplication(APPLIED).getId();
    String requestJson =
        """
        {
          "companyName": "  Example Ltd.  ",
          "positionTitle": "  Senior Java Developer  ",
          "jobPostingUrl": "https://example.com/jobs/84",
          "source": "  LinkedIn  ",
          "location": "  Budapest  ",
          "appliedOn": "2026-08-17",
          "notes": "  Referred by a former colleague.  ",
          "nextActionDescription": "  Contact the recruiter  ",
          "nextActionDueAt": "2026-08-20T14:00:00+02:00"
        }
        """;

    MvcResult firstUpdate =
        mockMvc
            .perform(
                put("/api/applications/{id}", applicationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(applicationId.toString()))
            .andExpect(jsonPath("$.companyName").value("Example Ltd."))
            .andExpect(jsonPath("$.positionTitle").value("Senior Java Developer"))
            .andExpect(jsonPath("$.status").value("Applied"))
            .andExpect(jsonPath("$.jobPostingUrl").value("https://example.com/jobs/84"))
            .andExpect(jsonPath("$.source").value("LinkedIn"))
            .andExpect(jsonPath("$.location").value("Budapest"))
            .andExpect(jsonPath("$.appliedOn").value("2026-08-17"))
            .andExpect(jsonPath("$.notes").value("Referred by a former colleague."))
            .andExpect(jsonPath("$.nextActionDescription").value("Contact the recruiter"))
            .andExpect(jsonPath("$.nextActionDueAt").value("2026-08-20T12:00:00Z"))
            .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(FIRST_UPDATE_AT.toString()))
            .andReturn();

    String firstResponse = firstUpdate.getResponse().getContentAsString();
    JobApplication firstPersisted = jobApplicationRepository.findById(applicationId).orElseThrow();
    assertThat(firstPersisted.getCompanyName()).isEqualTo("Example Ltd.");
    assertThat(firstPersisted.getSource()).isEqualTo("LinkedIn");
    assertThat(firstPersisted.getNextActionDueAt())
        .isEqualTo(Instant.parse("2026-08-20T12:00:00Z"));
    assertThat(firstPersisted.getStatus()).isEqualTo(APPLIED);
    assertThat(firstPersisted.getCreatedAt()).isEqualTo(CREATED_AT);
    assertThat(firstPersisted.getUpdatedAt()).isEqualTo(FIRST_UPDATE_AT);
    assertThat(statusChangeCount(applicationId)).isEqualTo(1L);

    testClock.setInstant(FIRST_UPDATE_AT.plusSeconds(86_400));

    mockMvc
        .perform(
            put("/api/applications/{id}", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isOk())
        .andExpect(content().string(firstResponse));

    JobApplication repeatedPersisted =
        jobApplicationRepository.findById(applicationId).orElseThrow();
    assertThat(repeatedPersisted.getStatus()).isEqualTo(APPLIED);
    assertThat(repeatedPersisted.getUpdatedAt()).isEqualTo(FIRST_UPDATE_AT);
    assertThat(statusChangeCount(applicationId)).isEqualTo(1L);
  }

  @Test
  void updateClearsOmittedOptionalDetails() throws Exception {
    UUID applicationId = persistExistingApplication(INTERVIEW).getId();

    mockMvc
        .perform(
            put("/api/applications/{id}", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"companyName":"Example Ltd.","positionTitle":"Java Developer"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyName").value("Example Ltd."))
        .andExpect(jsonPath("$.positionTitle").value("Java Developer"))
        .andExpect(jsonPath("$.status").value("Interview"))
        .andExpect(jsonPath("$.jobPostingUrl").value(nullValue()))
        .andExpect(jsonPath("$.source").value(nullValue()))
        .andExpect(jsonPath("$.location").value(nullValue()))
        .andExpect(jsonPath("$.appliedOn").value(nullValue()))
        .andExpect(jsonPath("$.notes").value(nullValue()))
        .andExpect(jsonPath("$.nextActionDescription").value(nullValue()))
        .andExpect(jsonPath("$.nextActionDueAt").value(nullValue()))
        .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
        .andExpect(jsonPath("$.updatedAt").value(FIRST_UPDATE_AT.toString()));

    JobApplication persisted = jobApplicationRepository.findById(applicationId).orElseThrow();
    assertThat(persisted.getJobPostingUrl()).isNull();
    assertThat(persisted.getSource()).isNull();
    assertThat(persisted.getLocation()).isNull();
    assertThat(persisted.getAppliedOn()).isNull();
    assertThat(persisted.getNotes()).isNull();
    assertThat(persisted.getNextActionDescription()).isNull();
    assertThat(persisted.getNextActionDueAt()).isNull();
    assertThat(persisted.getStatus()).isEqualTo(INTERVIEW);
    assertThat(persisted.getCreatedAt()).isEqualTo(CREATED_AT);
    assertThat(persisted.getUpdatedAt()).isEqualTo(FIRST_UPDATE_AT);
    assertThat(statusChangeCount(applicationId)).isEqualTo(1L);
  }

  @Test
  void updateUnknownJobApplicationReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(
            put("/api/applications/{id}", UNKNOWN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"companyName":"Example Ltd.","positionTitle":"Java Developer"}
                    """))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Job application not found"))
        .andExpect(
            jsonPath("$.detail").value("No job application with id '" + UNKNOWN_ID + "' exists."));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidUpdateRequests")
  void updateWithInvalidValuesReturnsValidationProblemWithoutChanges(String requestJson)
      throws Exception {
    UUID applicationId = persistExistingApplication(APPLIED).getId();

    mockMvc
        .perform(
            put("/api/applications/{id}", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Request validation failed"))
        .andExpect(jsonPath("$.errors").isNotEmpty());

    JobApplication persisted = jobApplicationRepository.findById(applicationId).orElseThrow();
    assertThat(persisted.getCompanyName()).isEqualTo("Original Corp.");
    assertThat(persisted.getPositionTitle()).isEqualTo("Backend Engineer");
    assertThat(persisted.getNotes()).isEqualTo("Original notes.");
    assertThat(persisted.getStatus()).isEqualTo(APPLIED);
    assertThat(persisted.getUpdatedAt()).isEqualTo(CREATED_AT);
    assertThat(statusChangeCount(applicationId)).isEqualTo(1L);
  }

  @Test
  void openApiDocumentsUpdateContract() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}'].put.requestBody.content['application/json'].schema['$ref']")
                .value("#/components/schemas/UpdateJobApplicationRequest"))
        .andExpect(
            jsonPath("$.components.schemas.UpdateJobApplicationRequest.properties.status")
                .doesNotExist())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}'].put.responses['200'].content['application/json'].schema")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}'].put.responses['400'].content['application/problem+json'].schema")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}'].put.responses['404'].content['application/problem+json'].schema")
                .exists());
  }

  static Stream<Named<String>> invalidUpdateRequests() {
    return Stream.of(
        Named.of("missing companyName", "{\"positionTitle\":\"Java Developer\"}"),
        Named.of(
            "blank positionTitle", "{\"companyName\":\"Example Ltd.\",\"positionTitle\":\" \"}"),
        Named.of(
            "relative jobPostingUrl",
            """
            {"companyName":"Example Ltd.","positionTitle":"Java Developer","jobPostingUrl":"/jobs/42"}
            """),
        Named.of(
            "next action description without due date",
            """
            {"companyName":"Example Ltd.","positionTitle":"Java Developer","nextActionDescription":"Contact recruiter"}
            """),
        Named.of(
            "next action due date without description",
            """
            {"companyName":"Example Ltd.","positionTitle":"Java Developer","nextActionDueAt":"2026-08-18T12:00:00Z"}
            """),
        Named.of(
            "notes too long",
            """
            {"companyName":"Example Ltd.","positionTitle":"Java Developer","notes":"%s"}
            """
                .formatted("x".repeat(NOTES_MAX_LENGTH + 1))));
  }

  private JobApplication persistExistingApplication(JobApplicationStatus status) {
    testClock.setInstant(CREATED_AT);
    JobApplication application =
        JobApplication.create(
            new JobApplicationDetails(
                "Original Corp.",
                "Backend Engineer",
                URI.create("https://example.com/jobs/original"),
                "Referral",
                "Remote",
                LocalDate.of(2026, 8, 10),
                "Original notes.",
                "Prepare for the interview",
                Instant.parse("2026-08-18T12:00:00Z")),
            status,
            testClock);
    JobApplication persisted = jobApplicationRepository.saveAndFlush(application);
    testClock.setInstant(FIRST_UPDATE_AT);
    return persisted;
  }

  private Long statusChangeCount(UUID applicationId) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM status_changes WHERE job_application_id = ?",
        Long.class,
        applicationId);
  }
}
