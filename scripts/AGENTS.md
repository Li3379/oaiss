<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# scripts

This directory contains shell scripts for API endpoint testing and operational tasks for the OAISS CHAIN platform.

Key Files (all are bash scripts):
- admin-test.sh — Admin endpoint tests
- aop-test.sh — AOP aspect tests
- blockchain-test.sh — Blockchain API tests
- bugfix-test.sh — Bug fix verification tests
- carbon-report-test.sh — Carbon report API tests
- cleanup-test-data.sh — Test data cleanup
- coin-balance-test.sh — Carbon coin balance tests
- credit-score-test.sh — Credit score API tests
- db-config.sh — Database configuration
- double-auction-test.sh — Double auction API tests
- edge-test.sh — Edge case tests
- emission-test.sh — Emission data API tests
- file-test.sh — File upload/download tests
- health-check.sh — Service health check
- login-test.sh — Login/auth tests
- p2p-trade-test.sh — P2P trading tests
- project-lifecycle-test.sh — Carbon neutral project lifecycle tests
- search-test.sh — Search API tests
- sign-test.sh — Digital signature tests
- skill-verify.sh — Skill verification
- thirdparty-test.sh — Third-party monitor tests

## For AI Agents

- **Working**: All scripts use curl against localhost:8080. Ensure backend is running first. Scripts require bash.
- **Testing**: Run individual scripts with `bash scripts/<name>.sh`. Check exit codes.
- **Patterns**: Scripts use curl + jq for API calls, expect JSON responses from ApiResponse<T> envelope.

## Dependencies

- **Internal**: oaiss-chain-backend must be running on port 8080
- **External**: bash, curl, jq
