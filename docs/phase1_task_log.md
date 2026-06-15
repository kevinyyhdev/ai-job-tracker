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
**Date:** 2026-06-15
**Status:** Done

- Created `common/api/ApiResponse<T>` — wraps all success responses: `{"success": true, "data": {...}}`
- Created `common/api/PageResponse<T>` — wraps paginated list responses with page/size/totalElements metadata
- Created `common/api/ErrorResponse` — wraps all error responses: `{"success": false, "error": {"code": "...", "message": "...", "fields": {...}}}`
- Created three custom exceptions:
  - `ResourceNotFoundException` → 404
  - `DuplicateResourceException` → 409
  - `BusinessRuleException` → 422
- Created `GlobalExceptionHandler` with `@RestControllerAdvice` mapping each exception to the correct HTTP status and `ErrorResponse`
- Also handles `MethodArgumentNotValidException` (Bean Validation failures) → 422 with per-field error details
- Unknown exceptions → 500 with safe message (no stack trace leaked)
- Created `GlobalExceptionHandlerTest` using standalone MockMvc (no Spring context, no database needed)
- All 7 tests pass: `Tests run: 7, Failures: 0, Errors: 0`

---

## Step 1.3 — Swagger/OpenAPI
**Date:** —
**Status:** Not started

- Need to add `springdoc-openapi` dependency to `pom.xml`
- Need to configure title and version
- Need to verify Swagger UI loads at `/swagger-ui.html`
