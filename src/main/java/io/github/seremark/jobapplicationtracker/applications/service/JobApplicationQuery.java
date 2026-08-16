package io.github.seremark.jobapplicationtracker.applications.service;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public record JobApplicationQuery(
    String search,
    JobApplicationStatus status,
    String source,
    LocalDate appliedFrom,
    LocalDate appliedTo,
    Instant nextActionBefore,
    int page,
    int pageSize,
    SortField sortBy,
    SortDirection sortDirection) {

  public static final int MAX_PAGE_SIZE = 100;

  public JobApplicationQuery {
    search = normalizeOptional(search);
    source = normalizeOptional(source);
    sortBy = Objects.requireNonNull(sortBy, "sortBy");
    sortDirection = Objects.requireNonNull(sortDirection, "sortDirection");

    if (page < 1) {
      throw new IllegalArgumentException("page must be at least 1.");
    }
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize must be at least 1.");
    }
    if (pageSize > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("pageSize cannot exceed " + MAX_PAGE_SIZE + ".");
    }
    if (((long) page - 1) * pageSize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "The requested page is too far beyond the available range.");
    }
    if (appliedFrom != null && appliedTo != null && appliedFrom.isAfter(appliedTo)) {
      throw new IllegalArgumentException("appliedFrom cannot be later than appliedTo.");
    }
  }

  public enum SortField {
    UPDATED_AT("updatedAt"),
    CREATED_AT("createdAt"),
    COMPANY_NAME("companyName"),
    POSITION_TITLE("positionTitle"),
    APPLIED_ON("appliedOn"),
    NEXT_ACTION_DUE_AT("nextActionDueAt");

    private final String requestValue;

    SortField(String requestValue) {
      this.requestValue = requestValue;
    }

    public String requestValue() {
      return requestValue;
    }

    public static Optional<SortField> fromRequestValue(String value) {
      return Arrays.stream(values())
          .filter(field -> field.requestValue.equalsIgnoreCase(value))
          .findFirst();
    }
  }

  public enum SortDirection {
    ASC("asc"),
    DESC("desc");

    private final String requestValue;

    SortDirection(String requestValue) {
      this.requestValue = requestValue;
    }

    public String requestValue() {
      return requestValue;
    }

    public static Optional<SortDirection> fromRequestValue(String value) {
      return Arrays.stream(values())
          .filter(direction -> direction.requestValue.equalsIgnoreCase(value))
          .findFirst();
    }
  }

  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
