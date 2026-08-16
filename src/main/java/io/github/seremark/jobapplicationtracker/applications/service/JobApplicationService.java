package io.github.seremark.jobapplicationtracker.applications.service;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationDetails;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.applications.persistence.JobApplicationRepository;
import java.time.Clock;
import java.util.UUID;
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
}
