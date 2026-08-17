package io.github.seremark.jobapplicationtracker.applications.service;

import io.github.seremark.jobapplicationtracker.applications.domain.ApplicationResume;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.applications.domain.StatusChange;
import io.github.seremark.jobapplicationtracker.applications.persistence.ApplicationResumeRepository;
import io.github.seremark.jobapplicationtracker.applications.persistence.JobApplicationRepository;
import io.github.seremark.jobapplicationtracker.applications.persistence.JobApplicationSpecifications;
import io.github.seremark.jobapplicationtracker.applications.persistence.JobApplicationSummaryRow;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobApplicationService {

  private static final long SUMMARY_WINDOW_DAYS = 7;

  private final ApplicationResumeRepository applicationResumeRepository;
  private final JobApplicationRepository jobApplicationRepository;
  private final Clock clock;

  public JobApplicationService(
      ApplicationResumeRepository applicationResumeRepository,
      JobApplicationRepository jobApplicationRepository,
      Clock clock) {
    this.applicationResumeRepository = applicationResumeRepository;
    this.jobApplicationRepository = jobApplicationRepository;
    this.clock = clock;
  }

  @Transactional
  public JobApplication create(JobApplicationDetails details, JobApplicationStatus initialStatus) {
    JobApplication application = JobApplication.create(details, initialStatus, clock);
    return jobApplicationRepository.save(application);
  }

  @Transactional(readOnly = true)
  public JobApplication getById(UUID id) {
    return requireById(id);
  }

  @Transactional
  public ApplicationResume putResume(
      UUID jobApplicationId, String fileName, String contentType, byte[] content) {
    requireById(jobApplicationId);
    Instant uploadedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
    Optional<ApplicationResume> storedResume =
        applicationResumeRepository.findById(jobApplicationId);
    ApplicationResume resume;

    if (storedResume.isPresent()) {
      resume = storedResume.orElseThrow();
      resume.replace(fileName, contentType, content, uploadedAt);
    } else {
      resume =
          ApplicationResume.create(jobApplicationId, fileName, contentType, content, uploadedAt);
    }

    return applicationResumeRepository.save(resume);
  }

  @Transactional(readOnly = true)
  public ApplicationResume getResume(UUID jobApplicationId) {
    requireById(jobApplicationId);
    return applicationResumeRepository
        .findById(jobApplicationId)
        .orElseThrow(() -> new ApplicationResumeNotFoundException(jobApplicationId));
  }

  @Transactional
  public JobApplication update(UUID id, JobApplicationDetails details) {
    JobApplication application = requireById(id);
    application.updateDetails(details, clock);
    return application;
  }

  @Transactional
  public JobApplication changeStatus(UUID id, JobApplicationStatus newStatus, String note) {
    JobApplication application = requireById(id);

    if (!application.changeStatus(newStatus, note, clock)) {
      throw new JobApplicationStatusConflictException(id, newStatus);
    }

    return application;
  }

  @Transactional(readOnly = true)
  public List<StatusChange> getStatusHistory(UUID id) {
    return requireById(id).getStatusHistory();
  }

  @Transactional
  public void delete(UUID id) {
    jobApplicationRepository.delete(requireById(id));
  }

  @Transactional(readOnly = true)
  public JobApplicationSummary getSummary() {
    Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
    Instant windowEnd = now.plus(SUMMARY_WINDOW_DAYS, ChronoUnit.DAYS);
    List<JobApplicationSummaryRow> rows =
        jobApplicationRepository.summarizeByStatus(now, windowEnd);

    Map<JobApplicationStatus, Long> countsByStatus = new EnumMap<>(JobApplicationStatus.class);
    long totalCount = 0;
    long overdueNextActionCount = 0;
    long nextActionDueWithinSevenDaysCount = 0;

    for (JobApplicationSummaryRow row : rows) {
      countsByStatus.put(row.status(), row.totalCount());
      totalCount += row.totalCount();
      overdueNextActionCount += row.overdueNextActionCount();
      nextActionDueWithinSevenDaysCount += row.nextActionDueWithinSevenDaysCount();
    }

    List<JobApplicationSummary.StatusCount> statusCounts = new ArrayList<>();
    for (JobApplicationStatus status : JobApplicationStatus.values()) {
      statusCounts.add(
          new JobApplicationSummary.StatusCount(status, countsByStatus.getOrDefault(status, 0L)));
    }

    return new JobApplicationSummary(
        totalCount, statusCounts, overdueNextActionCount, nextActionDueWithinSevenDaysCount);
  }

  @Transactional(readOnly = true)
  public Page<JobApplication> query(JobApplicationQuery query) {
    PageRequest pageRequest = PageRequest.of(query.page() - 1, query.pageSize(), createSort(query));

    return jobApplicationRepository.findAll(
        JobApplicationSpecifications.matching(
            query.search(),
            query.status(),
            query.source(),
            query.appliedFrom(),
            query.appliedTo(),
            query.nextActionBefore()),
        pageRequest);
  }

  private static Sort createSort(JobApplicationQuery query) {
    TypedPropertyPath<JobApplication, ?> primaryProperty =
        switch (query.sortBy()) {
          case UPDATED_AT -> TypedPropertyPath.path(application -> application.getUpdatedAt());
          case CREATED_AT -> TypedPropertyPath.path(application -> application.getCreatedAt());
          case COMPANY_NAME -> TypedPropertyPath.path(application -> application.getCompanyName());
          case POSITION_TITLE ->
              TypedPropertyPath.path(application -> application.getPositionTitle());
          case APPLIED_ON -> TypedPropertyPath.path(application -> application.getAppliedOn());
          case NEXT_ACTION_DUE_AT ->
              TypedPropertyPath.path(application -> application.getNextActionDueAt());
        };
    TypedPropertyPath<JobApplication, UUID> idProperty =
        TypedPropertyPath.path(application -> application.getId());
    Sort.Direction direction =
        switch (query.sortDirection()) {
          case ASC -> Sort.Direction.ASC;
          case DESC -> Sort.Direction.DESC;
        };

    return Sort.by(direction, primaryProperty).and(Sort.by(direction, idProperty));
  }

  private JobApplication requireById(UUID id) {
    return jobApplicationRepository
        .findById(id)
        .orElseThrow(() -> new JobApplicationNotFoundException(id));
  }
}
