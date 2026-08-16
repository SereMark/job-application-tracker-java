package io.github.seremark.jobapplicationtracker.applications.domain;

import java.time.Instant;
import java.util.Objects;

public final class StatusChange {

  public static final int NOTE_MAX_LENGTH = 500;

  private final JobApplicationStatus previousStatus;
  private final JobApplicationStatus newStatus;
  private final Instant changedAt;
  private final String note;

  private StatusChange(
      JobApplicationStatus previousStatus,
      JobApplicationStatus newStatus,
      Instant changedAt,
      String note) {
    this.newStatus = Objects.requireNonNull(newStatus, "newStatus");

    if (previousStatus == this.newStatus) {
      throw new IllegalArgumentException("The previous and new statuses must be different.");
    }

    this.previousStatus = previousStatus;
    this.changedAt = Objects.requireNonNull(changedAt, "changedAt");
    this.note = note;
  }

  static StatusChange create(
      JobApplicationStatus previousStatus,
      JobApplicationStatus newStatus,
      Instant changedAt,
      String note) {
    return new StatusChange(previousStatus, newStatus, changedAt, note);
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
