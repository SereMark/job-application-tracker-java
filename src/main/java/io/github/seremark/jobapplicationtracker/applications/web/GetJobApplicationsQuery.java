package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.COMPANY_NAME_MAX_LENGTH;
import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.SOURCE_MAX_LENGTH;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.applications.service.JobApplicationQuery;
import io.github.seremark.jobapplicationtracker.applications.service.JobApplicationQuery.SortDirection;
import io.github.seremark.jobapplicationtracker.applications.service.JobApplicationQuery.SortField;
import io.github.seremark.jobapplicationtracker.applications.web.validation.ValidJobApplicationQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;

@ValidJobApplicationQuery
public record GetJobApplicationsQuery(
    @Schema(description = "Literal text to find in company names or position titles")
        @Size(max = COMPANY_NAME_MAX_LENGTH) String search,
    @Schema(
            description = "Application status",
            allowableValues = {
              "Saved",
              "Applied",
              "Screening",
              "Interview",
              "Offer",
              "Rejected",
              "Withdrawn"
            })
        @Size(max = JobApplicationStatus.DATABASE_VALUE_MAX_LENGTH) String status,
    @Schema(description = "Exact application source") @Size(max = SOURCE_MAX_LENGTH) String source,
    @Schema(description = "Inclusive earliest application date", format = "date")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate appliedFrom,
    @Schema(description = "Inclusive latest application date", format = "date")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate appliedTo,
    @Schema(description = "Inclusive next-action deadline", format = "date-time")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant nextActionBefore,
    @Schema(description = "One-based page number", defaultValue = "1", minimum = "1") @Min(1) Integer page,
    @Schema(
            description = "Number of items per page",
            defaultValue = "20",
            minimum = "1",
            maximum = "100")
        @Min(1) @Max(MAX_PAGE_SIZE) Integer pageSize,
    @Schema(
            description = "Allowed sort field",
            defaultValue = "updatedAt",
            allowableValues = {
              "updatedAt",
              "createdAt",
              "companyName",
              "positionTitle",
              "appliedOn",
              "nextActionDueAt"
            })
        @Size(max = 50) String sortBy,
    @Schema(
            description = "Sort direction",
            defaultValue = "desc",
            allowableValues = {"asc", "desc"})
        @Size(max = 10) String sortDirection) {

  public static final int DEFAULT_PAGE = 1;
  public static final int DEFAULT_PAGE_SIZE = 20;
  public static final int MAX_PAGE_SIZE = JobApplicationQuery.MAX_PAGE_SIZE;

  JobApplicationQuery toServiceQuery() {
    return new JobApplicationQuery(
        search,
        resolvedStatus().orElse(null),
        source,
        appliedFrom,
        appliedTo,
        nextActionBefore,
        resolvedPage(),
        resolvedPageSize(),
        resolvedSortBy().orElseThrow(),
        resolvedSortDirection().orElseThrow());
  }

  public Optional<JobApplicationStatus> resolvedStatus() {
    if (status == null || status.isBlank()) {
      return Optional.empty();
    }

    String normalizedStatus = status.strip();
    return Arrays.stream(JobApplicationStatus.values())
        .filter(candidate -> candidate.toJsonValue().equalsIgnoreCase(normalizedStatus))
        .findFirst();
  }

  public Optional<SortField> resolvedSortBy() {
    if (sortBy == null || sortBy.isBlank()) {
      return Optional.of(SortField.UPDATED_AT);
    }

    return SortField.fromRequestValue(sortBy.strip());
  }

  public Optional<SortDirection> resolvedSortDirection() {
    if (sortDirection == null || sortDirection.isBlank()) {
      return Optional.of(SortDirection.DESC);
    }

    return SortDirection.fromRequestValue(sortDirection.strip());
  }

  public int resolvedPage() {
    return page == null ? DEFAULT_PAGE : page;
  }

  public int resolvedPageSize() {
    return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
  }
}
