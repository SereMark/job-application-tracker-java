package io.github.seremark.jobapplicationtracker.applications.web;

import io.github.seremark.jobapplicationtracker.applications.domain.ApplicationResume;
import io.github.seremark.jobapplicationtracker.applications.domain.JobApplication;
import io.github.seremark.jobapplicationtracker.applications.service.JobApplicationService;
import io.github.seremark.jobapplicationtracker.applications.service.JobApplicationSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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

  @GetMapping("/summary")
  @Operation(
      summary = "Summarize the application pipeline",
      description =
          "Returns total and per-status application counts, plus overdue next actions and next "
              + "actions due within the next seven days.")
  @ApiResponse(
      responseCode = "200",
      description = "Application pipeline summary returned",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApplicationSummaryResponse.class)))
  public ResponseEntity<ApplicationSummaryResponse> getSummary() {
    JobApplicationSummary summary = jobApplicationService.getSummary();
    return ResponseEntity.ok(JobApplicationMapper.toResponse(summary));
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

  @PutMapping(value = "/{id}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Upload or replace an application resume",
      description =
          "Stores one PDF or DOCX resume of at most 5 MB for the job application. Uploading "
              + "again replaces the previous file.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Resume stored",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ApplicationResumeResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Resume validation failed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Job application not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<ApplicationResumeResponse> uploadResume(
      @PathVariable UUID id, @RequestPart("file") MultipartFile file) throws IOException {
    ResumeUpload upload = ResumeUpload.from(file);
    ApplicationResume resume =
        jobApplicationService.putResume(
            id, upload.fileName(), upload.contentType(), upload.content());
    return ResponseEntity.ok(JobApplicationMapper.toResponse(resume));
  }

  @GetMapping("/{id}/resume")
  @Operation(
      summary = "Download an application resume",
      description =
          "Downloads the resume stored for the job application using its original file name.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Resume downloaded",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                schema = @Schema(type = "string", format = "binary"))),
    @ApiResponse(
        responseCode = "404",
        description = "Application or resume not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<byte[]> downloadResume(@PathVariable UUID id) {
    ApplicationResume resume = jobApplicationService.getResume(id);
    ContentDisposition contentDisposition =
        ContentDisposition.attachment()
            .filename(resume.getFileName(), StandardCharsets.UTF_8)
            .build();

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(resume.getContentType()))
        .contentLength(resume.getSize())
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        .lastModified(resume.getUploadedAt())
        .body(resume.getContent());
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Replace job application details",
      description =
          "Replaces all editable details and leaves the current status unchanged. Omitted "
              + "optional fields are cleared.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Job application updated",
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
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Job application not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<JobApplicationResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateJobApplicationRequest request) {
    JobApplication application =
        jobApplicationService.update(id, JobApplicationMapper.toDetails(request));
    return ResponseEntity.ok(JobApplicationMapper.toResponse(application));
  }

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Change a job application status",
      description =
          "Changes the current status and records the transition in status history. Changing "
              + "to the current status returns a conflict response.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Job application status changed",
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
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Job application not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Job application already has the requested status",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<JobApplicationResponse> changeStatus(
      @PathVariable UUID id, @Valid @RequestBody ChangeJobApplicationStatusRequest request) {
    JobApplication application =
        jobApplicationService.changeStatus(id, request.status(), request.note());
    return ResponseEntity.ok(JobApplicationMapper.toResponse(application));
  }

  @GetMapping("/{id}/status-history")
  @Operation(
      summary = "Get job application status history",
      description = "Returns the initial status and every later transition in chronological order.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Job application status history returned",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array =
                    @ArraySchema(schema = @Schema(implementation = StatusChangeResponse.class)))),
    @ApiResponse(
        responseCode = "404",
        description = "Job application not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<List<StatusChangeResponse>> getStatusHistory(@PathVariable UUID id) {
    List<StatusChangeResponse> history =
        jobApplicationService.getStatusHistory(id).stream()
            .map(change -> JobApplicationMapper.toResponse(change))
            .toList();
    return ResponseEntity.ok(history);
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete a job application",
      description = "Permanently deletes a job application, its status history, and its resume.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Job application deleted"),
    @ApiResponse(
        responseCode = "404",
        description = "Job application not found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    jobApplicationService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
