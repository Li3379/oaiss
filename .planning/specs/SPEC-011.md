---
status: complete
created: 2026-05-15
author: claude
source: Browser-QA Deep Testing
severity: HIGH
---

# SPEC-011: CERTIFIER Role Does Not Exist - completeCertification Endpoint Unreachable

## Problem

`CarbonNeutralProjectController.java` line 278 uses `@PreAuthorize("hasRole('CERTIFIER') or hasRole('ADMIN')")` for the `completeCertification` endpoint. However, the system only defines 4 roles:

- ADMIN
- ENTERPRISE
- REVIEWER
- THIRD_PARTY

The `CERTIFIER` role does not exist in the database, enum, or security configuration. This means only ADMIN can access this endpoint, and the `CERTIFIER` check is dead code.

## Root Cause

The controller was likely designed with a separate CERTIFIER role in mind, but the system was simplified to use REVIEWER for certification tasks. The `verify` endpoint at line 214 already correctly uses `hasRole('REVIEWER')`.

## Fix

Change `hasRole('CERTIFIER')` to `hasRole('REVIEWER')` on the `completeCertification` endpoint to match the existing pattern in the same controller.

## Affected File

`oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonNeutralProjectController.java` line 278

## Test

After fix, REVIEWER users should be able to access `POST /api/v1/carbon-neutral/{id}/certify`.
