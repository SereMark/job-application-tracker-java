package io.github.seremark.jobapplicationtracker.applications.domain;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JobApplication {

  public static final int COMPANY_NAME_MAX_LENGTH = 200;
  public static final int POSITION_TITLE_MAX_LENGTH = 200;
  public static final int JOB_POSTING_URL_MAX_LENGTH = 2_048;
  public static final int SOURCE_MAX_LENGTH = 100;
  public static final int LOCATION_MAX_LENGTH = 200;
  public static final int NOTES_MAX_LENGTH = 4_000;
  public static final int NEXT_ACTION_DESCRIPTION_MAX_LENGTH = 500;

  private final List<StatusChange> statusHistory = new ArrayList<>();

  private String companyName;
  private String positionTitle;
  private URI jobPostingUrl;
  private String source;
  private String location;
  private LocalDate appliedOn;
  private String notes;
  private String nextActionDescription;
  private Instant nextActionDueAt;
  private JobApplicationStatus status;
  private final Instant createdAt;
  private Instant updatedAt;

  private JobApplication(
      JobApplicationDetails details, JobApplicationStatus initialStatus, Instant createdAt) {
    applyDetails(details);
    status = initialStatus;
    this.createdAt = createdAt;
    updatedAt = createdAt;
    statusHistory.add(StatusChange.create(null, initialStatus, createdAt, null));
  }

  public static JobApplication create(
      JobApplicationDetails details, JobApplicationStatus initialStatus, Clock clock) {
    JobApplicationDetails normalizedDetails = normalizeDetails(details);
    JobApplicationStatus validatedStatus = Objects.requireNonNull(initialStatus, "initialStatus");
    Instant createdAt = currentTime(clock);

    return new JobApplication(normalizedDetails, validatedStatus, createdAt);
  }

  public boolean updateDetails(JobApplicationDetails details, Clock clock) {
    JobApplicationDetails normalizedDetails = normalizeDetails(details);

    if (hasSameDetails(normalizedDetails)) {
      return false;
    }

    Instant updateTime = currentTime(clock);
    ensureNotBeforeCurrentState(updateTime);

    applyDetails(normalizedDetails);
    updatedAt = updateTime;

    return true;
  }

  public boolean changeStatus(JobApplicationStatus newStatus, String note, Clock clock) {
    JobApplicationStatus validatedStatus = Objects.requireNonNull(newStatus, "newStatus");
    String normalizedNote = normalizeOptional(note, StatusChange.NOTE_MAX_LENGTH, "note");

    if (validatedStatus == status) {
      return false;
    }

    Instant changedAt = currentTime(clock);
    ensureNotBeforeCurrentState(changedAt);
    StatusChange statusChange =
        StatusChange.create(status, validatedStatus, changedAt, normalizedNote);

    status = validatedStatus;
    updatedAt = changedAt;
    statusHistory.add(statusChange);

    return true;
  }

  public String getCompanyName() {
    return companyName;
  }

  public String getPositionTitle() {
    return positionTitle;
  }

  public URI getJobPostingUrl() {
    return jobPostingUrl;
  }

  public String getSource() {
    return source;
  }

  public String getLocation() {
    return location;
  }

  public LocalDate getAppliedOn() {
    return appliedOn;
  }

  public String getNotes() {
    return notes;
  }

  public String getNextActionDescription() {
    return nextActionDescription;
  }

  public Instant getNextActionDueAt() {
    return nextActionDueAt;
  }

  public JobApplicationStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<StatusChange> getStatusHistory() {
    return List.copyOf(statusHistory);
  }

  private static JobApplicationDetails normalizeDetails(JobApplicationDetails details) {
    Objects.requireNonNull(details, "details");

    String companyName =
        normalizeRequired(details.companyName(), COMPANY_NAME_MAX_LENGTH, "companyName");
    String positionTitle =
        normalizeRequired(details.positionTitle(), POSITION_TITLE_MAX_LENGTH, "positionTitle");
    URI jobPostingUrl = validateJobPostingUrl(details.jobPostingUrl());
    String source = normalizeOptional(details.source(), SOURCE_MAX_LENGTH, "source");
    String location = normalizeOptional(details.location(), LOCATION_MAX_LENGTH, "location");
    String notes = normalizeOptional(details.notes(), NOTES_MAX_LENGTH, "notes");
    String nextActionDescription =
        normalizeOptional(
            details.nextActionDescription(),
            NEXT_ACTION_DESCRIPTION_MAX_LENGTH,
            "nextActionDescription");
    Instant nextActionDueAt = normalizeOptionalInstant(details.nextActionDueAt());

    if ((nextActionDescription == null) != (nextActionDueAt == null)) {
      throw new IllegalArgumentException(
          "Next action description and due date must either both be provided or both be omitted.");
    }

    return new JobApplicationDetails(
        companyName,
        positionTitle,
        jobPostingUrl,
        source,
        location,
        details.appliedOn(),
        notes,
        nextActionDescription,
        nextActionDueAt);
  }

  private static String normalizeRequired(String value, int maxLength, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required.");
    }

    String normalizedValue = value.strip();
    ensureMaximumLength(normalizedValue, maxLength, fieldName);
    return normalizedValue;
  }

  private static String normalizeOptional(String value, int maxLength, String fieldName) {
    if (value == null || value.isBlank()) {
      return null;
    }

    String normalizedValue = value.strip();
    ensureMaximumLength(normalizedValue, maxLength, fieldName);
    return normalizedValue;
  }

  private static void ensureMaximumLength(String value, int maxLength, String fieldName) {
    if (value.length() > maxLength) {
      throw new IllegalArgumentException(
          fieldName + " cannot exceed " + maxLength + " characters.");
    }
  }

  private static URI validateJobPostingUrl(URI jobPostingUrl) {
    if (jobPostingUrl == null) {
      return null;
    }

    String scheme = jobPostingUrl.getScheme();
    boolean hasHttpScheme =
        scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
    boolean hasHost = jobPostingUrl.getHost() != null && !jobPostingUrl.getHost().isBlank();

    if (!jobPostingUrl.isAbsolute() || !hasHttpScheme || !hasHost) {
      throw new IllegalArgumentException("jobPostingUrl must be an absolute HTTP or HTTPS URL.");
    }

    ensureMaximumLength(jobPostingUrl.toASCIIString(), JOB_POSTING_URL_MAX_LENGTH, "jobPostingUrl");
    return jobPostingUrl;
  }

  private static Instant normalizeOptionalInstant(Instant value) {
    return value == null ? null : value.truncatedTo(ChronoUnit.MICROS);
  }

  private static Instant currentTime(Clock clock) {
    Clock validatedClock = Objects.requireNonNull(clock, "clock");
    Instant time = validatedClock.instant().truncatedTo(ChronoUnit.MICROS);

    if (time.isBefore(Instant.EPOCH)) {
      throw new IllegalArgumentException("Event time cannot be before the Unix epoch.");
    }

    return time;
  }

  private void ensureNotBeforeCurrentState(Instant eventTime) {
    if (eventTime.isBefore(updatedAt)) {
      throw new IllegalArgumentException(
          "Event time cannot be earlier than the current state timestamp.");
    }
  }

  private boolean hasSameDetails(JobApplicationDetails details) {
    return companyName.equals(details.companyName())
        && positionTitle.equals(details.positionTitle())
        && Objects.equals(jobPostingUrl, details.jobPostingUrl())
        && Objects.equals(source, details.source())
        && Objects.equals(location, details.location())
        && Objects.equals(appliedOn, details.appliedOn())
        && Objects.equals(notes, details.notes())
        && Objects.equals(nextActionDescription, details.nextActionDescription())
        && Objects.equals(nextActionDueAt, details.nextActionDueAt());
  }

  private void applyDetails(JobApplicationDetails details) {
    companyName = details.companyName();
    positionTitle = details.positionTitle();
    jobPostingUrl = details.jobPostingUrl();
    source = details.source();
    location = details.location();
    appliedOn = details.appliedOn();
    notes = details.notes();
    nextActionDescription = details.nextActionDescription();
    nextActionDueAt = details.nextActionDueAt();
  }
}
