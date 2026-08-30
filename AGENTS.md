# Agent Instructions

Read these before editing:

1. `.github/copilot-instructions.md`
2. `docs/PRODUCT_SPEC.md`
3. `docs/DATA_SCHEMA.md`
4. `docs/FINALIZATION.md`

## Current repository state

The validated integration baseline is `feature/final-integration`. The earlier role/parallel sprint branches are implementation history and must not receive new work. Absolute feature freeze was declared after PR #18.

Do not add behavior inference, reports, screens, redesigns, polling changes, prompt/model changes, or experiments. Reopen code only for a reproduced crash or demo-blocking defect. Any such fix must:

1. start from the latest `feature/final-integration` on one narrow `fix/<name>` branch;
2. avoid a second integration branch;
3. run `./gradlew test` and `./gradlew assembleDebug`;
4. perform the smallest emulator/physical-device check that exercises the fix;
5. merge back into `feature/final-integration` through review;
6. keep PR #7 as the single final PR from the frozen candidate to `main`.

Do not merge PR #7 to `main` unless the project owner explicitly requests the final merge.

## Non-negotiable product rules

- Preserve the user-defined threshold: AI never recommends, judges, or changes it.
- Never diagnose addiction or another medical/mental-health condition.
- Never shame, moralize, label the person, or state unsupported causation.
- Crisis-signalling external text must be handled locally and must not enter the normal Gemini/repair path.
- Numeric report facts are computed locally.
- Keep the deterministic fallback usable.
- Keep user profile/records device-local and preserve one-action data deletion.
- Never commit secrets or a real `local.properties` file.
- Direct mobile Gemini access is hackathon-only; production would require a safer credential/backend design.

## Architecture boundaries

- `ui/` — Compose product UI
- `overlay/` — real system intervention overlay
- `permissions/`, `usage/`, `monitor/` — Android core
- `ai/` — Gemini transport, prompts, policy compiler, validators, fallback
- `data/` — local models/repository/demo records
- `EsikApp.kt` — integration/navigation boundary

Do not casually refactor cross-cutting architecture while implementing a small feature. Prefer a narrow change with explicit ownership.

## Validation expectations

For Android-core/overlay/voice changes, test the affected path on a physical phone when possible and report device/Android version. For pure logic, add/update unit tests. For AI changes, rerun the relevant golden scenarios in `docs/AI_EVALUATION.md`, plus offline fallback when network behavior changed.

User-facing copy is Turkish. Code, commit messages, and technical documentation are primarily English.
