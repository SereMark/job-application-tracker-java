package io.github.seremark.jobapplicationtracker.applications.service;

import java.io.Serial;
import java.util.UUID;

public final class JobApplicationNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public JobApplicationNotFoundException(UUID id) {
    super("No job application with id '" + id + "' exists.");
  }
}
