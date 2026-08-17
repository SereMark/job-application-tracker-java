package io.github.seremark.jobapplicationtracker.applications.service;

import java.io.Serial;
import java.util.UUID;

public final class ApplicationResumeNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public ApplicationResumeNotFoundException(UUID jobApplicationId) {
    super("Job application '" + jobApplicationId + "' does not have a stored resume.");
  }
}
