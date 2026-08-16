package io.github.seremark.jobapplicationtracker.platform.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public final class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String GENERAL_ERROR_KEY = "request";

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return handleValidationException(
        exception, headers, status, request, collectValidationErrors(exception));
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return handleValidationException(
        exception, headers, status, request, collectValidationErrors(exception));
  }

  private ResponseEntity<Object> handleValidationException(
      Exception exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request,
      Map<String, List<String>> errors) {
    ProblemDetail problemDetail =
        createProblemDetail(
            exception, status, "One or more request fields are invalid.", null, null, request);
    problemDetail.setTitle("Request validation failed");
    problemDetail.setProperty("errors", errors);

    return handleExceptionInternal(exception, problemDetail, headers, status, request);
  }

  private static Map<String, List<String>> collectValidationErrors(
      MethodArgumentNotValidException exception) {
    Map<String, List<String>> errors = new LinkedHashMap<>();

    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> addError(errors, error.getField(), error));
    exception
        .getBindingResult()
        .getGlobalErrors()
        .forEach(error -> addError(errors, GENERAL_ERROR_KEY, error));

    return errors;
  }

  private static Map<String, List<String>> collectValidationErrors(
      HandlerMethodValidationException exception) {
    Map<String, List<String>> errors = new LinkedHashMap<>();

    for (ParameterValidationResult result : exception.getParameterValidationResults()) {
      if (result instanceof ParameterErrors parameterErrors) {
        parameterErrors
            .getFieldErrors()
            .forEach(error -> addError(errors, error.getField(), error));
        parameterErrors
            .getGlobalErrors()
            .forEach(error -> addError(errors, GENERAL_ERROR_KEY, error));
      } else {
        String parameterName =
            Objects.requireNonNullElse(
                result.getMethodParameter().getParameterName(), GENERAL_ERROR_KEY);
        result.getResolvableErrors().forEach(error -> addError(errors, parameterName, error));
      }
    }

    exception
        .getCrossParameterValidationResults()
        .forEach(error -> addError(errors, GENERAL_ERROR_KEY, error));

    return errors;
  }

  private static void addError(
      Map<String, List<String>> errors, String key, MessageSourceResolvable error) {
    String message = Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value.");
    errors.computeIfAbsent(key, ignored -> new ArrayList<>()).add(message);
  }
}
