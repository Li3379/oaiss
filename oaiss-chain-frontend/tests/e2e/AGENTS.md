<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# e2e/

End-to-end Playwright test suites organized by category.

## Subdirectories

- **fixtures/** — Test fixtures, auth helpers, page objects (see fixtures/AGENTS.md)
- **flows/** — Full business flow E2E tests (13 spec files)
- **smoke/** — Quick smoke tests per role (5 spec files)

## Notes

Run with `npx playwright test`. Flows test complete user journeys; smoke tests verify basic page load and navigation.
