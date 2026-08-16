package io.github.seremark.jobapplicationtracker.support;

import io.github.seremark.jobapplicationtracker.applications.persistence.JobApplicationRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.password=must-be-overridden-by-service-connection")
@AutoConfigureMockMvc
@Import(IntegrationTestConfiguration.class)
@ActiveProfiles("postgresql-integration-test")
public abstract class PostgreSqlIntegrationTest {

  public static final Instant DEFAULT_TEST_INSTANT = Instant.parse("2026-08-16T08:30:00.123456Z");

  @Autowired protected MockMvc mockMvc;

  @Autowired protected JdbcTemplate jdbcTemplate;

  @Autowired protected JobApplicationRepository jobApplicationRepository;

  @Autowired protected AdjustableClock testClock;

  @BeforeEach
  protected final void resetIntegrationTestState() {
    jdbcTemplate.execute("TRUNCATE TABLE status_changes, job_applications");
    testClock.setInstant(DEFAULT_TEST_INSTANT);
  }
}
