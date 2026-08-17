package io.github.seremark.jobapplicationtracker.applications.web;

import java.time.Instant;

public record ApplicationResumeResponse(
    String fileName, String contentType, int size, Instant uploadedAt) {}
