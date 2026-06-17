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

---

## Step 2.2 — Register endpoint
**Date:** 2026-06-16
**Status:** Done

- Created `auth/dto/RegisterRequest` — validated DTO: email (required, valid format), password (required, min 8 chars), fullName (optional)
- Created `auth/dto/AuthResponse` — response DTO with token, email, fullName (token is null until Step 2.3 adds JWT)
- Added `PasswordEncoder` bean (`BCryptPasswordEncoder`) to `SecurityConfig`
- Created `auth/AuthService.register`:
  - Checks for duplicate email → throws `DuplicateResourceException` (409)
  - Hashes password with BCrypt before saving
  - Returns `AuthResponse`
- Created `auth/AuthController` with `POST /api/auth/register` → 201 on success
- Created `AuthControllerTest` using `@WebMvcTest` + `@MockBean AuthService`:
  - Valid registration returns 201
  - Invalid email returns 422 with field error
  - Short password returns 422 with field error
  - Duplicate email returns 409
  - Added `@Import(SecurityConfig.class)` to load our CSRF-disabled security config in the test context
- Created `AuthServiceTest` as a pure unit test (no Spring context):
  - Password saved to DB is not plaintext and matches BCrypt hash
- All 16 tests pass: `Tests run: 16, Failures: 0, Errors: 0`
- Manually verified: first register returns 201, second register with same email returns 409

---

## Step 2.3 — Login and JWT
**Date:** 2026-06-16
**Status:** Done

- Added jjwt 0.12.6 dependency (jjwt-api, jjwt-impl, jjwt-jackson) to `pom.xml`
- Added `app.jwt.secret` and `app.jwt.expiration-ms` to `application.yaml` (reads from env vars, defaults for local dev)
- Added JWT config to `application-test.yml` with fixed test secret
- Created `common/exception/InvalidCredentialsException` → 401
- Updated `GlobalExceptionHandler` to handle `InvalidCredentialsException` → 401 `INVALID_CREDENTIALS`
- Created `auth/JwtService`:
  - `generateToken(User)` — signs JWT with HS256, embeds user ID as subject and email as claim
  - `extractUserId(String)` — parses token and returns subject (user UUID)
  - `isTokenValid(String)` — returns true/false without throwing
- Created `auth/dto/LoginRequest` — email (required, valid format) + password (required)
- Updated `AuthService`:
  - `register` now returns a real JWT token (not null)
  - Added `login` — finds user by email, verifies BCrypt hash, returns JWT; both wrong email and wrong password return `InvalidCredentialsException` (same message, no user enumeration)
- Added `POST /api/auth/login` to `AuthController`
- Added 3 login tests to `AuthControllerTest` (200 + token, 401 wrong password, 401 unknown email)
- Updated `AuthServiceTest` — added token validity assertion; fixed mock to set UUID on saved user
- Created `AuthIntegrationTest` using `@SpringBootTest` + real PostgreSQL:
  - Full register → login → JWT validation flow
  - Wrong password returns 401
  - `@BeforeEach` cleans up users table to ensure test isolation
- All 22 tests pass: `Tests run: 22, Failures: 0, Errors: 0`
- Manually verified: login returns real JWT (`eyJhbGci...`), wrong password returns 401

---

## Step 2.4 — Protect APIs and add current user endpoint
**Date:** 2026-06-16
**Status:** Done

- Created `auth/JwtAuthenticationFilter` extending `OncePerRequestFilter`:
  - Reads `Authorization: Bearer <token>` header
  - Validates token with `JwtService`
  - Extracts user ID, loads `User` from DB, sets `Authentication` in `SecurityContext`
  - If no token or invalid token, continues filter chain unauthenticated
- Replaced temporary `SecurityConfig` with real JWT-enforced config:
  - `STATELESS` session policy (no server-side sessions)
  - Public: `/api/auth/**`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`
  - Protected: all other requests require valid JWT
  - Custom `AuthenticationEntryPoint` returns `UNAUTHORIZED` in our `ErrorResponse` format
  - JWT filter inserted before `UsernamePasswordAuthenticationFilter`
- Created `user/dto/UserResponse` — id, email, fullName
- Created `user/UserController` with `GET /api/users/me`:
  - Uses `@AuthenticationPrincipal User` to read the current user from `SecurityContext`
  - Returns 200 with user data
- Updated `AuthControllerTest` — added `@MockBean JwtService` and `@MockBean UserRepository` so `JwtAuthenticationFilter` can be constructed in `@WebMvcTest` context
- Added 2 tests to `AuthIntegrationTest`:
  - Register → call `/me` with token → 200 with correct user
  - Call `/me` without token → 401 `UNAUTHORIZED`
- All tests pass
- Manually verified: valid token returns user data, no token returns 401, fake token returns 401
