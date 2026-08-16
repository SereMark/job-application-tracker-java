package io.github.seremark.jobapplicationtracker.applications.web;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobApplicationResponse(
    UUID id,
    String companyName,
    String positionTitle,
    URI jobPostingUrl,
    String source,
    String location,
    JobApplicationStatus status,
    LocalDate appliedOn,
    String notes,
    String nextActionDescription,
    Instant nextActionDueAt,
    Instant createdAt,
    Instant updatedAt) {}
