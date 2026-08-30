# Eşik Documentation Map

Use this file to distinguish current product/release documentation from historical hackathon planning material.

## Current source of truth

Read these for any new work:

1. `PRODUCT_SPEC.md` — current product behavior and boundaries
2. `FINALIZATION.md` — validated baseline, active Git workflow, release gates
3. `DATA_SCHEMA.md` — persisted device data + current structured AI contracts
4. `PROMPT_DESIGN.md` — live Gemini prompt design and rationale
5. `AI_EVALUATION.md` — AI quality/safety scenarios and observed final-gate results
6. `AI_DEVICE_QA.md` — device procedures for live/offline AI checks
7. `VALIDATION.md` — Android/emulator/physical validation record
8. `HACKATHON_REPORT.md` — submission-ready 1–2 page project report
9. `DEMO_SCRIPT.md` — frozen five-minute route and recording gate

Repository-level coding instructions live in `../AGENTS.md`, `../COPILOT_PROMPT.md`, and `../.github/copilot-instructions.md`.

## Supporting implementation documentation

These remain useful when investigating how a subsystem was built, but they do not override the current source-of-truth documents above:

- `AI_PERSONALIZATION_IMPLEMENTATION.md`
- `PROMPT_DESIGN.md`
- `AI_DEVICE_QA.md`

## Historical planning / handoff documents

The following capture earlier parallel-sprint plans and should **not** be used as the active Git workflow or implementation baseline:

- `TEAM_PLAN.md`
- `IMPLEMENTATION_HANDOFF.md`
- `PARALLEL_SPRINT_PLAN.md`
- `UI_PRODUCT_REDESIGN_HANDOFF.md`
- `AI_QUALITY_V2_HANDOFF.md`

They are intentionally retained as implementation history for the hackathon, including design decisions and task decomposition.

## Active workflow summary

```text
main
  ^
  | PR #7
  |
feature/final-integration
```

The repository is under absolute feature freeze. Only a reproduced crash or demo-blocking defect may branch from `feature/final-integration`. PR #7 remains the single final PR to protected `main`; it must not be merged without the project owner's explicit final authorization.
