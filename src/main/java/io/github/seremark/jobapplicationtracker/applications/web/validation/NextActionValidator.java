package io.github.seremark.jobapplicationtracker.applications.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class NextActionValidator
    implements ConstraintValidator<ValidNextAction, NextActionFields> {

  @Override
  public boolean isValid(NextActionFields value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    boolean hasDescription =
        value.nextActionDescription() != null && !value.nextActionDescription().isBlank();
    boolean hasDueAt = value.nextActionDueAt() != null;

    if (hasDescription == hasDueAt) {
      return true;
    }

    String message = context.getDefaultConstraintMessageTemplate();
    context.disableDefaultConstraintViolation();
    addViolation(context, "nextActionDescription", message);
    addViolation(context, "nextActionDueAt", message);
    return false;
  }

  private static void addViolation(
      ConstraintValidatorContext context, String property, String message) {
    context
        .buildConstraintViolationWithTemplate(message)
        .addPropertyNode(property)
        .addConstraintViolation();
  }
}
