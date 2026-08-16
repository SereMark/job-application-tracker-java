package io.github.seremark.jobapplicationtracker.applications.persistence;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplicationStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class JobApplicationSpecifications {

  private static final char LIKE_ESCAPE_CHARACTER = '\\';

  private JobApplicationSpecifications() {}

  public static Specification<JobApplication> matching(
      String search,
      JobApplicationStatus status,
      String source,
      LocalDate appliedFrom,
      LocalDate appliedTo,
      Instant nextActionBefore) {
    return (root, criteriaQuery, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (search != null) {
        String pattern = "%" + escapeLikePattern(search.toLowerCase(Locale.ROOT)) + "%";
        Predicate companyNameMatches =
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("companyName")), pattern, LIKE_ESCAPE_CHARACTER);
        Predicate positionTitleMatches =
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("positionTitle")), pattern, LIKE_ESCAPE_CHARACTER);
        predicates.add(criteriaBuilder.or(companyNameMatches, positionTitleMatches));
      }

      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }

      if (source != null) {
        predicates.add(criteriaBuilder.equal(root.get("source"), source));
      }

      if (appliedFrom != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("appliedOn"), appliedFrom));
      }

      if (appliedTo != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("appliedOn"), appliedTo));
      }

      if (nextActionBefore != null) {
        predicates.add(
            criteriaBuilder.lessThanOrEqualTo(root.get("nextActionDueAt"), nextActionBefore));
      }

      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  static String escapeLikePattern(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
