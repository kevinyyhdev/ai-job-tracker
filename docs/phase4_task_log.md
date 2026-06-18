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
