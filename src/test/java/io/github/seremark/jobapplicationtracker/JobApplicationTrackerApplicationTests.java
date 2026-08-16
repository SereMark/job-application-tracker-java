package io.github.seremark.jobapplicationtracker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class JobApplicationTrackerApplicationTests {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(JobApplicationTrackerApplication.class);

  @Test
  void contextLoads() {
    contextRunner.run(context -> assertThat(context).hasNotFailed());
  }
}
