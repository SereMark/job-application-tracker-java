package io.github.seremark.jobapplicationtracker.applications.service;

import java.io.Serial;

public final class InvalidResumeFileException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public InvalidResumeFileException(String message) {
    super(message);
  }
}
