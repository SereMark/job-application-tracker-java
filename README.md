# Job Application Tracker

A Spring Boot REST API for managing job applications, follow-up actions, and status history.

## Requirements

- Java 21

A global Maven installation is not required because the repository includes the Maven Wrapper.

## Build and verify

On Windows:

```powershell
.\mvnw.cmd clean verify
```

On Linux or macOS:

```bash
./mvnw clean verify
```

## Format Java sources

```powershell
.\mvnw.cmd spotless:apply
```
