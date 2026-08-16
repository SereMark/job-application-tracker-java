package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.SAVED;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;

final class JobApplicationMapper {

  private JobApplicationMapper() {}

  static JobApplicationDetails toDetails(CreateJobApplicationRequest request) {
    return new JobApplicationDetails(
        request.companyName(),
        request.positionTitle(),
        request.jobPostingUrl(),
        request.source(),
        request.location(),
        request.appliedOn(),
        request.notes(),
        request.nextActionDescription(),
        request.nextActionDueAt());
  }

  static JobApplicationStatus toInitialStatus(CreateJobApplicationRequest request) {
    return request.status() == null ? SAVED : request.status();
  }

  static JobApplicationResponse toResponse(JobApplication application) {
    return new JobApplicationResponse(
        application.getId(),
        application.getCompanyName(),
        application.getPositionTitle(),
        application.getJobPostingUrl(),
        application.getSource(),
        application.getLocation(),
        application.getStatus(),
        application.getAppliedOn(),
        application.getNotes(),
        application.getNextActionDescription(),
        application.getNextActionDueAt(),
        application.getCreatedAt(),
        application.getUpdatedAt());
  }
}
