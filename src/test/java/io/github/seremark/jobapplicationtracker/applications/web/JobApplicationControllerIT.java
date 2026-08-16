package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.COMPANY_NAME_MAX_LENGTH;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.JOB_POSTING_URL_MAX_LENGTH;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.APPLIED;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.SAVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.support.PostgreSqlIntegrationTest;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class JobApplicationControllerIT extends PostgreSqlIntegrationTest {

  @Test
  void createThenGetPersistsAndReturnsJobApplication() throws Exception {
    String requestJson =
        """
        {
          "companyName": "  Example Ltd.  ",
          "positionTitle": "  Java Developer  ",
          "status": "Applied",
          "jobPostingUrl": "https://example.com/jobs/42",
          "source": "  LinkedIn  ",
          "location": "  Budapest  ",
          "appliedOn": "2026-08-16",
          "notes": "  Referred by a former colleague.  ",
          "nextActionDescription": "  Contact the recruiter  ",
          "nextActionDueAt": "2026-08-18T14:00:00+02:00"
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/applications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.companyName").value("Example Ltd."))
            .andExpect(jsonPath("$.positionTitle").value("Java Developer"))
            .andExpect(jsonPath("$.status").value("Applied"))
            .andExpect(jsonPath("$.jobPostingUrl").value("https://example.com/jobs/42"))
            .andExpect(jsonPath("$.source").value("LinkedIn"))
            .andExpect(jsonPath("$.location").value("Budapest"))
            .andExpect(jsonPath("$.appliedOn").value("2026-08-16"))
            .andExpect(jsonPath("$.notes").value("Referred by a former colleague."))
            .andExpect(jsonPath("$.nextActionDescription").value("Contact the recruiter"))
            .andExpect(jsonPath("$.nextActionDueAt").value("2026-08-18T12:00:00Z"))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_TEST_INSTANT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_TEST_INSTANT.toString()))
            .andReturn();

    String createdJson = createResult.getResponse().getContentAsString();
    UUID applicationId = UUID.fromString(JsonPath.read(createdJson, "$.id"));
    String location =
        Objects.requireNonNull(createResult.getResponse().getHeader(HttpHeaders.LOCATION));

    assertThat(applicationId.version()).isEqualTo(7);
    assertThat(location).endsWith("/api/applications/" + applicationId);

    mockMvc
        .perform(get(URI.create(location).getRawPath()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(createdJson));

    JobApplication persisted = jobApplicationRepository.findById(applicationId).orElseThrow();
    assertThat(persisted.getCompanyName()).isEqualTo("Example Ltd.");
    assertThat(persisted.getPositionTitle()).isEqualTo("Java Developer");
    assertThat(persisted.getStatus()).isEqualTo(APPLIED);
    assertThat(persisted.getAppliedOn()).isEqualTo(LocalDate.of(2026, 8, 16));
    assertThat(persisted.getNextActionDueAt()).isEqualTo(Instant.parse("2026-08-18T12:00:00Z"));

    UUID statusChangeId =
        Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "SELECT id FROM status_changes WHERE job_application_id = ?",
                UUID.class,
                applicationId));
    assertThat(statusChangeId.version()).isEqualTo(7);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT previous_status FROM status_changes WHERE job_application_id = ?",
                String.class,
                applicationId))
        .isNull();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT new_status FROM status_changes WHERE job_application_id = ?",
                String.class,
                applicationId))
        .isEqualTo("APPLIED");
    OffsetDateTime changedAt =
        Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "SELECT changed_at FROM status_changes WHERE job_application_id = ?",
                OffsetDateTime.class,
                applicationId));
    assertThat(changedAt.toInstant()).isEqualTo(DEFAULT_TEST_INSTANT);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT note FROM status_changes WHERE job_application_id = ?",
                String.class,
                applicationId))
        .isNull();
  }

  @Test
  void createWithoutStatusDefaultsToSaved() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/applications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"companyName":"Example Ltd.","positionTitle":"Java Developer"}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("Saved"))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_TEST_INSTANT.toString()))
            .andReturn();

    UUID applicationId =
        UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    JobApplication persisted = jobApplicationRepository.findById(applicationId).orElseThrow();

    assertThat(persisted.getStatus()).isEqualTo(SAVED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT new_status FROM status_changes WHERE job_application_id = ?",
                String.class,
                applicationId))
        .isEqualTo("SAVED");
  }

  @Test
  void getUnknownJobApplicationReturnsProblemDetail() throws Exception {
    UUID unknownId = UUID.fromString("0198b8c4-8a04-7000-8000-000000000001");

    mockMvc
        .perform(get("/api/applications/{id}", unknownId))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Job application not found"))
        .andExpect(
            jsonPath("$.detail").value("No job application with id '" + unknownId + "' exists."));
  }

  @Test
  void openApiDocumentsCreateAndGetResponseSchemas() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications'].post.responses['201'].content['application/json'].schema")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications'].post.responses['400'].content['application/problem+json'].schema")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}'].get.responses['200'].content['application/json'].schema")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}'].get.responses['404'].content['application/problem+json'].schema")
                .exists());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidCreateRequests")
  void createWithInvalidValuesReturnsValidationProblem(String requestJson) throws Exception {
    mockMvc
        .perform(
            post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(requestJson))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Request validation failed"))
        .andExpect(jsonPath("$.errors").isNotEmpty());

    assertDatabaseIsEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"Unknown\"", "1"})
  void createWithInvalidStatusReturnsProblemDetail(String statusJson) throws Exception {
    String requestJson =
        """
        {"companyName":"Example Ltd.","positionTitle":"Java Developer","status":%s}
        """
            .formatted(statusJson);

    mockMvc
        .perform(
            post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(requestJson))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400));

    assertDatabaseIsEmpty();
  }

  static Stream<Named<String>> invalidCreateRequests() {
    String longUrl =
        "https://example.com/"
            + "x".repeat(JOB_POSTING_URL_MAX_LENGTH - "https://example.com/".length() + 1);

    return Stream.of(
        Named.of("missing companyName", "{\"positionTitle\":\"Java Developer\"}"),
        Named.of(
            "blank positionTitle", "{\"companyName\":\"Example Ltd.\",\"positionTitle\":\" \"}"),
        Named.of(
            "companyName too long",
            """
            {"companyName":"%s","positionTitle":"Java Developer"}
            """
                .formatted("x".repeat(COMPANY_NAME_MAX_LENGTH + 1))),
        Named.of(
            "relative jobPostingUrl",
            """
            {"companyName":"Example Ltd.","positionTitle":"Java Developer","jobPostingUrl":"/jobs/42"}
            """),
        Named.of(
            "non-HTTP jobPostingUrl",
            """
            {"companyName":"Example Ltd.","positionTitle":"Java Developer","jobPostingUrl":"ftp://example.com/jobs/42"}
            """),
        Named.of(
            "jobPostingUrl too long",
            """
            {"companyName":"Example Ltd.","positionTitle":"Java Developer","jobPostingUrl":"%s"}
            """
                .formatted(longUrl)),
        Named.of(
            "next action description without due date",
            """
            {"companyName":"Example Ltd.","positionTitle":"Java Developer","nextActionDescription":"Contact recruiter"}
            """),
        Named.of(
            "next action due date without description",
            """
            {"companyName":"Example Ltd.","positionTitle":"Java Developer","nextActionDueAt":"2026-08-18T12:00:00Z"}
            """));
  }

  private void assertDatabaseIsEmpty() {
    assertThat(jobApplicationRepository.count()).isZero();
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM status_changes", Long.class))
        .isZero();
  }
}
