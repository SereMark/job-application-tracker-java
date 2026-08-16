package io.github.seremark.jobapplicationtracker.applications.web;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record StatusChangeResponse(
    UUID id,
    JobApplicationStatus previousStatus,
    JobApplicationStatus newStatus,
    Instant changedAt,
    String note) {}
