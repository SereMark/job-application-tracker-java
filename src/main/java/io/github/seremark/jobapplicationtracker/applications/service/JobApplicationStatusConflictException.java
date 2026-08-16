package io.github.seremark.jobapplicationtracker.applications.service;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

public final class JobApplicationStatusConflictException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public JobApplicationStatusConflictException(UUID id, JobApplicationStatus status) {
    super(
        "Job application '"
            + Objects.requireNonNull(id, "id")
            + "' already has status '"
            + Objects.requireNonNull(status, "status").toJsonValue()
            + "'.");
  }
}
