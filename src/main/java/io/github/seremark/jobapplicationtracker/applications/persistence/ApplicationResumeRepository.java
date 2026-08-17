package io.github.seremark.jobapplicationtracker.applications.persistence;

import io.github.seremark.jobapplicationtracker.applications.domain.ApplicationResume;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationResumeRepository extends JpaRepository<ApplicationResume, UUID> {}
