package io.github.seremark.jobapplicationtracker.support;

import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
@Profile("postgresql-integration-test")
class IntegrationTestConfiguration {

  private static final DockerImageName POSTGRES_IMAGE =
      DockerImageName.parse("postgres:18.4-alpine");

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    PostgreSQLContainer postgresContainer = new PostgreSQLContainer(POSTGRES_IMAGE);
    postgresContainer
        .withDatabaseName("job_application_tracker_test")
        .withUsername("integration_test")
        .withPassword("integration-tests-only");
    return postgresContainer;
  }

  @Bean
  @Primary
  AdjustableClock adjustableClock() {
    return new AdjustableClock(PostgreSqlIntegrationTest.DEFAULT_TEST_INSTANT, ZoneOffset.UTC);
  }
}
