package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.INTERVIEW;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.SAVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.support.PostgreSqlIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class GetApplicationSummaryIT extends PostgreSqlIntegrationTest {

  private static final Instant NOW_RAW = Instant.parse("2026-08-16T12:00:00.123456789Z");
  private static final Instant NOW = NOW_RAW.truncatedTo(ChronoUnit.MICROS);
  private static final Instant CREATED_AT = NOW.minus(30, ChronoUnit.DAYS);

  @Test
  void getSummaryReturnsZeroCountsForEmptyDatabase() throws Exception {
    testClock.setInstant(NOW_RAW);

    MvcResult result =
        mockMvc
            .perform(get("/api/applications/summary"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.totalCount").value(0))
            .andExpect(jsonPath("$.overdueNextActionCount").value(0))
            .andExpect(jsonPath("$.nextActionDueWithinSevenDaysCount").value(0))
            .andReturn();

    String responseJson = result.getResponse().getContentAsString();
    List<String> statuses = JsonPath.read(responseJson, "$.statusCounts[*].status");
    List<Integer> counts = JsonPath.read(responseJson, "$.statusCounts[*].count");

    assertThat(statuses)
        .containsExactly(
            "Saved", "Applied", "Screening", "Interview", "Offer", "Rejected", "Withdrawn");
    assertThat(counts).containsExactly(0, 0, 0, 0, 0, 0, 0);
  }

  @Test
  void getSummaryCountsApplicationsByStatus() throws Exception {
    testClock.setInstant(CREATED_AT);
    List<JobApplication> applications = new ArrayList<>();
    for (JobApplicationStatus status : JobApplicationStatus.values()) {
      applications.add(createApplication(status, null));
    }
    applications.add(createApplication(SAVED, null));
    applications.add(createApplication(INTERVIEW, null));
    jobApplicationRepository.saveAllAndFlush(applications);
    testClock.setInstant(NOW_RAW);

    mockMvc
        .perform(get("/api/applications/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCount").value(9))
        .andExpect(jsonPath("$.statusCounts[0].status").value("Saved"))
        .andExpect(jsonPath("$.statusCounts[0].count").value(2))
        .andExpect(jsonPath("$.statusCounts[1].status").value("Applied"))
        .andExpect(jsonPath("$.statusCounts[1].count").value(1))
        .andExpect(jsonPath("$.statusCounts[2].status").value("Screening"))
        .andExpect(jsonPath("$.statusCounts[2].count").value(1))
        .andExpect(jsonPath("$.statusCounts[3].status").value("Interview"))
        .andExpect(jsonPath("$.statusCounts[3].count").value(2))
        .andExpect(jsonPath("$.statusCounts[4].status").value("Offer"))
        .andExpect(jsonPath("$.statusCounts[4].count").value(1))
        .andExpect(jsonPath("$.statusCounts[5].status").value("Rejected"))
        .andExpect(jsonPath("$.statusCounts[5].count").value(1))
        .andExpect(jsonPath("$.statusCounts[6].status").value("Withdrawn"))
        .andExpect(jsonPath("$.statusCounts[6].count").value(1));
  }

  @Test
  void getSummaryUsesExclusiveOverdueAndInclusiveSevenDayBoundaries() throws Exception {
    testClock.setInstant(CREATED_AT);
    jobApplicationRepository.saveAllAndFlush(
        List.of(
            createApplication(SAVED, NOW.minus(1, ChronoUnit.MICROS)),
            createApplication(SAVED, NOW),
            createApplication(SAVED, NOW.plus(7, ChronoUnit.DAYS)),
            createApplication(SAVED, NOW.plus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.MICROS)),
            createApplication(SAVED, null)));
    testClock.setInstant(NOW_RAW);

    mockMvc
        .perform(get("/api/applications/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCount").value(5))
        .andExpect(jsonPath("$.overdueNextActionCount").value(1))
        .andExpect(jsonPath("$.nextActionDueWithinSevenDaysCount").value(2));
  }

  @Test
  void openApiDocumentsSummaryResponse() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/summary'].get.responses['200'].content['application/json'].schema['$ref']")
                .value("#/components/schemas/ApplicationSummaryResponse"));
  }

  private JobApplication createApplication(JobApplicationStatus status, Instant nextActionDueAt) {
    JobApplicationDetails details =
        new JobApplicationDetails(
            status.toJsonValue() + " Example Ltd.",
            "Java Developer",
            null,
            null,
            null,
            null,
            null,
            nextActionDueAt == null ? null : "Follow up",
            nextActionDueAt);
    return JobApplication.create(details, status, testClock);
  }
}
