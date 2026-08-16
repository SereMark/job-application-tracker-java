package io.github.seremark.jobapplicationtracker.applications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class JobApplicationTests {

  private static final Instant CREATED_AT_RAW = Instant.parse("2026-08-15T08:30:00.123456789Z");
  private static final Instant CREATED_AT = Instant.parse("2026-08-15T08:30:00.123456Z");
  private static final Instant CHANGED_AT_RAW = Instant.parse("2026-08-16T09:45:00.987654321Z");
  private static final Instant CHANGED_AT = Instant.parse("2026-08-16T09:45:00.987654Z");
  private static final Clock CREATED_CLOCK = clockAt(CREATED_AT_RAW);

  @Test
  void createWithValidDetailsNormalizesDataAndAddsInitialHistory() {
    var details =
        new JobApplicationDetails(
            "  Example Ltd.  ",
            "  Java Developer  ",
            URI.create("https://example.com/jobs/42"),
            "  LinkedIn  ",
            "  Budapest  ",
            LocalDate.of(2026, 8, 15),
            "  Referral from a former colleague.  ",
            "  Follow up with the recruiter  ",
            CHANGED_AT_RAW);

    JobApplication application =
        JobApplication.create(details, JobApplicationStatus.APPLIED, CREATED_CLOCK);

    assertThat(application.getCompanyName()).isEqualTo("Example Ltd.");
    assertThat(application.getPositionTitle()).isEqualTo("Java Developer");
    assertThat(application.getJobPostingUrl()).isEqualTo(URI.create("https://example.com/jobs/42"));
    assertThat(application.getSource()).isEqualTo("LinkedIn");
    assertThat(application.getLocation()).isEqualTo("Budapest");
    assertThat(application.getAppliedOn()).isEqualTo(LocalDate.of(2026, 8, 15));
    assertThat(application.getNotes()).isEqualTo("Referral from a former colleague.");
    assertThat(application.getNextActionDescription()).isEqualTo("Follow up with the recruiter");
    assertThat(application.getNextActionDueAt()).isEqualTo(CHANGED_AT);
    assertThat(application.getStatus()).isEqualTo(JobApplicationStatus.APPLIED);
    assertThat(application.getCreatedAt()).isEqualTo(CREATED_AT);
    assertThat(application.getUpdatedAt()).isEqualTo(CREATED_AT);

    assertThat(application.getStatusHistory())
        .singleElement()
        .satisfies(
            change -> {
              assertThat(change.getPreviousStatus()).isNull();
              assertThat(change.getNewStatus()).isEqualTo(JobApplicationStatus.APPLIED);
              assertThat(change.getChangedAt()).isEqualTo(CREATED_AT);
              assertThat(change.getNote()).isNull();
            });
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "\t"})
  void createWithBlankCompanyNameThrowsIllegalArgumentException(String companyName) {
    var details = new JobApplicationDetails(companyName, "Java Developer");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> JobApplication.create(details, JobApplicationStatus.SAVED, CREATED_CLOCK))
        .withMessageContaining("companyName");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/jobs/42", "ftp://example.com/jobs/42", "https:jobs/42"})
  void createWithInvalidJobPostingUrlThrowsIllegalArgumentException(String url) {
    JobApplicationDetails details = detailsWithJobPostingUrl(URI.create(url));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> JobApplication.create(details, JobApplicationStatus.SAVED, CREATED_CLOCK))
        .withMessageContaining("jobPostingUrl");
  }

  @Test
  void createWithBlankOptionalFieldsStoresNullValues() {
    var details =
        new JobApplicationDetails(
            "Example Ltd.", "Java Developer", null, " ", "\t", null, "\n", "  ", null);

    JobApplication application =
        JobApplication.create(details, JobApplicationStatus.SAVED, CREATED_CLOCK);

    assertThat(application.getSource()).isNull();
    assertThat(application.getLocation()).isNull();
    assertThat(application.getNotes()).isNull();
    assertThat(application.getNextActionDescription()).isNull();
    assertThat(application.getNextActionDueAt()).isNull();
  }

  @Test
  void createWithOverlongCompanyNameThrowsIllegalArgumentException() {
    var details =
        new JobApplicationDetails(
            "x".repeat(JobApplication.COMPANY_NAME_MAX_LENGTH + 1), "Java Developer");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> JobApplication.create(details, JobApplicationStatus.SAVED, CREATED_CLOCK))
        .withMessageContaining("companyName");
  }

  @Test
  void createWithIncompleteNextActionThrowsIllegalArgumentException() {
    var descriptionOnly =
        new JobApplicationDetails(
            "Example Ltd.",
            "Java Developer",
            null,
            null,
            null,
            null,
            null,
            "Contact the recruiter",
            null);
    var dueDateOnly =
        new JobApplicationDetails(
            "Example Ltd.", "Java Developer", null, null, null, null, null, null, CHANGED_AT_RAW);

    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> JobApplication.create(descriptionOnly, JobApplicationStatus.SAVED, CREATED_CLOCK))
        .withMessageContaining("both be provided");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> JobApplication.create(dueDateOnly, JobApplicationStatus.SAVED, CREATED_CLOCK))
        .withMessageContaining("both be provided");
  }

  @Test
  void createWithClockBeforeUnixEpochThrowsIllegalArgumentException() {
    Clock invalidClock = clockAt(Instant.EPOCH.minusNanos(1));

    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                JobApplication.create(
                    createValidDetails(), JobApplicationStatus.SAVED, invalidClock))
        .withMessageContaining("Unix epoch");
  }

  @Test
  void updateDetailsWithChangedDetailsReplacesEditableFields() {
    JobApplication application = createApplication();
    var changedDetails = new JobApplicationDetails("New Company", "Senior Java Developer");

    boolean updated = application.updateDetails(changedDetails, clockAt(CHANGED_AT_RAW));

    assertThat(updated).isTrue();
    assertThat(application.getCompanyName()).isEqualTo("New Company");
    assertThat(application.getPositionTitle()).isEqualTo("Senior Java Developer");
    assertThat(application.getJobPostingUrl()).isNull();
    assertThat(application.getSource()).isNull();
    assertThat(application.getLocation()).isNull();
    assertThat(application.getAppliedOn()).isNull();
    assertThat(application.getNotes()).isNull();
    assertThat(application.getStatus()).isEqualTo(JobApplicationStatus.SAVED);
    assertThat(application.getUpdatedAt()).isEqualTo(CHANGED_AT);
    assertThat(application.getStatusHistory()).hasSize(1);
  }

  @Test
  void updateDetailsWithEquivalentNormalizedDetailsDoesNotChangeTimestamp() {
    JobApplication application = createApplication();
    JobApplicationDetails equivalentDetails =
        new JobApplicationDetails(
            "  Example Ltd.  ",
            "  Java Developer  ",
            URI.create("https://example.com/jobs/42"),
            "  LinkedIn  ",
            "  Budapest  ",
            LocalDate.of(2026, 8, 15),
            "  Referral from a former colleague.  ",
            null,
            null);

    boolean updated = application.updateDetails(equivalentDetails, clockAt(CHANGED_AT_RAW));

    assertThat(updated).isFalse();
    assertThat(application.getUpdatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void updateDetailsWithEarlierClockThrowsWithoutChangingState() {
    JobApplication application = createApplication();
    var changedDetails = new JobApplicationDetails("New Company", "Java Developer");
    Clock earlierClock = clockAt(CREATED_AT.minusNanos(1_000));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> application.updateDetails(changedDetails, earlierClock))
        .withMessageContaining("earlier than");
    assertThat(application.getCompanyName()).isEqualTo("Example Ltd.");
    assertThat(application.getUpdatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void changeStatusWithDifferentStatusUpdatesApplicationAndAddsHistory() {
    JobApplication application = createApplication();

    boolean changed =
        application.changeStatus(
            JobApplicationStatus.SCREENING,
            "  Recruiter call completed.  ",
            clockAt(CHANGED_AT_RAW));

    assertThat(changed).isTrue();
    assertThat(application.getStatus()).isEqualTo(JobApplicationStatus.SCREENING);
    assertThat(application.getUpdatedAt()).isEqualTo(CHANGED_AT);
    assertThat(application.getStatusHistory())
        .hasSize(2)
        .last()
        .satisfies(
            change -> {
              assertThat(change.getPreviousStatus()).isEqualTo(JobApplicationStatus.SAVED);
              assertThat(change.getNewStatus()).isEqualTo(JobApplicationStatus.SCREENING);
              assertThat(change.getChangedAt()).isEqualTo(CHANGED_AT);
              assertThat(change.getNote()).isEqualTo("Recruiter call completed.");
            });
  }

  @Test
  void changeStatusWithCurrentStatusDoesNotChangeState() {
    JobApplication application = createApplication();

    boolean changed =
        application.changeStatus(JobApplicationStatus.SAVED, null, clockAt(CHANGED_AT_RAW));

    assertThat(changed).isFalse();
    assertThat(application.getUpdatedAt()).isEqualTo(CREATED_AT);
    assertThat(application.getStatusHistory()).hasSize(1);
  }

  @Test
  void changeStatusWithEarlierClockThrowsWithoutChangingState() {
    JobApplication application = createApplication();
    Clock earlierClock = clockAt(CREATED_AT.minusNanos(1_000));

    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> application.changeStatus(JobApplicationStatus.INTERVIEW, null, earlierClock))
        .withMessageContaining("earlier than");
    assertThat(application.getStatus()).isEqualTo(JobApplicationStatus.SAVED);
    assertThat(application.getStatusHistory()).hasSize(1);
  }

  @Test
  void changeStatusWithTooLongNoteThrowsWithoutChangingState() {
    JobApplication application = createApplication();
    String note = "x".repeat(StatusChange.NOTE_MAX_LENGTH + 1);

    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                application.changeStatus(
                    JobApplicationStatus.INTERVIEW, note, clockAt(CHANGED_AT_RAW)))
        .withMessageContaining("note");
    assertThat(application.getStatus()).isEqualTo(JobApplicationStatus.SAVED);
    assertThat(application.getStatusHistory()).hasSize(1);
  }

  @Test
  void statusHistoryCannotBeModifiedExternally() {
    JobApplication application = createApplication();
    List<StatusChange> statusHistory = application.getStatusHistory();

    assertThatThrownBy(statusHistory::clear).isInstanceOf(UnsupportedOperationException.class);
    assertThat(application.getStatusHistory()).hasSize(1);
  }

  private static JobApplication createApplication() {
    return JobApplication.create(createValidDetails(), JobApplicationStatus.SAVED, CREATED_CLOCK);
  }

  private static JobApplicationDetails createValidDetails() {
    return new JobApplicationDetails(
        "Example Ltd.",
        "Java Developer",
        URI.create("https://example.com/jobs/42"),
        "LinkedIn",
        "Budapest",
        LocalDate.of(2026, 8, 15),
        "Referral from a former colleague.",
        null,
        null);
  }

  private static JobApplicationDetails detailsWithJobPostingUrl(URI jobPostingUrl) {
    return new JobApplicationDetails(
        "Example Ltd.", "Java Developer", jobPostingUrl, null, null, null, null, null, null);
  }

  private static Clock clockAt(Instant instant) {
    return Clock.fixed(instant, ZoneOffset.UTC);
  }
}
