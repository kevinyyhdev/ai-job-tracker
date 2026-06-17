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
