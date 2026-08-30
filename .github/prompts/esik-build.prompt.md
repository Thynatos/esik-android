---
agent: 'agent'
description: 'Fix one reproduced Eşik crash or demo blocker after feature freeze'
---

Implement only a reproduced crash or demo-blocking fix without disturbing the validated baseline. Optional features and experiments are frozen.

Before editing:

1. Read `COPILOT_PROMPT.md` completely; it is the canonical executor prompt.
2. Read `AGENTS.md`, `.github/copilot-instructions.md`, `docs/PRODUCT_SPEC.md`, `docs/DATA_SCHEMA.md`, and `docs/FINALIZATION.md`.
3. If the request changes AI behavior, also read `docs/AI_EVALUATION.md` and `docs/PROMPT_DESIGN.md`.
4. Inspect the actual files involved before proposing or making edits.
5. Confirm the work is based on the latest `feature/final-integration`; old sprint branches are historical.

Keep the change narrow. Do not add behavior inference, reports, screens, redesigns, polling changes, or prompt/model experiments. Preserve the user-owned threshold, local crisis gate, device-local records, structured/validated AI behavior, deterministic fallback, and existing Android monitoring/overlay behavior.

Run `./gradlew test` and `./gradlew assembleDebug` before finishing. Perform the smallest emulator/physical-device check that actually exercises the changed behavior. Do not repeat unrelated edge-case matrices for a small feature.

Do not merge `main` or PR #7. Final output should list the branch, files changed, behavior implemented, validation performed/results, relevant device check, remaining limitation, and whether fallback/privacy/safety behavior remains intact.
