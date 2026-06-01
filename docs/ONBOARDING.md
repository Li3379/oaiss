# OAISS CHAIN Onboarding

## Start Here

The maintained developer takeover guide is:

- `docs/stability-first-handover.md`

Use it as the primary entrypoint for:

- environment baseline verification
- login and core business smoke coverage
- ML and Fabric extension checks
- delivery guard and release safety validation

## Quick Commands

### Core takeover baseline

```bash
bash ./scripts/stability-baseline.sh
```

### Baseline with extension and delivery checks

```bash
bash ./scripts/stability-baseline.sh --with-ml-health --with-blockchain --with-delivery-guards
```

### Windows wrapper

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stability-baseline.ps1
```

## Reference Documents

- `docs/stability-first-handover.md`
- `docs/final-acceptance-checklist.md`
- `docs/go-live-gate-matrix.md`
- `docs/production-readiness.md`
- `docs/deployment-runbook.md`

## Notes

- This file is now a lightweight pointer to the maintained takeover flow.
- The previous onboarding content had encoding issues and overlapping guidance.
- The stability-first handover guide is the authoritative developer entrypoint going forward.
