# Eşik repository instructions for GitHub Copilot

Eşik is a two-day Android hackathon prototype for digital wellbeing. It monitors one user-selected app. When numeric use reaches the user’s own daily limit, it may show an intervention at most once every 15 minutes while that app is active. It asks what is happening, generates a neutral personalized card, and records whether the user continued or stopped.

## Non-negotiable product rules

- Keep exactly four product screens: onboarding, home, intervention, daily report. Debug controls stay on Home.
- Never diagnose addiction or any medical or mental-health condition.
- Never decide or describe what is “too much.” Refer only to the user’s own target and numeric usage.
- Do not use “çok,” “fazla,” “aşırı,” “too much,” or “excessive” in generated interpretations of behavior.
- Avoid shame, accusation, moralizing, causal certainty, and retrospective blame.
- Run the crisis filter locally before every remote AI request. On a crisis match, do not call AI and do not transmit the text.
- With fewer than seven records on the current local date, do not call the report model; show an insufficient-data state.
- Compute all counts and durations locally. Never delegate arithmetic to a model.
- Keep user data on-device and preserve the clear-data action.
- Never commit API keys. Mobile-direct Anthropic access is hackathon-only; production requires a proxy.

## Current architecture and ownership

- Android app: Kotlin, Jetpack Compose, min SDK 26, compile SDK 37, target SDK 36.
- `data/`: frozen contract and device-local JSON repository.
- `ui/`: exactly four product screens.
- `permissions/`, `usage/`, `monitor/`, `overlay/`: Android-core territory.
- `ai/`: interface, deterministic mock, prompts, crisis filter, language validator, and real-network seam.
- `EsikApp.kt`: integration boundary; coordinate before editing because all three roles may depend on it.
- Preserve `MockAiGateway` before and after real networking is added so UI work and demos never depend on the API.

## Execution order

1. Read `docs/DATA_SCHEMA.md` and preserve the frozen contract.
2. Keep the mock path working.
3. Prove Usage Access and overlay permissions.
4. Pass the Saturday noon checkpoint on a physical phone: selected app active + limit reached -> overlay visible.
5. Connect overlay choices to local records and cooldown behavior.
6. Add real AI behind `AiGateway`, asynchronously, with strict JSON parsing, timeouts, validation, and deterministic fallback.
7. Add report synthesis only after the intervention loop is stable.

## Engineering rules

- Inspect actual files before editing and report plan/code mismatches first.
- Implement one vertical slice at a time; validate after each meaningful slice.
- Keep the 60-second poll interval and 15-minute cooldown as named constants.
- Do not use Accessibility Service as a shortcut.
- Treat permission denial, target app missing, screen locked, midnight, overlay already visible, clock changes, service stop, reboot, and OEM battery behavior as explicit test cases.
- Prefer small readable files and narrow interfaces over architecture-heavy refactors.
- Do not add accounts, cloud databases, analytics SDKs, streaks, gamification, or unrelated features.
- Add or update pure unit tests for cooldown, crisis filtering, language validation, daily eligibility, and local-date grouping.
- After the standard wrapper is generated, run `./gradlew test` and `./gradlew assembleDebug` when an Android SDK is available.

## Language

User-facing demo copy is Turkish. Code, comments, commit messages, and technical documentation are English. Numeric reports use no adjectives. Observations are questions. Each valid daily report has one micro-step.
