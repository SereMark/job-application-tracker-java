# Job Application Tracker

A Spring Boot REST API for managing job applications, follow-up actions, and status history.

## Requirements

- Java 21
- Docker Desktop or Docker Engine with Docker Compose

A global Maven installation is not required because the repository includes the Maven Wrapper.

## Local database

Copy `.env.example` to `.env`, set a strong local-only password, then start PostgreSQL:

```powershell
Copy-Item .env.example .env
# Edit .env and set POSTGRES_PASSWORD before continuing.
docker compose up -d --wait postgres
```

The application reads the same local `.env` file. Starting it applies pending Flyway migrations,
then validates the JPA mappings against the database schema:

```powershell
.\mvnw.cmd spring-boot:run
```

PostgreSQL is exposed only on `127.0.0.1`. Stop the container without deleting its data:

```powershell
docker compose down
```

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
