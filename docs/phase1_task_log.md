# Phase 1 — Task Log

## Step 1.1 — Health endpoint
**Date:** 2026-06-15
**Status:** Done

- Configured Actuator in `application.yaml`:
  - Only exposes `/actuator/health` (not all endpoints)
  - `show-details: always` so DB health is visible in the response
- Created `HealthEndpointTest` using MockMvc to assert `/actuator/health` returns 200 and `{"status":"UP"}`
- Added `@ActiveProfiles("test")` to both `HealthEndpointTest` and `JobtrackerApplicationTests` so tests always load `application-test.yml`
- Fixed `application-test.yml` fallback URL from `job_tracker_test` to `job_tracker` to match local Docker database
- `./mvnw test` passes locally: `Tests run: 2, Failures: 0, Errors: 0`

---

## Step 1.2 — Standard API response and global error handling
**Date:** —
**Status:** Not started

- Need to create `ApiResponse<T>`, `PageResponse<T>`, `ErrorResponse`
- Need to create `ResourceNotFoundException`, `DuplicateResourceException`, `BusinessRuleException`
- Need to create `GlobalExceptionHandler` mapping exceptions to correct HTTP status codes
- Need tests for each error mapping

---

## Step 1.3 — Swagger/OpenAPI
**Date:** —
**Status:** Not started

- Need to add `springdoc-openapi` dependency to `pom.xml`
- Need to configure title and version
- Need to verify Swagger UI loads at `/swagger-ui.html`
