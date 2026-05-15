<!-- Parent: ../../../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# Test Package — com.oaiss.chain

Test classes mirroring the main source package structure under `src/main/java/com/oaiss/chain/`.

## Key Files

| File | Purpose |
|------|---------|
| `BaseIntegrationTest.java` | Base class for integration tests (Testcontainers setup) |
| `OaissChainApplicationTests.java` | Application context smoke test |

## Subdirectories

These mirror the main source package layout:

| Directory | Purpose |
|-----------|---------|
| `aop/` | Tests for AOP aspects (audit log, rate limiting, distributed lock) |
| `config/` | Tests for Spring configuration classes |
| `controller/` | Tests for REST controllers |
| `dto/` | Tests for DTO validation |
| `entity/` | Tests for JPA entity constraints |
| `exception/` | Tests for custom exceptions and global handler |
| `integration/` | End-to-end integration tests |
| `repository/` | Tests for Spring Data JPA repositories |
| `security/` | Tests for JWT filter and security configuration |
| `service/` | Tests for business logic services |
| `util/` | Tests for utility classes |

## Conventions for AI Agents

- **Unit tests**: `*Test.java` — run by Maven Surefire plugin (`mvn test`)
- **Integration tests**: `*IntegrationTest.java` — run by Maven Failsafe plugin with Testcontainers (`mvn verify`)
- **Test naming**: `methodName_WhenCondition_ShouldExpectedBehavior()`
- **JaCoCo coverage target**: 90% line coverage
- **Testcontainers**: MySQL 8 and Redis 7 containers spin up automatically via `BaseIntegrationTest`
