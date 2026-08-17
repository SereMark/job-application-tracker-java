package io.github.seremark.jobapplicationtracker.applications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "application_resumes")
public class ApplicationResume {

  public static final int FILE_NAME_MAX_LENGTH = 255;
  public static final int CONTENT_TYPE_MAX_LENGTH = 100;
  public static final int MAX_FILE_SIZE = 5 * 1_024 * 1_024;

  @Id
  @Column(name = "job_application_id", nullable = false, updatable = false)
  private UUID jobApplicationId;

  @Column(name = "file_name", nullable = false, length = FILE_NAME_MAX_LENGTH)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = CONTENT_TYPE_MAX_LENGTH)
  private String contentType;

  @Column(nullable = false, columnDefinition = "bytea")
  private byte[] content;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;

  protected ApplicationResume() {}

  private ApplicationResume(
      UUID jobApplicationId,
      String fileName,
      String contentType,
      byte[] content,
      Instant uploadedAt) {
    this.jobApplicationId = Objects.requireNonNull(jobApplicationId, "jobApplicationId");
    replace(fileName, contentType, content, uploadedAt);
  }

  public static ApplicationResume create(
      UUID jobApplicationId,
      String fileName,
      String contentType,
      byte[] content,
      Instant uploadedAt) {
    return new ApplicationResume(jobApplicationId, fileName, contentType, content, uploadedAt);
  }

  public void replace(String fileName, String contentType, byte[] content, Instant uploadedAt) {
    this.fileName = validateRequiredText(fileName, FILE_NAME_MAX_LENGTH, "fileName");
    this.contentType = validateRequiredText(contentType, CONTENT_TYPE_MAX_LENGTH, "contentType");
    Objects.requireNonNull(content, "content");

    if (content.length == 0 || content.length > MAX_FILE_SIZE) {
      throw new IllegalArgumentException(
          "Resume content must contain between 1 and " + MAX_FILE_SIZE + " bytes.");
    }

    Instant normalizedUploadTime =
        Objects.requireNonNull(uploadedAt, "uploadedAt").truncatedTo(ChronoUnit.MICROS);

    if (normalizedUploadTime.isBefore(Instant.EPOCH)) {
      throw new IllegalArgumentException("The upload time cannot be before the Unix epoch.");
    }

    this.content = Arrays.copyOf(content, content.length);
    this.uploadedAt = normalizedUploadTime;
  }

  public UUID getJobApplicationId() {
    return jobApplicationId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public byte[] getContent() {
    return Arrays.copyOf(content, content.length);
  }

  public int getSize() {
    return content.length;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  private static String validateRequiredText(String value, int maxLength, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required.");
    }

    String normalizedValue = value.strip();

    if (normalizedValue.length() > maxLength) {
      throw new IllegalArgumentException(
          fieldName + " cannot exceed " + maxLength + " characters.");
    }

    return normalizedValue;
  }
}
