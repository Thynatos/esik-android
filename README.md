# Eşik — Android Hackathon Starter

**Eşik** is an Android digital-wellbeing prototype that intervenes after a user exceeds a limit they set for one app. It asks “What is happening right now?”, generates a neutral personalized reflection card, and records whether the user continued or stopped.

This repository is structured for a two-day, four-person hackathon. User-facing demo copy is Turkish; code and technical documentation are English.

## Product boundary

Eşik does not diagnose addiction, decide what “too much” means, or shame the user. It only compares numeric use with the user’s own target.

Exactly four product screens are in scope:

1. Onboarding
2. Home
3. Intervention card
4. Daily report

Debug controls may live on the Home screen; do not create a fifth screen.

## Team workflow

We use a deliberately simple GitHub Flow. There is no `dev`, `test`, or `staging` branch.

```text
main
├── work/android-core
├── work/ui
├── work/ai
└── work/integration
```

Rules:

- `main` is always the demo-ready branch.
- Nobody develops directly on `main`.
- Each person works only on their assigned `work/...` branch.
- Changes reach `main` through a Pull Request.
- `main` is protected by the repository ruleset: direct push, force push, and branch deletion are blocked; PR review is required.
- Prefer small PRs that compile independently instead of one huge Sunday-night merge.
- Do not have two people editing the same shared file at the same time.

## Team ownership

### A — Android Core — Bahadır / `work/android-core`

**Owner: Bahadır (project owner). This is the highest-risk technical track.**

Responsibilities:

- Usage Access permission flow
- Draw Over Other Apps permission flow
- `UsageStatsManager` reads for the selected package
- Foreground service and 60-second polling
- Foreground/target-app detection
- User-defined threshold trigger
- 15-minute intervention cooldown
- `TYPE_APPLICATION_OVERLAY` behavior
- Overlay lifecycle, keyboard/back behavior, and device/OEM edge cases
- **Yine de gir:** dismiss the card and keep/return the target app in front
- **Vazgeçtim:** dismiss the card and move away from the target app
- Physical-device validation of the system trigger

Primary ownership:

```text
permissions/
usage/
monitor/
overlay/
app/src/main/AndroidManifest.xml   (coordinate shared edits)
```

**Saturday noon gate:** the intervention card must actually appear above the selected app on the physical demo phone after the user exceeds the configured limit. If this fails, A stays focused on this risk until it works or a documented demo fallback exists.

### B — UI / Compose — `work/ui`

Responsibilities:

- Onboarding screen
- Home screen and usage/limit presentation
- Intervention card UI and states
- Daily report screen
- Installed-app picker UX
- Limit input/edit UX
- Loading, empty, and error states
- Final visual polish and Turkish copy consistency

Primary ownership:

```text
ui/
screens/
components/
```

B must build against interfaces/mocks and must not wait for the real Gemini API or Android service implementation.

### C — AI & Safety — `work/ai`

Responsibilities:

- Provide a fixed-JSON/mock AI implementation in the first 30 minutes
- Profile, intervention-card, and daily-report prompts
- Gemini REST integration for the hackathon prototype
- Strict JSON parsing
- Timeout/error handling and deterministic fallback output
- Crisis-language gate before any AI call
- Generated-language validation
- Block diagnostic/judgmental language
- Preserve the rule that AI never decides the threshold and only references the user’s own target

Primary ownership:

```text
ai/
network/
safety/
prompts/
```

The mock path must remain working even after the real API is connected. This prevents B and D from being blocked by API work.

### D — Data, Integration & Demo — `work/integration`

Responsibilities:

- Freeze the shared data contract first thing Saturday morning
- Device-local JSON/Room persistence
- Repository/data layer
- UI ↔ data wiring
- service ↔ data wiring
- AI ↔ data wiring
- Today-record aggregation and report eligibility
- 7-record minimum behavior
- 3–4 days of prepared demo data
- Integration tests and cross-feature bug fixing
- Coordination of shared files and navigation
- README/report/ethics/demo-flow maintenance
- Demo-state preparation and final submission coordination

Primary ownership:

```text
data/
repository/
model/
demo/
integration/
docs/
```

D owns coordination for high-conflict shared files such as `EsikApp.kt`, navigation, shared dependency wiring, and frozen data-model files. D is not responsible for rescuing incomplete work at the end; every role should submit a working PR.

## Critical dependency rules

1. **Freeze the data model first.** D publishes the approved contract Saturday morning before parallel implementation drifts.
2. **Mock AI first.** C ships a fixed-JSON implementation in the first 30 minutes, so UI/integration never waits for Gemini.
3. **Physical overlay checkpoint at noon.** A validates the highest-risk Android behavior on the actual demo phone.
4. **One owner per file.** Coordinate edits to shared files before touching them.
5. **Keep `main` green/demoable.** Merge only reviewable, working increments.

## Weekend plan

### Saturday

| Time | A — Android Core | B — UI | C — AI & Safety | D — Data / Integration |
|---|---|---|---|---|
| Morning | Permissions + usage reads | Onboarding + Home | Mock gateway + prompt v1 | Freeze schema + persistence |
| Noon | **CHECKPOINT: overlay appears above target app on physical phone** | Intervention UI | Real API behind existing interface | UI/data integration |
| Afternoon | Cooldown + continue/stop behavior | Finish intervention states | Prompt v2 + parser + safety | Service/data/AI wiring |
| Evening | Four-person end-to-end first pass | Same | Same | Integration coordination |

If the noon checkpoint fails, A keeps working on it. B, C, and D continue using mocks/interfaces.

### Sunday

| Time | A — Android Core | B — UI | C — AI & Safety | D — Data / Integration |
|---|---|---|---|---|
| Morning | Edge cases, battery/OEM, lock screen | Daily report UI | Report prompt | Daily aggregation + eligibility |
| Noon | Device/integration support | Finish report UI | Crisis + validator tests | Integration + bug fixing |
| Afternoon | Demo phone preparation | Visual polish | Prompt freeze + fallback verification | Demo dataset + report/ethics |
| Evening | Demo rehearsal ×3 + backup video + submission | Same | Same | Same |

Never arrive at the demo with an empty report screen. D should maintain seeded data across 3–4 days, with enough current-day records for the report.

## Current feature-branch implementation

The `feature/ai-personalization` branch currently includes:

- narrative-first onboarding through voice or editable text;
- AI-generated structured profile with goals, contexts, preferred activities, low-energy alternatives, tone, and six quick states;
- three instant profile-aware replies in both the Compose intervention and real system overlay;
- custom text and voice input;
- enriched intervention records and cautious daily synthesis;
- a timeout-bounded Gemini `generateContent` REST client;
- deterministic offline fallback for blank key, airplane mode, quota/error, blocked generation, malformed JSON, and unsafe output;
- backward-compatible schema-v1 reads;
- Android CI for unit tests and debug APK assembly.

See:

- `docs/AI_PERSONALIZATION_IMPLEMENTATION.md`
- `docs/AI_DEVICE_QA.md`
- `docs/DATA_SCHEMA.md`

## What is already scaffolded

- Four Jetpack Compose screens and an in-app debug flow
- Installed launchable-app picker
- Device-local JSON persistence in `filesDir/esik_state.json`
- Deterministic mock profile, card, and report generation
- Local crisis-language gate that runs before AI
- Banned-language output validator with fallback
- 15-minute cooldown policy
- `UsageStatsManager` permission checks and daily usage reads
- User-started foreground service polling every 60 seconds
- `TYPE_APPLICATION_OVERLAY` intervention window
- Four-day demo data with enough records today to show a report
- Unit tests for cooldown, crisis filtering, output language, report eligibility, date grouping, and AI fallback
- Copilot context in `.github/copilot-instructions.md`, `AGENTS.md`, `COPILOT_PROMPT.md`, and the reusable `/esik-build` prompt file

## What still requires physical-device validation

- Live Gemini profile/card/report calls using the local demo key
- Voice cancellation and recognizer-unavailable fallback
- Voice from the real overlay and late-result handling
- Airplane-mode fallback after a live build
- Crisis short-circuit
- Existing-profile migration and complete data deletion
- Lock-screen, battery/OEM, and long-running service behavior

## First setup

The source tree pins Android Gradle Plugin 9.3.0, Kotlin/Compose Compiler 2.3.21, Compose BOM 2026.08.00, `compileSdk 37`, `targetSdk 36`, and Java 17.

Run the Gradle Wrapper bootstrap once from the repository root.

### Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap-gradle-wrapper.ps1
```

### macOS or Linux

```bash
./scripts/bootstrap-gradle-wrapper.sh
```

Then:

1. Install Android Studio, Android SDK Platform 37, and JDK 17.
2. Copy `local.properties.example` to `local.properties` and set `sdk.dir` if Android Studio does not create it.
3. Add `GEMINI_API_KEY` to `local.properties` only for the live hackathon test.
4. Open the repository in Android Studio and sync Gradle.
5. Run on a physical Android phone.
6. Grant **Usage Access** and **Draw over other apps** from the app’s guided Settings buttons.
7. Use **4 günlük demo verisi yükle** and **Kart ekranını test et** while validating the system trigger.

Example local configuration:

```properties
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
GEMINI_API_KEY=<your-local-demo-key>
GEMINI_FAST_MODEL=gemini-2.5-flash-lite
GEMINI_REPORT_MODEL=gemini-2.5-flash
```

## Build and test

```bash
./gradlew test
./gradlew assembleDebug
```

A physical phone is required to validate UsageStats accuracy, target-app detection, foreground-service behavior, overlays, voice, lock-screen behavior, and OEM battery restrictions. See `docs/VALIDATION.md` and `docs/AI_DEVICE_QA.md`.

## Secret handling

Never commit a Gemini API key. A mobile-direct Gemini request may be used only for the hackathon demo. The ignored local key is still embedded in the resulting APK and can be extracted; a production version therefore needs a backend proxy, server-held or short-lived credentials, abuse controls, and an explicit logging/redaction policy.

See `docs/TEAM_PLAN.md`, `docs/AI_PERSONALIZATION_IMPLEMENTATION.md`, and `docs/VALIDATION.md` for deeper project guidance.
