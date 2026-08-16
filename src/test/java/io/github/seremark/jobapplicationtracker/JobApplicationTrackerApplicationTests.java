package io.github.seremark.jobapplicationtracker;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.seremark.jobapplicationtracker.platform.web.ApiExceptionHandler;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.DispatcherServlet;

class JobApplicationTrackerApplicationTests {

  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner()
          .withUserConfiguration(JobApplicationTrackerApplication.class);

  @Test
  void contextLoads() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(DispatcherServlet.class);
          assertThat(context).hasSingleBean(Validator.class);
          assertThat(context).hasSingleBean(ApiExceptionHandler.class);
          assertThat(context).hasSingleBean(OpenAPI.class);
          assertThat(context).hasSingleBean(HealthEndpoint.class);
        });
  }
}
