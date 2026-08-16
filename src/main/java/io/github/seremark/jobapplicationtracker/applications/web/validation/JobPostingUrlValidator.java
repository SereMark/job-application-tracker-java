package io.github.seremark.jobapplicationtracker.applications.web.validation;

import static io.github.seremark.jobapplicationtracker.applications.domain.JobApplication.JOB_POSTING_URL_MAX_LENGTH;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;

public final class JobPostingUrlValidator implements ConstraintValidator<ValidJobPostingUrl, URI> {

  @Override
  public boolean isValid(URI value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    String scheme = value.getScheme();
    boolean hasHttpScheme =
        scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
    boolean hasHost = value.getHost() != null && !value.getHost().isBlank();

    if (!value.isAbsolute() || !hasHttpScheme || !hasHost) {
      return false;
    }

    if (value.toASCIIString().length() <= JOB_POSTING_URL_MAX_LENGTH) {
      return true;
    }

    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(
            "The job posting URL cannot exceed " + JOB_POSTING_URL_MAX_LENGTH + " characters.")
        .addConstraintViolation();
    return false;
  }
}
