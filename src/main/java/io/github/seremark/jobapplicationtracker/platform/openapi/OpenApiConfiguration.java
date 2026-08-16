package io.github.seremark.jobapplicationtracker.platform.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

  @Bean
  OpenAPI jobApplicationTrackerOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Job Application Tracker API")
                .description("REST API for managing job applications")
                .version("v1"));
  }
}
