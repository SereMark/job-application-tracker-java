# Job Application Tracker

A Spring Boot REST API for managing job applications, follow-up actions, and status history.

## Requirements

- Java 21
- Docker Desktop or Docker Engine with Docker Compose

A global Maven installation is not required because the repository includes the Maven Wrapper.

## Run the complete stack

Copy `.env.example` to `.env`, set a strong local-only password, then build and start the API and PostgreSQL:

```powershell
Copy-Item .env.example .env
# Edit .env and set POSTGRES_PASSWORD before continuing.
docker compose up --build --wait
```

The API is available at `http://localhost:8081`, Swagger UI at
`http://localhost:8081/swagger-ui.html`, and the readiness check at
`http://localhost:8081/actuator/health/readiness`. Example requests are in
`requests/JobApplicationTracker.http`.

Stop the containers without deleting the PostgreSQL data volume:

```powershell
docker compose down
```

Running `docker compose down --volumes` also permanently deletes the stored database data.

## Run the API directly

Start only PostgreSQL:

```powershell
docker compose up -d --wait postgres
```

The application reads the same local `.env` file. Starting it applies pending Flyway migrations,
then validates the JPA mappings against the database schema:

```powershell
.\mvnw.cmd spring-boot:run
```

PostgreSQL and the containerized API are exposed only on `127.0.0.1`.

## Build and verify

On Windows:

```powershell
.\mvnw.cmd clean verify
```

On Linux or macOS:

```bash
./mvnw clean verify
```

The `verify` phase runs unit tests and `*IT` integration tests. The integration suite starts an
isolated PostgreSQL container automatically, so Docker must be running.

## Format Java sources

```powershell
.\mvnw.cmd spotless:apply
```
