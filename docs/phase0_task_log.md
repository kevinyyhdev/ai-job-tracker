# Phase 0 — Task Log

## Step 0.1 — Repo and Spring Boot skeleton
**Date:** 2026-06-15
**Status:** Done

- Spring Boot 3.5.x skeleton generated via Spring Initializr with Java 21
- Dependencies included: Web, Security, Data JPA, PostgreSQL Driver, Flyway, Validation, Actuator, Lombok
- Package name: `com.kevin.jobtracker`
- `.gitignore` configured (covers Maven, IntelliJ, macOS, secrets, uploads)
- GitHub remote: `git@github.com:kevinyyhdev/ai-job-tracker.git`
- Created `.github/workflows/ci.yml` — runs `mvn test` on push/PR to main using a PostgreSQL service container
- Created `src/test/resources/application-test.yml` — configures datasource for the test profile used in CI
- CI confirmed green on GitHub Actions

---

## Step 0.2 — PostgreSQL with Docker Compose
**Date:** 2026-06-15
**Status:** Done

- Created `docker-compose.yml` with PostgreSQL 16, named volume `postgres_data`, port 5432
- Database: `job_tracker`, user: `job_tracker_user`
- Docker Desktop installed and running
- Verified with:
  ```bash
  docker compose up -d
  docker compose ps
  docker exec -it jobtracker-postgres-1 pg_isready
  ```
- Output confirmed: `localhost:5432 - accepting connections`

---

## Step 0.3 — Spring profiles and database connection
**Date:** 2026-06-15
**Status:** Done

- Created `src/main/resources/application-local.yml` with local datasource config pointing to Docker PostgreSQL
- Created `.env.example` documenting all required environment variables (datasource, profile, JWT placeholders)
- App starts successfully with `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run`
- Verified `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`
- Spring Security warning about generated password is expected — will be replaced in Phase 2
