package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.APPLIED;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.INTERVIEW;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.SAVED;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.support.PostgreSqlIntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

class GetJobApplicationsIT extends PostgreSqlIntegrationTest {

  private static final Instant BASE_TIME = Instant.parse("2026-08-01T08:00:00Z");

  @Test
  void queryUsesDefaultSortAndPagination() throws Exception {
    List<JobApplication> applications =
        IntStream.range(0, 22)
            .mapToObj(
                index ->
                    createApplication(
                        "Company %02d".formatted(index),
                        "Java Developer",
                        BASE_TIME.plusSeconds(index * 3_600L),
                        SAVED,
                        null,
                        null,
                        null))
            .toList();
    jobApplicationRepository.saveAllAndFlush(applications);

    mockMvc
        .perform(get("/api/applications"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.pageSize").value(GetJobApplicationsQuery.DEFAULT_PAGE_SIZE))
        .andExpect(jsonPath("$.totalCount").value(22))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.items.length()").value(20))
        .andExpect(jsonPath("$.items[0].companyName").value("Company 21"))
        .andExpect(jsonPath("$.items[19].companyName").value("Company 02"));

    mockMvc
        .perform(get("/api/applications").queryParam("page", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].companyName").value("Company 01"))
        .andExpect(jsonPath("$.items[1].companyName").value("Company 00"));
  }

  @Test
  void queryCombinesCaseInsensitiveSearchAndFilters() throws Exception {
    JobApplication expected =
        createApplication(
            "Acme Cloud",
            "Platform Engineer",
            BASE_TIME,
            APPLIED,
            "LinkedIn",
            LocalDate.of(2026, 8, 10),
            Instant.parse("2026-08-20T12:00:00Z"));
    List<JobApplication> applications =
        List.of(
            expected,
            createApplication(
                "Late Action Ltd.",
                "Platform Engineer",
                BASE_TIME.plusSeconds(3_600),
                APPLIED,
                "LinkedIn",
                LocalDate.of(2026, 8, 10),
                Instant.parse("2026-08-25T12:00:00Z")),
            createApplication(
                "Old Application Ltd.",
                "Platform Engineer",
                BASE_TIME.plusSeconds(7_200),
                APPLIED,
                "LinkedIn",
                LocalDate.of(2026, 8, 1),
                Instant.parse("2026-08-20T12:00:00Z")),
            createApplication(
                "Different Status Ltd.",
                "Platform Engineer",
                BASE_TIME.plusSeconds(10_800),
                INTERVIEW,
                "LinkedIn",
                LocalDate.of(2026, 8, 10),
                Instant.parse("2026-08-20T12:00:00Z")),
            createApplication(
                "Different Source Ltd.",
                "Platform Engineer",
                BASE_TIME.plusSeconds(14_400),
                APPLIED,
                "Company website",
                LocalDate.of(2026, 8, 10),
                Instant.parse("2026-08-20T12:00:00Z")),
            createApplication(
                "Different Role Ltd.",
                "Product Designer",
                BASE_TIME.plusSeconds(18_000),
                APPLIED,
                "LinkedIn",
                LocalDate.of(2026, 8, 10),
                Instant.parse("2026-08-20T12:00:00Z")));
    jobApplicationRepository.saveAllAndFlush(applications);

    mockMvc
        .perform(
            get("/api/applications")
                .queryParam("search", "ENGINEER")
                .queryParam("status", "applied")
                .queryParam("source", "  LinkedIn  ")
                .queryParam("appliedFrom", "2026-08-05")
                .queryParam("appliedTo", "2026-08-15")
                .queryParam("nextActionBefore", "2026-08-21T00:00:00+02:00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(expected.getId().toString()))
        .andExpect(jsonPath("$.totalCount").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void queryTreatsLikeWildcardsAsLiteralSearchText() throws Exception {
    jobApplicationRepository.saveAllAndFlush(
        List.of(
            createApplication("100% Talent", "Java Developer", BASE_TIME),
            createApplication("100X Talent", "Java Developer", BASE_TIME.plusSeconds(1)),
            createApplication("Under_score", "Java Developer", BASE_TIME.plusSeconds(2)),
            createApplication("UnderXscore", "Java Developer", BASE_TIME.plusSeconds(3))));

    mockMvc
        .perform(get("/api/applications").queryParam("search", "%"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].companyName").value("100% Talent"));

    mockMvc
        .perform(get("/api/applications").queryParam("search", "_"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].companyName").value("Under_score"));
  }

  @Test
  void querySortsOnlyByAllowedFieldAndDirection() throws Exception {
    jobApplicationRepository.saveAllAndFlush(
        List.of(
            createApplication("Charlie Ltd.", "Java Developer", BASE_TIME),
            createApplication("Alpha Ltd.", "Java Developer", BASE_TIME.plusSeconds(1)),
            createApplication("Bravo Ltd.", "Java Developer", BASE_TIME.plusSeconds(2))));

    mockMvc
        .perform(
            get("/api/applications")
                .queryParam("sortBy", "companyname")
                .queryParam("sortDirection", "ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].companyName").value("Alpha Ltd."))
        .andExpect(jsonPath("$.items[1].companyName").value("Bravo Ltd."))
        .andExpect(jsonPath("$.items[2].companyName").value("Charlie Ltd."));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "updatedAt",
        "createdAt",
        "companyName",
        "positionTitle",
        "appliedOn",
        "nextActionDueAt"
      })
  void queryAcceptsEveryAllowedSortField(String sortBy) throws Exception {
    mockMvc
        .perform(get("/api/applications").queryParam("sortBy", sortBy))
        .andExpect(status().isOk());
  }

  @Test
  void queryUsesIdAsStableSecondarySort() throws Exception {
    List<UUID> ids =
        List.of(
            UUID.fromString("0198b8c4-8a04-7000-8000-000000000001"),
            UUID.fromString("0198b8c4-8a04-7000-8000-000000000002"),
            UUID.fromString("0198b8c4-8a04-7000-8000-000000000003"),
            UUID.fromString("0198b8c4-8a04-7000-8000-000000000004"));
    ids.forEach(this::insertApplicationWithSharedUpdatedAt);

    mockMvc
        .perform(get("/api/applications").queryParam("pageSize", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(ids.get(3).toString()))
        .andExpect(jsonPath("$.items[1].id").value(ids.get(2).toString()));

    mockMvc
        .perform(get("/api/applications").queryParam("page", "2").queryParam("pageSize", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(ids.get(1).toString()))
        .andExpect(jsonPath("$.items[1].id").value(ids.get(0).toString()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "?page=0",
        "?pageSize=101",
        "?page=2147483647&pageSize=100",
        "?appliedFrom=2026-08-20&appliedTo=2026-08-10",
        "?appliedFrom=not-a-date",
        "?nextActionBefore=not-a-date",
        "?page=not-a-number",
        "?status=Unknown",
        "?status=999",
        "?sortBy=NotAllowed",
        "?sortDirection=Sideways"
      })
  void queryWithInvalidParametersReturnsProblemDetail(String queryString) throws Exception {
    mockMvc
        .perform(get("/api/applications" + queryString))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Request validation failed"))
        .andExpect(jsonPath("$.errors").isNotEmpty());
  }

  @Test
  void openApiDocumentsQueryContract() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications'].get.responses['200'].content['application/json'].schema")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications'].get.responses['400'].content['application/problem+json'].schema")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/applications'].get.parameters[*].name")
                .value(
                    hasItems(
                        "search",
                        "status",
                        "source",
                        "appliedFrom",
                        "appliedTo",
                        "nextActionBefore",
                        "page",
                        "pageSize",
                        "sortBy",
                        "sortDirection")));
  }

  private JobApplication createApplication(
      String companyName, String positionTitle, Instant createdAt) {
    return createApplication(companyName, positionTitle, createdAt, SAVED, null, null, null);
  }

  private JobApplication createApplication(
      String companyName,
      String positionTitle,
      Instant createdAt,
      JobApplicationStatus status,
      String source,
      LocalDate appliedOn,
      Instant nextActionDueAt) {
    testClock.setInstant(createdAt);
    return JobApplication.create(
        new JobApplicationDetails(
            companyName,
            positionTitle,
            null,
            source,
            null,
            appliedOn,
            null,
            nextActionDueAt == null ? null : "Follow up",
            nextActionDueAt),
        status,
        testClock);
  }

  private void insertApplicationWithSharedUpdatedAt(UUID id) {
    jdbcTemplate.update(
        """
        INSERT INTO job_applications (
            id, company_name, position_title, status, created_at, updated_at
        ) VALUES (?, 'Same company', 'Java Developer', 'SAVED',
            '2026-08-01T08:00:00Z', '2026-08-01T08:00:00Z')
        """,
        id);
  }
}
