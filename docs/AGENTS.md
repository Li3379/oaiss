<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# docs/ — Documentation Directory

This directory holds all reference, specification, and planning documents for the OAISS CHAIN carbon accounting platform.

## Contents

### Top-Level Files

| File | Description |
|------|-------------|
| `DATABASE_SCHEMA.md` | Database schema documentation |
| `execution.md` | Execution guide |
| `FIX-SPEC.md` | Fix specifications |
| `ONBOARDING.md` | Developer onboarding guide |
| `phase-01-spec.md` | Phase 1 specification |
| `phase-02-prompts.md` | Phase 2 prompts |
| `phase-03-inner.md` | Phase 3 inner details |
| `phase-04-output.md` | Phase 4 output documentation |
| `product-specification.md` | Product specification |
| `verifiability.md` | Verifiability criteria |

### Subdirectories

| Directory | Description |
|-----------|-------------|
| `raw/` | Original Chinese project requirement docs (5 files) |
| `specs/` | Technical specifications (AI module, blockchain integration, carbon calculation, gap analysis) |
| `superpowers/` | Dated design docs with `plans/` and `specs/` subdirectories (2026-05-03 through 2026-05-10) |

### raw/ — Original Requirements

Chinese-language project requirement documents:

1. `01-项目需求分析.md` — Project requirements analysis
2. `02-项目概要介绍.md` — Project overview introduction
3. `03-项目详细方案.md` — Detailed project plan
4. `04-碳核算模型介绍文档.md` — Carbon accounting model documentation
5. `05-项目测试文档.md` — Project testing documentation

### specs/ — Technical Specifications

| File | Description |
|------|-------------|
| `AI-MODULE-SPEC.md` | AI module specification |
| `BLOCKCHAIN-INTEGRATION-SPEC.md` | Blockchain integration specification |
| `CARBON-CALCULATION-SPEC.md` | Carbon calculation specification |
| `GAP-ANALYSIS.md` | Gap analysis between requirements and implementation |
| `README.md` | Specs directory overview |

## For AI Agents

### Working in This Directory

These docs are reference material. When modifying docs, preserve Chinese content in `raw/` and `tracks/` directories.

### Testing

No code to test here. Verify doc links are valid.

### Patterns

- Use markdown formatting
- Include diagrams where helpful
- Keep Chinese and English content in separate files

## Dependencies

- **Internal**: References the project codebase (entities, services, controllers)
- **External**: None
