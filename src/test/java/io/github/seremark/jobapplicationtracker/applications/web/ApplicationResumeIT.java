package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.ApplicationResume.MAX_FILE_SIZE;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.APPLIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.seremark.jobapplicationtracker.applications.domain.ApplicationResume;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.support.PostgreSqlIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class ApplicationResumeIT extends PostgreSqlIntegrationTest {

  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

  @Test
  void uploadThenDownloadPersistsAndReturnsResume() throws Exception {
    UUID applicationId = persistApplication();
    byte[] resumeContent = "%PDF-1.7 portfolio resume".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = resumeFile("C:\\fakepath\\Mark-Resume.pdf", resumeContent);

    mockMvc
        .perform(
            multipart(HttpMethod.PUT, "/api/applications/{id}/resume", applicationId).file(file))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.fileName").value("Mark-Resume.pdf"))
        .andExpect(jsonPath("$.contentType").value(MediaType.APPLICATION_PDF_VALUE))
        .andExpect(jsonPath("$.size").value(resumeContent.length))
        .andExpect(jsonPath("$.uploadedAt").value(DEFAULT_TEST_INSTANT.toString()));

    mockMvc
        .perform(get("/api/applications/{id}/resume", applicationId))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    org.hamcrest.Matchers.containsString("Mark-Resume.pdf")))
        .andExpect(
            header()
                .dateValue(
                    HttpHeaders.LAST_MODIFIED, DEFAULT_TEST_INSTANT.getEpochSecond() * 1_000))
        .andExpect(content().bytes(resumeContent));

    ApplicationResume persistedResume =
        applicationResumeRepository.findById(applicationId).orElseThrow();

    assertThat(persistedResume.getFileName()).isEqualTo("Mark-Resume.pdf");
    assertThat(persistedResume.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
    assertThat(persistedResume.getContent()).isEqualTo(resumeContent);
    assertThat(persistedResume.getUploadedAt()).isEqualTo(DEFAULT_TEST_INSTANT);
  }

  @Test
  void uploadAgainReplacesPreviousResume() throws Exception {
    UUID applicationId = persistApplication();
    MockMultipartFile firstFile =
        resumeFile("First-Resume.pdf", "%PDF first".getBytes(StandardCharsets.UTF_8));
    mockMvc
        .perform(
            multipart(HttpMethod.PUT, "/api/applications/{id}/resume", applicationId)
                .file(firstFile))
        .andExpect(status().isOk());
    testClock.setInstant(DEFAULT_TEST_INSTANT.plusSeconds(3_600));
    byte[] replacementContent = "PK replacement".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile replacementFile = resumeFile("Tailored-Resume.docx", replacementContent);

    mockMvc
        .perform(
            multipart(HttpMethod.PUT, "/api/applications/{id}/resume", applicationId)
                .file(replacementFile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fileName").value("Tailored-Resume.docx"))
        .andExpect(jsonPath("$.contentType").value(DOCX_CONTENT_TYPE))
        .andExpect(
            jsonPath("$.uploadedAt").value(DEFAULT_TEST_INSTANT.plusSeconds(3_600).toString()));

    assertThat(applicationResumeRepository.count()).isOne();
    ApplicationResume replacement =
        applicationResumeRepository.findById(applicationId).orElseThrow();
    assertThat(replacement.getFileName()).isEqualTo("Tailored-Resume.docx");
    assertThat(replacement.getContent()).isEqualTo(replacementContent);
  }

  @Test
  void uploadUnsupportedFileReturnsValidationProblem() throws Exception {
    UUID applicationId = persistApplication();
    MockMultipartFile file =
        resumeFile("Resume.txt", "plain text".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart(HttpMethod.PUT, "/api/applications/{id}/resume", applicationId).file(file))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Request validation failed"))
        .andExpect(jsonPath("$.errors.file").isNotEmpty());

    assertThat(applicationResumeRepository.count()).isZero();
  }

  @Test
  void uploadOversizedFileReturnsValidationProblem() throws Exception {
    UUID applicationId = persistApplication();
    MockMultipartFile file = resumeFile("Resume.pdf", new byte[MAX_FILE_SIZE + 1]);

    mockMvc
        .perform(
            multipart(HttpMethod.PUT, "/api/applications/{id}/resume", applicationId).file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.file").isNotEmpty());
  }

  @Test
  void resumeEndpointsReturnNotFoundWhenResourceIsMissing() throws Exception {
    UUID applicationId = persistApplication();
    UUID unknownId = UUID.fromString("0198b8c4-8a04-7000-8000-000000000011");
    MockMultipartFile file =
        resumeFile("Resume.pdf", "%PDF resume".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart(HttpMethod.PUT, "/api/applications/{id}/resume", unknownId).file(file))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Job application not found"));

    mockMvc
        .perform(get("/api/applications/{id}/resume", applicationId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Application resume not found"));
  }

  @Test
  void deleteApplicationCascadesStoredResume() throws Exception {
    UUID applicationId = persistApplication();
    MockMultipartFile file =
        resumeFile("Resume.pdf", "%PDF resume".getBytes(StandardCharsets.UTF_8));
    mockMvc
        .perform(
            multipart(HttpMethod.PUT, "/api/applications/{id}/resume", applicationId).file(file))
        .andExpect(status().isOk());

    assertThat(applicationResumeRepository.existsById(applicationId)).isTrue();

    mockMvc
        .perform(delete("/api/applications/{id}", applicationId))
        .andExpect(status().isNoContent());

    assertThat(applicationResumeRepository.existsById(applicationId)).isFalse();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT delete_rule
                FROM information_schema.referential_constraints
                WHERE constraint_name = 'fk_application_resumes_job_application'
                """,
                String.class))
        .isEqualTo("CASCADE");
  }

  @Test
  void openApiDocumentsResumeEndpoints() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}/resume'].put.requestBody.content['multipart/form-data']")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}/resume'].put.responses['200'].content['application/json']")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/applications/{id}/resume'].get.responses['200'].content['application/octet-stream']")
                .exists());
  }

  private UUID persistApplication() {
    JobApplication application =
        JobApplication.create(
            new JobApplicationDetails("Example Ltd.", "Java Developer"), APPLIED, testClock);
    return jobApplicationRepository.saveAndFlush(application).getId();
  }

  private static MockMultipartFile resumeFile(String fileName, byte[] content) {
    return new MockMultipartFile(
        "file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, content);
  }
}
