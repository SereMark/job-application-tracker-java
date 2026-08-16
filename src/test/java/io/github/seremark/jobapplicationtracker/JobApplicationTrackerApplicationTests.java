package io.github.seremark.jobapplicationtracker;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.seremark.jobapplicationtracker.platform.web.ApiExceptionHandler;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.DispatcherServlet;

class JobApplicationTrackerApplicationTests {

  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner()
          .withPropertyValues(
              "spring.autoconfigure.exclude="
                  + DataSourceAutoConfiguration.class.getName()
                  + ","
                  + HibernateJpaAutoConfiguration.class.getName()
                  + ","
                  + FlywayAutoConfiguration.class.getName(),
              "management.endpoint.health.validate-group-membership=false")
          .withUserConfiguration(JobApplicationTrackerApplication.class);

  @Test
  void webPlatformContextLoadsWithoutDatabaseInfrastructure() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(DispatcherServlet.class);
          assertThat(context).hasSingleBean(Validator.class);
          assertThat(context).hasSingleBean(ApiExceptionHandler.class);
          assertThat(context).hasSingleBean(OpenAPI.class);
          assertThat(context).hasSingleBean(HealthEndpoint.class);
          assertThat(context).hasSingleBean(Clock.class);
          assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
        });
  }
}
