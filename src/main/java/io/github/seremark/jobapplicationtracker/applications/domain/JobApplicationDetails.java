package io.github.seremark.jobapplicationtracker.applications.domain;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;

public record JobApplicationDetails(
    String companyName,
    String positionTitle,
    URI jobPostingUrl,
    String source,
    String location,
    LocalDate appliedOn,
    String notes,
    String nextActionDescription,
    Instant nextActionDueAt) {

  public JobApplicationDetails(String companyName, String positionTitle) {
    this(companyName, positionTitle, null, null, null, null, null, null, null);
  }
}
