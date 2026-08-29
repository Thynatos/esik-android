# Eşik — Android Hackathon Starter

**Eşik** is an Android digital-wellbeing prototype that intervenes after a user exceeds a limit they set for one app. It asks “What is happening right now?”, generates a neutral personalized reflection card, and records whether the user continued or stopped.

This repository is structured for a two-day, three-person hackathon. User-facing demo copy is Turkish; code and technical documentation are English.

## Product boundary

Eşik does not diagnose addiction, decide what “too much” means, or shame the user. It only compares numeric use with the user’s own target.

Exactly four product screens are in scope:

1. Onboarding
2. Home
3. Intervention card
4. Daily report

Debug controls may live on the Home screen; do not create a fifth screen.

## What is already scaffolded

- Four Jetpack Compose screens and an in-app debug flow
- Installed launchable-app picker
- Device-local JSON persistence in `filesDir/esik_state.json`
- Deterministic mock card and report generation
- Local crisis-language gate that runs before AI
- Banned-language output validator with fallback
- 15-minute cooldown policy
- `UsageStatsManager` permission checks and daily usage reads
- User-started foreground service polling every 60 seconds
- Basic `TYPE_APPLICATION_OVERLAY` intervention window
- Four-day demo data with enough records today to show a report
- Unit tests for cooldown, crisis filtering, output language, report eligibility, and date grouping
- Copilot context in `.github/copilot-instructions.md`, `AGENTS.md`, `COPILOT_PROMPT.md`, and the reusable `/esik-build` prompt file

## What the team still needs to finish

- Validate Usage Access, foreground-app detection, and overlays on the actual demo phone
- Improve overlay lifecycle, keyboard, back-button, and OEM/battery behavior
- Replace the placeholder Anthropic gateway with a timeout-bounded HTTP client and strict JSON parsing
- Keep the mock/fallback path working even after real AI is connected
- Add asynchronous state/loading/error handling around real network calls
- Verify local-midnight behavior and daily report record selection
- Polish Turkish copy and prepare the final report, ethics section, demo rehearsal, and backup video

## First setup

The source tree pins Android Gradle Plugin 9.3.0, Kotlin/Compose Compiler 2.3.21, Compose BOM 2026.08.00, `compileSdk 37`, `targetSdk 36`, and Java 17.

A standard Gradle Wrapper binary is intentionally generated on the developer machine rather than reconstructed from an unverified binary. Run one bootstrap script from the repository root:

### Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap-gradle-wrapper.ps1
```

### macOS or Linux

```bash
./scripts/bootstrap-gradle-wrapper.sh
```

The script downloads Gradle 9.5.0, verifies its published distribution checksum, generates the standard wrapper in an isolated minimal build, verifies the published Wrapper JAR checksum, and pins the distribution checksum.

Then:

1. Install Android Studio, Android SDK Platform 37, and JDK 17.
2. Copy `local.properties.example` to `local.properties` and set `sdk.dir` if Android Studio does not create it.
3. Open the repository in Android Studio and sync Gradle.
4. Run on a physical Android phone.
5. Grant **Usage Access** and **Draw over other apps** from the app’s guided Settings buttons.
6. Use **4 günlük demo verisi yükle** and **Kart ekranını test et** before the system trigger is ready.
7. Give Copilot Agent Mode the contents of `COPILOT_PROMPT.md`, or invoke `/esik-build` in an IDE that supports repository prompt files.

## Build and test

After generating the wrapper:

```bash
./gradlew test
./gradlew assembleDebug
```

The pure unit tests can run on a development machine. `./scripts/verify-pure-kotlin.sh` also provides a lightweight check when `kotlinc` is available. A physical phone is required to validate UsageStats accuracy, target-app detection, foreground-service behavior, overlays, lock-screen behavior, and OEM battery restrictions. See `docs/VALIDATION.md` for the exact checks already performed and the remaining validation boundary.

## Create the GitHub repository

After GitHub CLI is authenticated, run one script from the repository root. It initializes Git and creates the first commit when needed; Git must have `user.name` and `user.email` configured.

### Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/create-github-repo.ps1
```

### macOS or Linux

```bash
./scripts/create-github-repo.sh
```

Both scripts create and push the private repository `Thynatos/esik-android`.

## Secret handling

Never commit an Anthropic API key. A mobile-direct Anthropic request may be used only for the hackathon demo. A production version needs a backend proxy, short-lived or server-held credentials, abuse controls, and an explicit logging/redaction policy.

## Team ownership

- **A — Android core:** permissions, UsageStats, foreground service, threshold trigger, cooldown, overlay
- **B — UI:** four Compose screens and local-persistence UX
- **C — AI + integration:** frozen data contract, mock gateway first, prompts, HTTP/JSON, safety gates, report, presentation

See `docs/TEAM_PLAN.md`, `docs/IMPLEMENTATION_HANDOFF.md`, and `docs/VALIDATION.md`.
