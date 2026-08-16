package io.github.seremark.jobapplicationtracker.applications.web.validation;

import java.time.Instant;

public interface NextActionFields {

  String nextActionDescription();

  Instant nextActionDueAt();
}
