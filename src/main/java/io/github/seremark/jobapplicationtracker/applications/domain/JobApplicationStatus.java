package io.github.seremark.jobapplicationtracker.applications.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JobApplicationStatus {
  SAVED("Saved"),
  APPLIED("Applied"),
  SCREENING("Screening"),
  INTERVIEW("Interview"),
  OFFER("Offer"),
  REJECTED("Rejected"),
  WITHDRAWN("Withdrawn");

  public static final int DATABASE_VALUE_MAX_LENGTH = 20;

  private final String jsonValue;

  JobApplicationStatus(String jsonValue) {
    this.jsonValue = jsonValue;
  }

  @JsonCreator
  public static JobApplicationStatus fromJsonValue(String value) {
    for (JobApplicationStatus status : values()) {
      if (status.jsonValue.equals(value)) {
        return status;
      }
    }

    throw new IllegalArgumentException("Unsupported job application status: " + value);
  }

  @JsonValue
  public String toJsonValue() {
    return jsonValue;
  }
}
