package io.github.seremark.jobapplicationtracker.applications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationResumeTests {

  private static final Instant UPLOADED_AT = Instant.parse("2026-08-17T09:30:00Z");

  @Test
  void createWithValidFileStoresAnIndependentCopy() {
    UUID applicationId = UUID.fromString("0198b8c4-8a04-7000-8000-000000000010");
    byte[] content = "%PDF-1.7 resume".getBytes(StandardCharsets.UTF_8);

    ApplicationResume resume =
        ApplicationResume.create(
            applicationId, "  Mark-Resume.pdf  ", "  application/pdf  ", content, UPLOADED_AT);
    content[0] = 0;

    assertThat(resume.getJobApplicationId()).isEqualTo(applicationId);
    assertThat(resume.getFileName()).isEqualTo("Mark-Resume.pdf");
    assertThat(resume.getContentType()).isEqualTo("application/pdf");
    assertThat(resume.getContent()[0]).isEqualTo((byte) '%');
    assertThat(resume.getUploadedAt()).isEqualTo(UPLOADED_AT);
  }

  @Test
  void replaceUpdatesTheStoredFile() {
    ApplicationResume resume = createResume();
    byte[] replacement = "PK replacement docx".getBytes(StandardCharsets.UTF_8);

    resume.replace(
        "Mark-Resume.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        replacement,
        UPLOADED_AT.plusSeconds(3_600));

    assertThat(resume.getFileName()).isEqualTo("Mark-Resume.docx");
    assertThat(resume.getContent()).isEqualTo(replacement);
    assertThat(resume.getUploadedAt()).isEqualTo(UPLOADED_AT.plusSeconds(3_600));
  }

  @Test
  void createWithEmptyContentThrowsIllegalArgumentException() {
    assertThatThrownBy(
            () ->
                ApplicationResume.create(
                    UUID.fromString("0198b8c4-8a04-7000-8000-000000000010"),
                    "Mark-Resume.pdf",
                    "application/pdf",
                    new byte[0],
                    UPLOADED_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static ApplicationResume createResume() {
    return ApplicationResume.create(
        UUID.fromString("0198b8c4-8a04-7000-8000-000000000010"),
        "Mark-Resume.pdf",
        "application/pdf",
        "%PDF-1.7 resume".getBytes(StandardCharsets.UTF_8),
        UPLOADED_AT);
  }
}
