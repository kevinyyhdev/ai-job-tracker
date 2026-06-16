# Phase 2 — Task Log

## Step 2.1 — Create users table
**Date:** 2026-06-16
**Status:** Done

- Created Flyway migration `db/migration/V1__create_users.sql`:
  - `id` UUID primary key with `gen_random_uuid()` default
  - `email` VARCHAR unique, not null
  - `password_hash` VARCHAR not null (never stores plaintext)
  - `full_name` VARCHAR nullable
  - `role` VARCHAR not null, defaults to `'USER'`
  - `created_at`, `updated_at` TIMESTAMP WITH TIME ZONE with `now()` defaults
  - Index on `email` for fast lookup
- Created `user/User.java` JPA entity mapping to `users` table
  - UUID primary key with `@GeneratedValue(strategy = GenerationType.UUID)`
  - `@CreationTimestamp` / `@UpdateTimestamp` for automatic timestamps
  - Constructor `(email, passwordHash, fullName)` — role defaults to `USER`
- Created `user/UserRepository.java` extending `JpaRepository<User, UUID>`
  - `findByEmail(String email)` — used at login and duplicate check
  - `existsByEmail(String email)` — used at registration to reject duplicates
- Created `UserRepositoryTest` using `@DataJpaTest` + real PostgreSQL (no H2 mock)
  - Save and find by email
  - `existsByEmail` returns true for existing email
  - `existsByEmail` returns false for unknown email
  - Duplicate email throws `DataIntegrityViolationException`
- All 11 tests pass: `Tests run: 11, Failures: 0, Errors: 0`
- Verified `users` table in local Docker DB with `\d users`
