---
phase: 18-fabric-ca
date: 2026-05-22
---

# Phase 18 Discussion Log

## Phase Overview

Phase 18 covers 1 requirement: FABRIC-01 (Fabric CA registerEnrollment integration).

## Analysis

### Current State (Greenfield for CA)

Codebase scouting revealed:
- `FabricProperties.Ca` inner class exists with `enabled`, `endpoint`, `adminName`, `adminPassword` fields
- `docker-compose.fabric.yml` defines `ca.org1.example.com:7054` service
- `FabricGatewayConfig` reads static crypto from classpath (user-cert.pem, user-key.pem)
- Zero Java-side CA integration — no service class, no enrollment flow, no conditional logic
- `isFabricAvailable()` E2E guard exists from Phase 16

### Key Observations
1. The CA Docker infrastructure is ready — no infra work needed
2. The config scaffold (`FabricProperties.Ca`) exists but is unused
3. Gateway always boots from static files — CA enrollment would be a conditional alternative
4. Phase is LOW priority / optional — scope should be minimal

## Decisions Made

| ID | Decision | Rationale |
|----|----------|-----------|
| D-01 | Gateway admin enrollment only | Single registerEnrollment(), FabricProperties.Ca has no enterprise fields |
| D-02 | Graceful fallback to static crypto | Success criterion #2: CA unavailable doesn't break basic function |
| D-03 | No BlockchainServicePort changes | CA is connection-layer, not business operation |
| D-04 | FabricCAService as standalone @Profile("fabric") | Follows established pattern |
| D-05 | E2E: small describe block in existing test | Minimal footprint for optional feature |

## User Interaction

User chose "自行分析并决策" — deferred all decisions to Claude's analysis.
