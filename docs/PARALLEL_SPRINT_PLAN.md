# Eşik — Parallel Finish Sprint

This plan freezes the validated functional base and separates the remaining work into two low-conflict tracks.

## Branches

```text
feature/ai-personalization        # validated shared base
├── feature/ui-product-redesign   # Codex / UI track
└── feature/ai-quality-v2         # ChatGPT / AI-quality track
```

Neither track should modify the other track's owned files. Integration happens only after both branches build independently.

## Current status

- **UI track:** active on `feature/ui-product-redesign`; physical visual QA and final screenshots remain.
- **AI track:** quality-v2 architecture is implemented on `feature/ai-quality-v2`; latest CI and the live Gemini scenario matrix are the remaining gates.
- **Integration:** not started. Keep both PRs draft until their branch-specific checks pass.

## Ownership

### Track U — Product UI — `feature/ui-product-redesign`

Owner: Codex working locally with Android build/ADB access.

Owns:

- `app/src/main/java/com/thynatos/esik/ui/**`
- `app/src/main/java/com/thynatos/esik/ui/theme/**`
- UI resources
- visual-only changes in `overlay/OverlayController.kt`
- device screenshots and visual QA

Must not change:

- `ai/**`
- Gemini prompts/transport
- data models and persistence
- usage monitoring, permissions, or cooldown

Detailed handoff: `docs/UI_PRODUCT_REDESIGN_HANDOFF.md` on the UI branch.

### Track A — AI Quality — `feature/ai-quality-v2`

Owner: ChatGPT through the GitHub connector, with GitHub Actions as automated validation and the demo phone used for manual A/B checks.

Owns:

- `app/src/main/java/com/thynatos/esik/ai/**`
- AI tests
- `docs/PROMPT_DESIGN.md`
- `docs/AI_EVALUATION.md`
- narrowly necessary AI model/configuration fields

Must not change:

- `ui/**`
- theme or visual resources
- overlay visuals
- monitoring, permission, or cooldown behavior

Detailed handoff: `docs/AI_QUALITY_V2_HANDOFF.md` on the AI branch.

## Sprint 0 — Freeze and baseline

Both tracks:

1. Pull the correct branch.
2. Inspect the actual repository before editing.
3. Run `./gradlew test` and `./gradlew assembleDebug`.
4. Record any pre-existing failures.
5. Do not change shared interfaces without coordinating first.

Exit criterion: both branches start green and both agents acknowledge ownership boundaries.

## Sprint 1 — Highest-value foundations

### UI

- Define product colors, typography, shapes, spacing, and shared components.
- Redesign Home into a clear product dashboard.
- Move test controls into a discreet developer area.

Exit criterion: Home no longer looks like a debug form; all callbacks still work.

### AI — implemented

- Defined quality rubric and golden scenarios.
- Added local intervention context/strategy compiler.
- Added state-specific energy, strategy, duration, and grounding constraints.

Exit criterion: the same profile produces distinct tired, procrastinating, intentional-rest, boredom, waiting, habit, and late-night policies.

## Sprint 2 — Core experience

### UI

- Redesign onboarding.
- Redesign Compose intervention and real system overlay.
- Preserve voice, loading, crisis, and final-choice behavior.

Exit criterion: onboarding through intervention looks like one coherent product.

### AI — implemented

- Added prompt v2 with compact contrastive few-shot examples.
- Added structured JSON schemas and task-specific generation settings.
- Added semantic card validation and one bounded repair attempt.
- Preserved deterministic fallback.

Exit criterion: cards are parseable, state-appropriate, grounded, actionable, autonomy-preserving, and safe—or fall back locally.

## Sprint 3 — Report and hardening

### UI

- Redesign Daily Report.
- Test scrolling, keyboard overlap, large text, small screens, and dark/light behavior.
- Capture screenshots of all four screens and overlay states.

### AI — implemented pending device matrix

- Added profile grounding sanitizer.
- Added local report evidence aggregates.
- Added evidence-aware report validation.
- Added task-specific model overrides and privacy-safe Logcat diagnostics.
- Updated exact prompt/rationale and device QA documentation.

Exit criterion: CI is green and the live/offline scenario matrix is completed on the demo phone.

## Sprint 4 — Integration

1. Ensure PR #4 and PR #5 are individually green.
2. Merge AI Quality v2 into the shared base first.
3. Rebase or merge the UI branch onto that updated base.
4. Resolve only intentional shared-file edits.
5. Build and install the combined candidate.
6. Run onboarding, monitoring, real overlay, voice/text, live/fallback AI, both final actions, report, crisis route, and data persistence.
7. Run the exact demo route twice.
8. Freeze features.

## Sprint 5 — Submission

- Finish the 1–2 page report using `docs/PROMPT_DESIGN.md` and `docs/AI_EVALUATION.md`.
- Finish the five-minute demo script.
- Record the main demo and a backup video.
- Verify API-key hygiene and the final repository state.

## Stop rules

- Do not add another major product feature after integration starts.
- Do not merge either branch while its CI or branch-specific QA is failing.
- Do not make model changes after the A/B matrix is frozen.
- Do not refactor Android Core during visual/AI integration unless a concrete regression is reproduced.
