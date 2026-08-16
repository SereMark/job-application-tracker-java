package io.github.seremark.jobapplicationtracker.applications.persistence;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;

public record JobApplicationSummaryRow(
    JobApplicationStatus status,
    long totalCount,
    long overdueNextActionCount,
    long nextActionDueWithinSevenDaysCount) {}
