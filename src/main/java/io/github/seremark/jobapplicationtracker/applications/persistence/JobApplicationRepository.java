package io.github.seremark.jobapplicationtracker.applications.persistence;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobApplicationRepository
    extends JpaRepository<JobApplication, UUID>, JpaSpecificationExecutor<JobApplication> {}
