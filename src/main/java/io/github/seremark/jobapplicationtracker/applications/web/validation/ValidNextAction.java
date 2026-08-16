package io.github.seremark.jobapplicationtracker.applications.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = NextActionValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNextAction {

  String message() default
      "Next action description and due date must either both be provided or both be omitted.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
