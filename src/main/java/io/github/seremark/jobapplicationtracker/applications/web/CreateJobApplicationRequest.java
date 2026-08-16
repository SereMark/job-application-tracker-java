package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.COMPANY_NAME_MAX_LENGTH;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.LOCATION_MAX_LENGTH;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.NEXT_ACTION_DESCRIPTION_MAX_LENGTH;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.NOTES_MAX_LENGTH;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.POSITION_TITLE_MAX_LENGTH;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.SOURCE_MAX_LENGTH;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.applications.web.validation.ValidJobPostingUrl;
import io.github.seremark.jobapplicationtracker.applications.web.validation.ValidNextAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;

@ValidNextAction
public record CreateJobApplicationRequest(
    @NotBlank @Size(max = COMPANY_NAME_MAX_LENGTH) String companyName,
    @NotBlank @Size(max = POSITION_TITLE_MAX_LENGTH) String positionTitle,
    JobApplicationStatus status,
    @ValidJobPostingUrl URI jobPostingUrl,
    @Size(max = SOURCE_MAX_LENGTH) String source,
    @Size(max = LOCATION_MAX_LENGTH) String location,
    LocalDate appliedOn,
    @Size(max = NOTES_MAX_LENGTH) String notes,
    @Size(max = NEXT_ACTION_DESCRIPTION_MAX_LENGTH) String nextActionDescription,
    Instant nextActionDueAt) {}
