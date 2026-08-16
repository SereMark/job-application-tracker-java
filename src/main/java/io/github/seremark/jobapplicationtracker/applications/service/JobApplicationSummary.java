package io.github.seremark.jobapplicationtracker.applications.service;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import java.util.List;

public record JobApplicationSummary(
    long totalCount,
    List<StatusCount> statusCounts,
    long overdueNextActionCount,
    long nextActionDueWithinSevenDaysCount) {

  public JobApplicationSummary {
    statusCounts = List.copyOf(statusCounts);
  }

  public record StatusCount(JobApplicationStatus status, long count) {}
}
