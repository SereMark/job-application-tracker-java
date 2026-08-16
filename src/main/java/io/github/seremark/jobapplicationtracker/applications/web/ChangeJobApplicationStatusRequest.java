package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.StatusChange.NOTE_MAX_LENGTH;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeJobApplicationStatusRequest(
    @NotNull JobApplicationStatus status, @Size(max = NOTE_MAX_LENGTH) String note) {}
