package io.github.seremark.jobapplicationtracker.applications.web;

import java.util.List;

public record ApplicationSummaryResponse(
    long totalCount,
    List<ApplicationStatusCountResponse> statusCounts,
    long overdueNextActionCount,
    long nextActionDueWithinSevenDaysCount) {}
