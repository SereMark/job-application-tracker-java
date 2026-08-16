package io.github.seremark.jobapplicationtracker.applications.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = JobPostingUrlValidator.class)
@Target({
  ElementType.FIELD,
  ElementType.METHOD,
  ElementType.PARAMETER,
  ElementType.ANNOTATION_TYPE,
  ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidJobPostingUrl {

  String message() default "The job posting URL must be an absolute HTTP or HTTPS URL.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
