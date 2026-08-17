package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus.SAVED;

import io.github.seremark.jobapplicationtracker.applications.domain.ApplicationResume;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.applications.domain.StatusChange;
import io.github.seremark.jobapplicationtracker.applications.service.JobApplicationSummary;
import java.util.List;
import org.springframework.data.domain.Page;

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

  static JobApplicationDetails toDetails(UpdateJobApplicationRequest request) {
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

  static ApplicationResumeResponse toResponse(ApplicationResume resume) {
    return new ApplicationResumeResponse(
        resume.getFileName(), resume.getContentType(), resume.getSize(), resume.getUploadedAt());
  }

  static PagedJobApplicationsResponse toPagedResponse(Page<JobApplication> applications) {
    return new PagedJobApplicationsResponse(
        applications.getContent().stream().map(JobApplicationMapper::toResponse).toList(),
        applications.getNumber() + 1,
        applications.getSize(),
        applications.getTotalElements(),
        applications.getTotalPages());
  }

  static StatusChangeResponse toResponse(StatusChange change) {
    return new StatusChangeResponse(
        change.getId(),
        change.getPreviousStatus(),
        change.getNewStatus(),
        change.getChangedAt(),
        change.getNote());
  }

  static ApplicationSummaryResponse toResponse(JobApplicationSummary summary) {
    List<ApplicationStatusCountResponse> statusCounts =
        summary.statusCounts().stream()
            .map(
                statusCount ->
                    new ApplicationStatusCountResponse(statusCount.status(), statusCount.count()))
            .toList();

    return new ApplicationSummaryResponse(
        summary.totalCount(),
        statusCounts,
        summary.overdueNextActionCount(),
        summary.nextActionDueWithinSevenDaysCount());
  }
}
