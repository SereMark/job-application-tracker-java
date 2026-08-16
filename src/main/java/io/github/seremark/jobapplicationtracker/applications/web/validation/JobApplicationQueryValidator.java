package io.github.seremark.jobapplicationtracker.applications.web.validation;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import io.github.seremark.jobapplicationtracker.applications.service.JobApplicationQuery.SortDirection;
import io.github.seremark.jobapplicationtracker.applications.service.JobApplicationQuery.SortField;
import io.github.seremark.jobapplicationtracker.applications.web.GetJobApplicationsQuery;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class JobApplicationQueryValidator
    implements ConstraintValidator<ValidJobApplicationQuery, GetJobApplicationsQuery> {

  @Override
  public boolean isValid(GetJobApplicationsQuery value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    boolean valid = true;
    context.disableDefaultConstraintViolation();

    if (hasText(value.status()) && value.resolvedStatus().isEmpty()) {
      addViolation(
          context,
          "status",
          "Status must be one of: "
              + Arrays.stream(JobApplicationStatus.values())
                  .map(status -> status.toJsonValue())
                  .collect(Collectors.joining(", "))
              + ".");
      valid = false;
    }

    if (hasText(value.sortBy()) && value.resolvedSortBy().isEmpty()) {
      addViolation(
          context,
          "sortBy",
          "Sort by must be one of: "
              + Arrays.stream(SortField.values())
                  .map(field -> field.requestValue())
                  .collect(Collectors.joining(", "))
              + ".");
      valid = false;
    }

    if (hasText(value.sortDirection()) && value.resolvedSortDirection().isEmpty()) {
      addViolation(
          context,
          "sortDirection",
          "Sort direction must be one of: "
              + Arrays.stream(SortDirection.values())
                  .map(direction -> direction.requestValue())
                  .collect(Collectors.joining(", "))
              + ".");
      valid = false;
    }

    if (value.appliedFrom() != null
        && value.appliedTo() != null
        && value.appliedFrom().isAfter(value.appliedTo())) {
      String message = "Applied from cannot be later than applied to.";
      addViolation(context, "appliedFrom", message);
      addViolation(context, "appliedTo", message);
      valid = false;
    }

    int page = value.resolvedPage();
    int pageSize = value.resolvedPageSize();
    if (page >= 1
        && pageSize >= 1
        && pageSize <= GetJobApplicationsQuery.MAX_PAGE_SIZE
        && ((long) page - 1) * pageSize > Integer.MAX_VALUE) {
      String message = "The requested page is too far beyond the available range.";
      addViolation(context, "page", message);
      addViolation(context, "pageSize", message);
      valid = false;
    }

    return valid;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static void addViolation(
      ConstraintValidatorContext context, String property, String message) {
    context
        .buildConstraintViolationWithTemplate(message)
        .addPropertyNode(property)
        .addConstraintViolation();
  }
}
