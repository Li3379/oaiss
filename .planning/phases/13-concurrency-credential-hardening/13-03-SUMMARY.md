---
phase: 13-concurrency-credential-hardening
plan: 03
subsystem: security
tags: [authorization, preauthorize, x-user-id-removal, actuator-auth]
dependency_graph:
  requires: [SEC-10, SEC-11, SEC-12, SEC-13]
  provides: [role-based-file-endpoints, no-header-spoofing, search-auth, prometheus-auth]
  affects: [FileController, SearchController, SecurityConfig, JwtAuthenticationFilter]
tech_stack:
  added: []
  patterns: ["@PreAuthorize method-level security", "Spring Security requestMatchers ordering"]
key_files:
  created: []
  modified:
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/SearchController.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/security/JwtAuthenticationFilter.java
decisions:
  - FileController class-level uses isAuthenticated() with method-level overrides for tighter roles
  - ADMIN-only for list-all-files and copy-file operations (sensitive administrative functions)
  - ENTERPRISE+ADMIN for upload and presigned URL operations (core business workflow)
  - SearchController uses isAuthenticated() as all roles need search capability
  - Prometheus requires ADMIN role; health remains open for k8s probes
  - JwtAuthenticationFilter whitelist narrowed from /actuator to /actuator/health only
metrics:
  duration: 8m
  completed: 2026-05-19
  tasks: 3
  files: 4
---

# Phase 13 Plan 03: Authorization Hardening Summary

Role-based @PreAuthorize on FileController and SearchController; X-User-Id/X-User-Type header fallback removed; Prometheus endpoint requires ADMIN authentication.

## Changes Made

### Task 1: Add @PreAuthorize to FileController and remove X-User-Id fallback (SEC-10, SEC-11)

**File:** `FileController.java`

- Added class-level `@PreAuthorize("isAuthenticated()")` -- all endpoints require authentication
- Added method-level overrides:
  - Upload endpoints (upload, uploadBatch): `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")`
  - Presigned URL endpoints (presigned-url, presigned-upload-url): `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")`
  - List all files: `@PreAuthorize("hasRole('ADMIN')")`
  - Copy file: `@PreAuthorize("hasRole('ADMIN')")`
  - Download, info, exists, delete: inherit class-level `@PreAuthorize("isAuthenticated()")`
- Removed `X-User-Id`/`X-User-Type` header fallback from `resolveUserId()` and `resolveUserType()`
- Simplified both methods to use `JwtUserDetails` parameter only (no `HttpServletRequest`)
- Updated `checkDeletePermission()` and both delete endpoints to remove `HttpServletRequest` parameter
- Removed unused `HttpServletRequest` import

### Task 2: Add @PreAuthorize to SearchController (SEC-12)

**File:** `SearchController.java`

- Added class-level `@PreAuthorize("isAuthenticated()")` -- all 3 endpoints require authentication
- All authenticated roles (ENTERPRISE, REVIEWER, THIRD_PARTY, ADMIN) can access search
- Anonymous search blocked entirely

### Task 3: Require authentication for Prometheus endpoint (SEC-13)

**File:** `SecurityConfig.java`

- Split combined `/actuator/health` + `/actuator/prometheus` permitAll into separate rules
- `/actuator/health` remains `permitAll()` (k8s liveness/readiness probes)
- `/actuator/prometheus` changed to `hasRole("ADMIN")`
- Rule ordering preserved: health permitAll, prometheus ADMIN, actuator/** authenticated

**File:** `JwtAuthenticationFilter.java`

- Changed whitelist from blanket `/actuator` to specific `/actuator/health`
- `/actuator/prometheus` now goes through JWT validation

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

| Check | Result |
|-------|--------|
| No X-User-Id/X-User-Type in controller layer | PASS |
| FileController @PreAuthorize count | 7 (1 class + 6 method-level) |
| SearchController @PreAuthorize count | 1 (class-level) |
| SecurityConfig prometheus rule | hasRole("ADMIN") |
| JwtAuthenticationFilter whitelist | /actuator/health only |
| Maven compile | PASS |

## Self-Check: PASSED
