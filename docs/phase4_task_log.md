# Phase 4 — Task Log

## Step 4.1 — Resume table and storage abstraction
**Date:** 2026-06-18
**Status:** Done

- Created Flyway migration `db/migration/V3__create_resumes.sql`:
  - `id` UUID primary key, `user_id` FK to users, `original_filename`, `content_type`, `size_bytes` — metadata about the file
  - `storage_key` VARCHAR UNIQUE — the identifier used to locate the file bytes on disk (UUID prefix + original filename); uniqueness enforced at DB level
  - `extracted_text` TEXT nullable — populated in Step 4.3 when Apache Tika parses the file; stored here so AI matching reads from DB instead of re-parsing on every request
  - `created_at`, `updated_at` timestamps
  - Single-column index on `user_id` — resume queries are always scoped by user but don't have a second filter column like status
- Created `resume/Resume.java` JPA entity — same patterns as `JobApplication`: `@ManyToOne(fetch = FetchType.LAZY)` on user, `@CreationTimestamp`/`@UpdateTimestamp`, minimal constructor `(user, originalFilename, contentType, sizeBytes, storageKey)`
- Created `resume/ResumeRepository`:
  - `findByIdAndUserId(UUID id, UUID userId)` — tenant-safe ownership check (same pattern as job applications; returns empty if wrong owner)
  - `findByUserIdOrderByCreatedAtDesc(UUID userId)` — list for current user, newest first; no pagination needed since users typically have few resumes
- Created `common/storage/StorageService` interface with three methods: `store`, `load`, `delete` — hiding the storage medium behind an interface so a future `S3StorageService` can be dropped in without changing any resume business logic
- Created `common/storage/LocalStorageService` implementing `StorageService`:
  - `@Value("${storage.local.root:uploads}")` — storage root is configurable; defaults to `uploads/` relative to working directory
  - Constructor calls `Files.createDirectories` at startup so the directory exists before any upload is attempted
  - `store()` generates a UUID-prefixed storage key to avoid filename collisions across users; writes bytes to disk
  - `load()` reads bytes by key; throws `ResourceNotFoundException` if file is missing
  - All uploaded files land in the same directory distinguished by UUID prefix; ownership is enforced by the DB check in the repository, not by filesystem layout
- Created `ResumeRepositoryTest` (`@DataJpaTest` + real PostgreSQL): save and find, tenant isolation (user B gets empty), list scoped to user A only. `@BeforeEach` deletes resumes → job_applications → users in FK-safe order
- Created `LocalStorageServiceTest` (plain unit test, `@TempDir`): store and load round-trip, two stores of the same filename produce different keys, delete then load throws `ResourceNotFoundException`, load of nonexistent key throws `ResourceNotFoundException`
- `uploads/` already in `.gitignore`
- All 7 tests pass

---

## Step 4.2 — Upload, list, download, delete resume
**Date:** 2026-06-18
**Status:** Done

- Configured `spring.servlet.multipart.max-file-size: 5MB` and `max-request-size: 5MB` in `application.yaml` — Spring rejects requests exceeding this before they reach the controller
- Created `resume/dto/ResumeResponse` — exposes id, originalFilename, contentType, sizeBytes, createdAt, updatedAt; deliberately excludes `storageKey` (internal server path) and `extractedText` (populated later in Step 4.3)
- Created `resume/ResumeService`:
  - `upload()` — validates file (not empty, PDF/DOCX only, ≤5 MB), reads bytes, calls `storageService.store()` to write to disk and get a storage key, saves Resume metadata row, returns `ResumeResponse`
  - `list()` — returns metadata for all current user's resumes, newest first
  - `download()` — verifies ownership via `getOwnedResumeOrThrow`, loads bytes from storage using `storageKey`, returns `ResponseEntity<byte[]>` with `Content-Disposition: attachment` and correct `Content-Type` so the client knows the file format
  - `delete()` — deletes bytes from storage first, then deletes DB row; order is intentional — deleting the DB row first and then failing on storage would leave orphaned bytes with no metadata pointer to clean them up
  - `getOwnedResumeOrThrow()` — same 404-for-wrong-owner pattern as job applications
- Created `resume/ResumeController` at `/api/resumes`:
  - `POST /api/resumes` (multipart/form-data) → 201
  - `GET /api/resumes` → 200 list of metadata
  - `GET /api/resumes/{id}/download` → raw bytes with content headers (not wrapped in ApiResponse)
  - `DELETE /api/resumes/{id}` → 204
- Added `MaxUploadSizeExceededException` handler to `GlobalExceptionHandler` → 422 `BUSINESS_RULE_VIOLATION` — fires when Spring itself rejects the file before the controller is reached
- Migrated all `@WebMvcTest` files from deprecated `@MockBean` (Spring Boot 3.4+) to `@MockitoBean` (`AuthControllerTest`, `ApplicationControllerTest`, `ResumeControllerTest`)
- Created `ResumeControllerTest` (`@WebMvcTest` + `@MockitoBean`): valid PDF upload 201, valid DOCX upload 201, unauthenticated upload 401, list 200, download 200 with correct bytes and headers, download not found 404, delete 204
- All tests pass

---
