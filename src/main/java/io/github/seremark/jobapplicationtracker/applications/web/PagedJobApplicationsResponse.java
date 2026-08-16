package io.github.seremark.jobapplicationtracker.applications.web;

import java.util.List;

public record PagedJobApplicationsResponse(
    List<JobApplicationResponse> items, int page, int pageSize, long totalCount, int totalPages) {}
