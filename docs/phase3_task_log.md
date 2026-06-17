# Phase 3 — Task Log

## Step 3.1 — Create job applications table
**Date:** 2026-06-17
**Status:** Done

- Created Flyway migration `db/migration/V2__create_job_applications.sql`:
  - `id` UUID primary key with `gen_random_uuid()` default
  - `user_id` UUID foreign key referencing `users(id)` — establishes ownership
  - `company_name`, `job_title` VARCHAR not null
  - `job_link`, `location`, `employment_type`, `source`, `notes` nullable
  - `status` VARCHAR not null, defaults to `'SAVED'`
  - `applied_at`, `deleted_at` nullable timestamps (deleted_at used for soft delete)
  - `created_at`, `updated_at` TIMESTAMP WITH TIME ZONE with `now()` defaults
  - Composite index on `(user_id, status)` — fast filter by status per user
  - Composite index on `(user_id, created_at)` — fast sort by date per user
- Created `ApplicationStatus` enum: SAVED, APPLIED, INTERVIEWING, OFFER, REJECTED, WITHDRAWN
- Created `application/JobApplication.java` JPA entity:
  - `@ManyToOne(fetch = FetchType.LAZY)` on `user` field — avoids N+1 queries on list endpoints
  - `@Enumerated(EnumType.STRING)` on `status` — stored as readable string in DB, not numeric index
  - Constructor `(user, companyName, jobTitle)` — minimal required fields; status defaults to SAVED
- Created `application/JobApplicationRepository.java` extending `JpaRepository<JobApplication, UUID>`:
  - `findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId)` — the tenant-safe lookup used in all user-facing service calls; returns empty if wrong owner or soft-deleted
- Created `JobApplicationRepositoryTest` using `@DataJpaTest` + real PostgreSQL:
  - Save and find by id + user — verifies default status is SAVED
  - Tenant isolation — userB cannot fetch userA's application (returns empty)
  - Soft delete — application with deletedAt set is not returned
- All tests pass
- Verified `job_applications` table in local Docker DB with `\d job_applications`

### Architecture note — indexes and admin systems

The composite indexes `(user_id, status)` and `(user_id, created_at)` are optimized for client-facing queries where every query is scoped by `user_id`. A future admin/management system that queries across all users (e.g. "show all INTERVIEWING applications globally") would need separate single-column indexes on `status` and `created_at`, since those queries have no `user_id` filter.

Admin query patterns are fundamentally different from user-facing query patterns. At scale, this is one reason companies run separate **read replicas** for admin dashboards — admin queries (full-table scans, aggregations, cross-tenant views) are slow and resource-intensive and should not compete with user traffic on the primary database. The admin replica can have its own index strategy tuned for admin query shapes.

---

## Step 3.2 — Tenant-safe service pattern
**Date:** 2026-06-17
**Status:** Done

- Created `application/JobApplicationService` with ownership helper:
  - `getOwnedApplicationOrThrow(UUID id, UUID userId)` — wraps the tenant-safe repository lookup; throws `ResourceNotFoundException` if the application doesn't exist, doesn't belong to the user, or is soft-deleted
  - Made package-private (not private) so it can be tested directly and called within the same package
  - All future CRUD service methods call this helper instead of plain `findById`
- Created `JobApplicationServiceTest` as a pure unit test using Mockito:
  - `@Mock JobApplicationRepository` — fake repository, no DB needed
  - `@InjectMocks JobApplicationService` — real service with mock injected via constructor
  - Test: wrong user ID → `findByIdAndUserIdAndDeletedAtIsNull` returns empty → service throws `ResourceNotFoundException`
- All tests pass

---

## Step 3.3 — Create application endpoint
**Date:** 2026-06-17
**Status:** Done

- Added `EXPIRED` to `ApplicationStatus` enum — covers job postings that were saved but later canceled by the company; no migration needed since status is stored as VARCHAR
- Created `application/dto/CreateApplicationRequest`:
  - `companyName` and `jobTitle` — `@NotBlank` required
  - `jobLink` — `@URL` optional, validated if present
  - `location`, `employmentType`, `source`, `notes`, `status` — all optional
  - If `status` not provided, entity defaults to `SAVED`
- Created `application/dto/ApplicationResponse` — id, all fields, timestamps; does not expose `userId`
- Updated `JobApplicationService`:
  - `create(request, currentUser)` — builds entity, saves, returns response via `toResponse()`
  - `toResponse()` private helper maps entity → DTO; centralizes mapping used by all future service methods
- Created `ApplicationController` at `/api/applications`:
  - `POST /api/applications` → 201; uses `@AuthenticationPrincipal User` to get current user from SecurityContext
- Created `ApplicationControllerTest` using `@WebMvcTest` + `@Import(SecurityConfig.class)`:
  - Valid create with auth → 201 with correct data
  - Missing company name → 422 `VALIDATION_ERROR`
  - Missing job title → 422 `VALIDATION_ERROR`
  - Invalid URL → 422 `VALIDATION_ERROR`
  - No token → 401 `UNAUTHORIZED`
  - Uses `SecurityMockMvcRequestPostProcessors.authentication()` to inject a `User` entity as principal
- All tests pass
- Manually verified: valid request returns 201 with UUID and timestamps, no token returns 401, missing field returns 422

---

## Step 3.4 — List and detail endpoints
**Date:** 2026-06-17
**Status:** Done

- Added `findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable)` to `JobApplicationRepository` — Spring Data generates paginated SQL scoped to current user, excluding soft-deleted rows
- Updated `JobApplicationService`:
  - `list(UUID userId, int page, int size)` — builds `PageRequest` sorted by `createdAt DESC`, calls repository, maps result to `PageResponse<ApplicationResponse>` via `PageResponse.from()`
  - `getById(UUID id, UUID userId)` — delegates to `getOwnedApplicationOrThrow`, maps to response
- Added two endpoints to `ApplicationController`:
  - `GET /api/applications` — accepts `page` (default 0) and `size` (default 20) query params
  - `GET /api/applications/{id}` — reads UUID from path variable
- Added 3 tests to `ApplicationControllerTest`:
  - List returns 200 with content array and pagination metadata
  - Detail returns 200 for own application
  - Non-existent or foreign application returns 404 `NOT_FOUND`
- Fixed `AuthIntegrationTest.cleanUp` — now deletes `job_applications` before `users` to respect the FK constraint
- All tests pass
- Manually verified: list returns paginated response, detail returns application by ID, non-existent ID returns 404

---

## Step 3.5 — Update and soft delete
**Date:** 2026-06-17
**Status:** Done

- Created `application/dto/UpdateApplicationRequest` — all fields optional (no `@NotBlank`); only non-null fields are applied by the service; `@URL` still validates `jobLink` when provided
- Updated `JobApplicationService`:
  - `update(UUID id, UpdateApplicationRequest request, UUID userId)` — calls `getOwnedApplicationOrThrow`, applies only non-null fields, saves and returns updated response
  - `delete(UUID id, UUID userId)` — calls `getOwnedApplicationOrThrow`, sets `deletedAt = OffsetDateTime.now()`, saves; returns void (204)
- Added two endpoints to `ApplicationController`:
  - `PATCH /api/applications/{id}` → 200 with updated application
  - `DELETE /api/applications/{id}` → 204 No Content
- Added 4 tests to `ApplicationControllerTest`:
  - Update returns 200 with updated fields
  - Update non-existent application returns 404
  - Delete returns 204
  - Delete non-existent application returns 404
  - Used `doThrow(...).when(service).delete(...)` for void method mocking
- Added `OpenApiConfig` bean to register JWT Bearer security scheme — enables Authorize button in Swagger UI
- All tests pass
- Manually verified via curl: update changes only sent fields, delete sets `deleted_at` in DB (confirmed in DBeaver), deleted application returns 404 on detail and disappears from list

---
