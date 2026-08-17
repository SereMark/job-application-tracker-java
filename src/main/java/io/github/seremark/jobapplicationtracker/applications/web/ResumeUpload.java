package io.github.seremark.jobapplicationtracker.applications.web;

import static io.github.seremark.jobapplicationtracker.applications.domain.ApplicationResume.FILE_NAME_MAX_LENGTH;
import static io.github.seremark.jobapplicationtracker.applications.domain.ApplicationResume.MAX_FILE_SIZE;

import io.github.seremark.jobapplicationtracker.applications.service.InvalidResumeFileException;
import java.io.IOException;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;

record ResumeUpload(String fileName, String contentType, byte[] content) {

  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String PDF_CONTENT_TYPE = "application/pdf";

  static ResumeUpload from(MultipartFile file) throws IOException {
    if (file.isEmpty()) {
      throw new InvalidResumeFileException("The resume file cannot be empty.");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new InvalidResumeFileException("The resume file cannot exceed 5 MB.");
    }

    String fileName = cleanFileName(file.getOriginalFilename());
    String lowerCaseFileName = fileName.toLowerCase(Locale.ROOT);
    String contentType;

    if (lowerCaseFileName.endsWith(".pdf")) {
      contentType = PDF_CONTENT_TYPE;
    } else if (lowerCaseFileName.endsWith(".docx")) {
      contentType = DOCX_CONTENT_TYPE;
    } else {
      throw new InvalidResumeFileException("Only PDF and DOCX resume files are supported.");
    }

    byte[] content = file.getBytes();

    if (content.length == 0 || content.length > MAX_FILE_SIZE) {
      throw new InvalidResumeFileException(
          content.length == 0
              ? "The resume file cannot be empty."
              : "The resume file cannot exceed 5 MB.");
    }

    return new ResumeUpload(fileName, contentType, content);
  }

  private static String cleanFileName(String originalFileName) {
    String normalizedPath = originalFileName == null ? "" : originalFileName.replace('\\', '/');
    int lastSeparator = normalizedPath.lastIndexOf('/');
    String fileName = normalizedPath.substring(lastSeparator + 1).strip();

    if (fileName.isBlank()) {
      throw new InvalidResumeFileException("The resume file name is required.");
    }

    if (fileName.length() > FILE_NAME_MAX_LENGTH) {
      throw new InvalidResumeFileException(
          "The resume file name cannot exceed " + FILE_NAME_MAX_LENGTH + " characters.");
    }

    return fileName;
  }
}
