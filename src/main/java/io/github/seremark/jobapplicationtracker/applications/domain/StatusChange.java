package io.github.seremark.jobapplicationtracker.applications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "status_changes")
public class StatusChange {

  public static final int NOTE_MAX_LENGTH = 500;

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "job_application_id", nullable = false, updatable = false)
  private JobApplication jobApplication;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", length = JobApplicationStatus.DATABASE_VALUE_MAX_LENGTH)
  private JobApplicationStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "new_status",
      nullable = false,
      length = JobApplicationStatus.DATABASE_VALUE_MAX_LENGTH)
  private JobApplicationStatus newStatus;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  @Column(length = NOTE_MAX_LENGTH)
  private String note;

  protected StatusChange() {}

  private StatusChange(
      JobApplication jobApplication,
      JobApplicationStatus previousStatus,
      JobApplicationStatus newStatus,
      Instant changedAt,
      String note) {
    this.jobApplication = Objects.requireNonNull(jobApplication, "jobApplication");
    this.newStatus = Objects.requireNonNull(newStatus, "newStatus");

    if (previousStatus == this.newStatus) {
      throw new IllegalArgumentException("The previous and new statuses must be different.");
    }

    this.previousStatus = previousStatus;
    this.changedAt = Objects.requireNonNull(changedAt, "changedAt");
    this.note = note;
  }

  static StatusChange create(
      JobApplication jobApplication,
      JobApplicationStatus previousStatus,
      JobApplicationStatus newStatus,
      Instant changedAt,
      String note) {
    return new StatusChange(jobApplication, previousStatus, newStatus, changedAt, note);
  }

  public UUID getId() {
    return id;
  }

  public JobApplicationStatus getPreviousStatus() {
    return previousStatus;
  }

  public JobApplicationStatus getNewStatus() {
    return newStatus;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public String getNote() {
    return note;
  }
}
