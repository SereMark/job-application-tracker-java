package io.github.seremark.jobapplicationtracker.applications.web;

import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.service.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications")
public class JobApplicationController {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobApplicationController.class);

  private final JobApplicationService jobApplicationService;

  public JobApplicationController(JobApplicationService jobApplicationService) {
    this.jobApplicationService = jobApplicationService;
  }

  @PostMapping
  @Operation(
      summary = "Create a job application",
      description =
          "Creates a job application. Status defaults to Saved when omitted. Next action "
              + "description and due date must be provided together.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Job application created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JobApplicationResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Request validation failed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<JobApplicationResponse> create(
      @Valid @RequestBody CreateJobApplicationRequest request) {
    JobApplication application =
        jobApplicationService.create(
            JobApplicationMapper.toDetails(request), JobApplicationMapper.toInitialStatus(request));
    JobApplicationResponse response = JobApplicationMapper.toResponse(application);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

    LOGGER.info(
        "Created job application {} for {} as {}.",
        response.id(),
        response.companyName(),
        response.positionTitle());

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  @Operation(
      summary = "Query job applications",
      description =
          "Searches company names and position titles and supports status, source, application "
              + "date, and next-action filters. Results are paged and can only be sorted by "
              + "updatedAt, createdAt, companyName, positionTitle, appliedOn, or nextActionDueAt "
              + "in asc or desc direction.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Job applications returned",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PagedJobApplicationsResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Query validation failed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<PagedJobApplicationsResponse> query(
      @Valid @ParameterObject @ModelAttribute GetJobApplicationsQuery query) {
    Page<JobApplication> applications = jobApplicationService.query(query.toServiceQuery());
    return ResponseEntity.ok(JobApplicationMapper.toPagedResponse(applications));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a job application")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Job application found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JobApplicationResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Job application not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<JobApplicationResponse> getById(@PathVariable UUID id) {
    JobApplication application = jobApplicationService.getById(id);
    return ResponseEntity.ok(JobApplicationMapper.toResponse(application));
  }
}
