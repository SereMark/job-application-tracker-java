package io.github.seremark.jobapplicationtracker.applications.service;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.applications.persistence.JobApplicationRepository;
import io.github.seremark.jobapplicationtracker.applications.persistence.JobApplicationSpecifications;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobApplicationService {

  private final JobApplicationRepository jobApplicationRepository;
  private final Clock clock;

  public JobApplicationService(JobApplicationRepository jobApplicationRepository, Clock clock) {
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
    return jobApplicationRepository
        .findById(id)
        .orElseThrow(() -> new JobApplicationNotFoundException(id));
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
}
