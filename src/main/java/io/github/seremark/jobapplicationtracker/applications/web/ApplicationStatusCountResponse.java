package io.github.seremark.jobapplicationtracker.applications.web;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;

public record ApplicationStatusCountResponse(JobApplicationStatus status, long count) {}
