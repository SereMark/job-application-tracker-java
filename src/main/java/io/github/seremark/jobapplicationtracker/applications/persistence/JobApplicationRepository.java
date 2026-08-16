package io.github.seremark.jobapplicationtracker.applications.persistence;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobApplicationRepository
    extends JpaRepository<JobApplication, UUID>, JpaSpecificationExecutor<JobApplication> {

  @Query(
      """
      SELECT new io.github.seremark.jobapplicationtracker.applications.persistence.JobApplicationSummaryRow(
          application.status,
          COUNT(application),
          SUM(CASE WHEN application.nextActionDueAt < :now THEN 1 ELSE 0 END),
          SUM(CASE WHEN application.nextActionDueAt >= :now
              AND application.nextActionDueAt <= :windowEnd THEN 1 ELSE 0 END))
      FROM JobApplication application
      GROUP BY application.status
      """)
  List<JobApplicationSummaryRow> summarizeByStatus(
      @Param("now") Instant now, @Param("windowEnd") Instant windowEnd);
}
