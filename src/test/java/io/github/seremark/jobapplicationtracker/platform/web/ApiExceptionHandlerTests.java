package io.github.seremark.jobapplicationtracker.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

class ApiExceptionHandlerTests {

  @Test
  void validationErrorsAreReturnedAsProblemDetail() throws NoSuchMethodException {
    var request = new TestRequest("");
    var bindingResult = new BeanPropertyBindingResult(request, "request");
    bindingResult.addError(new FieldError("request", "companyName", "must not be blank"));
    bindingResult.addError(
        new ObjectError("request", "Next action fields must be provided together."));

    var methodParameter =
        new MethodParameter(TestRequest.class.getDeclaredConstructor(String.class), 0);
    var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);
    var webRequest = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/applications"));
    var exceptionHandler = new ApiExceptionHandler();
    exceptionHandler.setMessageSource(new StaticMessageSource());

    ResponseEntity<Object> response =
        exceptionHandler.handleMethodArgumentNotValid(
            exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);

    ProblemDetail problemDetail = (ProblemDetail) response.getBody();
    assertThat(problemDetail.getTitle()).isEqualTo("Request validation failed");
    assertThat(problemDetail.getDetail()).isEqualTo("One or more request fields are invalid.");
    assertThat(problemDetail.getProperties())
        .containsEntry(
            "errors",
            Map.of(
                "companyName",
                List.of("must not be blank"),
                "request",
                List.of("Next action fields must be provided together.")));
  }

  @ParameterizedTest
  @ValueSource(ints = 0)
  void methodValidationErrorsAreReturnedAsProblemDetail(int page) throws NoSuchMethodException {
    var method =
        ApiExceptionHandlerTests.class.getDeclaredMethod(
            "methodValidationErrorsAreReturnedAsProblemDetail", int.class);
    var methodParameter = new MethodParameter(method, 0);
    var validationError =
        new DefaultMessageSourceResolvable(
            new String[] {"Min"}, null, "must be greater than or equal to 1");
    var parameterResult =
        new ParameterValidationResult(
            methodParameter,
            page,
            List.of(validationError),
            null,
            null,
            null,
            (error, sourceType) -> null);
    var validationResult = MethodValidationResult.create(this, method, List.of(parameterResult));
    var exception = new HandlerMethodValidationException(validationResult);
    var webRequest =
        new ServletWebRequest(new MockHttpServletRequest("GET", "/api/applications?page=0"));
    var exceptionHandler = new ApiExceptionHandler();
    exceptionHandler.setMessageSource(new StaticMessageSource());

    ResponseEntity<Object> response =
        exceptionHandler.handleHandlerMethodValidationException(
            exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);

    ProblemDetail problemDetail = (ProblemDetail) response.getBody();
    assertThat(problemDetail.getProperties())
        .containsEntry("errors", Map.of("page", List.of("must be greater than or equal to 1")));
  }

  private record TestRequest(String companyName) {}
}
