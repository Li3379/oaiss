<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# src/test — Test Suites

Test suites mirroring the main source structure.

## Structure

| Directory | Purpose |
|-----------|---------|
| `java/com/oaiss/chain/` | Unit and integration tests mirroring main source packages |
| `e2e/` | End-to-end business flow test (`full_business_flow.py`, Python) |
| `resources/` | Test configuration (`application-test.yml`) |

## Conventions

- Unit tests: `*Test.java` (Surefire plugin)
- Integration tests: `*IntegrationTest.java` (Failsafe plugin + Testcontainers)
- E2E tests: Python scripts in `e2e/`
- Test config uses H2 in-memory database via `application-test.yml`
- Test classes mirror the package structure of `src/main/java/com/oaiss/chain/`
