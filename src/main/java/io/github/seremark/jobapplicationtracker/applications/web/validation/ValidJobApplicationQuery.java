package io.github.seremark.jobapplicationtracker.applications.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = JobApplicationQueryValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidJobApplicationQuery {

  String message() default "Job application query parameters are invalid.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
