# Eşik

Eşik is an Android digital-wellbeing prototype that creates a short, personalized pause after the user reaches a daily usage threshold **they set for one app**.

The app does not diagnose addiction, decide what "too much" means, or force the user to stop. It reads local Android usage data, asks for the user's current context, and offers one brief reflection plus a realistic micro-alternative. The user can intentionally continue or stop.

## Current product

Eşik currently includes:

- narrative-first onboarding by text or Android speech recognition;
- Gemini-assisted profile structuring from user-supplied goals, contexts, and activities;
- a user-selected target app and user-defined daily threshold;
- `UsageStatsManager` usage reads and foreground-app detection;
- a foreground monitoring service with responsive 5-second polling;
- a real `TYPE_APPLICATION_OVERLAY` intervention over the target app;
- three personalized local quick states plus custom text and voice input;
- policy-constrained Gemini intervention cards with semantic validation and one bounded repair attempt;
- deterministic local fallback when Gemini is unavailable or an output is rejected;
- local crisis-language short-circuit before any AI request;
- device-local intervention records and a cautious daily reflection after at least seven records;
- a redesigned Compose UI for onboarding, Home, intervention, and daily report;
- full local data deletion;
- backup/transfer exclusions for the local profile, records, and monitoring state.

User-facing copy is Turkish. Technical documentation and source comments are primarily English.

## Product boundary

The following rules are part of the product contract:

- The **user sets the threshold**. AI never recommends or changes it.
- Eşik does not diagnose addiction, mental-health conditions, personality traits, or motives.
- Generated language must remain tentative, non-shaming, and autonomy-preserving.
- Crisis-signalling text stays on-device and bypasses Gemini.
- Numeric report facts are computed locally; Gemini receives constrained evidence and writes the reflection only.
- No account is required. Profile and intervention records are stored in the app's private local storage.

## Architecture

```text
Compose UI / system overlay
        |
        v
EsikRepository (device-local JSON)
        |
        +--> UsageStatsReader + UsageMonitorService
        |
        +--> GeminiAiGateway
                |
                +--> local context / strategy compiler
                +--> structured prompt
                +--> semantic validation
                +--> one repair attempt
                +--> deterministic MockAiGateway fallback
```

Important implementation areas:

```text
app/src/main/java/com/thynatos/esik/ui/          product UI
app/src/main/java/com/thynatos/esik/overlay/     real system overlay
app/src/main/java/com/thynatos/esik/monitor/     foreground monitoring
app/src/main/java/com/thynatos/esik/usage/       UsageStats + cooldown logic
app/src/main/java/com/thynatos/esik/ai/          Gemini, policy, safety, fallback
app/src/main/java/com/thynatos/esik/data/        local models and persistence
```

## Hackathon AI configuration

The direct mobile Gemini integration is for the hackathon prototype only. The API key is read from ignored `local.properties`, but it is still embedded in the built APK and must be rotated/removed after the demo. A production architecture needs a backend proxy or short-lived credentials.

Copy `local.properties.example` to `local.properties` and configure:

```properties
sdk.dir=C:/path/to/Android/Sdk
GEMINI_API_KEY=your-local-demo-key
GEMINI_PROFILE_MODEL=gemini-2.5-flash-lite
GEMINI_CARD_MODEL=gemini-2.5-flash-lite
GEMINI_REPORT_MODEL=gemini-3.6-flash
```

The final combined QA used the two `2.5-flash-lite` fast tasks and `gemini-3.6-flash` for the report. Model values remain locally overridable.

## Build

Requirements:

- JDK 17
- Android SDK Platform 37
- Android Gradle Plugin 9.3.0
- Gradle 9.5.0
- Kotlin / Compose compiler 2.3.21
- `compileSdk 37`, `targetSdk 36`, `minSdk 26`

Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

macOS/Linux:

```bash
./gradlew test
./gradlew assembleDebug
```

A physical Android phone is required for final validation of Usage Access, foreground detection, overlays, voice recognition, and service behavior.

## Validated baseline

Current combined candidate branch:

```text
feature/final-integration
```

Validated on emulator and physical Android hardware:

- onboarding and grounded profile generation;
- Home dashboard and target usage display;
- Usage Access and overlay permission flow;
- monitoring service and threshold trigger;
- real overlay above the selected target app;
- tired-state and procrastination-state live cards;
- custom text and physical-device voice input;
- both final decisions;
- airplane-mode deterministic fallback;
- seeded daily-report flow;
- live report generation after the schema/token-budget fix;
- Gradle unit tests and debug APK assembly in GitHub Actions.

See `docs/FINALIZATION.md`, `docs/AI_DEVICE_QA.md`, `docs/AI_EVALUATION.md`, and `docs/VALIDATION.md` for details.

## Frozen submission workflow

The original parallel hackathon branches are historical. Feature work is frozen after PR #18:

```text
main                         protected, demo/release history
  ^
  |
PR #7
  |
feature/final-integration    frozen integration + submission candidate
```

Rules from this point:

1. Do not work directly on `main`.
2. Do not add new work to the old UI/AI sprint branches.
3. Do not add optional behavior, reports, screens, redesigns, polling changes, or AI experiments.
4. Reopen code only for a reproduced crash or demo-blocking defect, on one narrow branch with proportional validation.
5. Keep PR #7 as the single final PR to `main`.
6. Run the frozen demo route twice, obtain the required review, and wait for the project owner's explicit final-merge authorization.
7. After the presentation/submission, rotate or revoke the hackathon Gemini key.

Emergency blocker setup:

```powershell
git fetch origin
git switch feature/final-integration
git pull --ff-only
git switch -c fix/<demo-blocker>
```

## Demo diagnostics

Usage monitor:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" `
  logcat -s EsikUsageMonitor:D "*:S"
```

AI source/fallback diagnostics:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" `
  logcat -s EsikAi:D "*:S"
```

Diagnostics intentionally log task/model/source/outcome/latency rather than raw biography, intervention text, crisis text, or API keys.

## Documentation

- `docs/PRODUCT_SPEC.md` — product boundary and core flow
- `docs/PROMPT_DESIGN.md` — prompt design and structured-output rationale
- `docs/AI_EVALUATION.md` — AI quality rubric and scenario suite
- `docs/AI_DEVICE_QA.md` — live/offline device test procedure
- `docs/DATA_SCHEMA.md` — local persistence contract
- `docs/VALIDATION.md` — Android/core validation record
- `docs/FINALIZATION.md` — current baseline status, workflow, and remaining non-feature gates
- `docs/HACKATHON_REPORT.md` — submission-ready project report
- `docs/DEMO_SCRIPT.md` — frozen five-minute demo and recording checklist

The older team/sprint/handoff files are retained as implementation history, not as the active workflow.
