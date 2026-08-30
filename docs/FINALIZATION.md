# Eşik Baseline Finalization

This document is the source of truth for the frozen hackathon submission candidate.

Baseline branch: `feature/final-integration`

Feature freeze was declared on 2026-08-30 after PR #18. No new behavior inference, reports, screens, redesigns, polling experiments, or AI experiments are allowed. Only a crash or a demonstrated demo-blocking defect may reopen code work.

Final Android version: `versionName 0.1.0`, `versionCode 1`.

## Product status

| Area | Status | Notes |
|---|---|---|
| Onboarding | PASS | Narrative-first text/voice input; generated profile is grounded against supplied evidence. |
| Target app selection | PASS | Launchable apps are listed; Eşik itself is excluded. |
| User-defined threshold | PASS | Threshold is always entered/changed by the user. AI does not choose it. |
| Usage Access | PASS | Guided system-settings flow validated. |
| Usage measurement | PASS | `UsageStatsManager` values validated against the selected target app. |
| Foreground monitoring | PASS | Foreground service polls every 5 seconds; the post-merge phone pass measured the target-app overlay at 6.0 seconds. |
| Monitoring notification | PASS | Android 13+ notification permission remains optional; the explicit Eşik foreground notification was visible on the final phone build. |
| Threshold trigger | PASS | Real target-app trigger validated on physical Android hardware. |
| Cooldown | PASS | 15-minute cooldown works; changing the limit resets it for testing. |
| System overlay | PASS | `TYPE_APPLICATION_OVERLAY` appears above the target app and is usable on the physical phone. |
| Quick-state intervention | PASS | Personalized local quick states appear without an AI call. |
| Custom text | PASS | Custom context reaches the card pipeline and can override a generic state. |
| Voice | PASS | Physical-device speech recognition returns text to the intervention flow. |
| Live intervention AI | PASS | Profile-grounded/state-aware cards validated. |
| AI semantic validation | PASS | Structured need/strategy/duration/anchor contract plus display-safety validation. |
| Repair/fallback | PASS | One repair attempt is bounded; deterministic local fallback remains usable offline. |
| Intentional continuation | PASS | User can intentionally continue; no forced blocking. |
| Stop/try alternative action | PASS | User can dismiss the target-app moment and return toward launcher/home. |
| In-app navigation | PASS | Android system Back from the report returned Home on the final phone build. |
| Local records | PASS | Intervention records persist locally in app-private JSON storage. |
| Daily report threshold | PASS | Live synthesis is withheld below seven records. |
| Daily report counts | PASS | Numeric facts are computed locally. |
| Daily report AI | PASS | Evidence-constrained live report validated after gateway fixes. |
| Daily report loading UX | IMPLEMENTED | Home now shows/disables the report action while the reflection is being generated instead of appearing frozen. |
| Data deletion | PASS | The final phone build displayed the destructive confirmation; the test cancelled safely without deleting data. |
| Backup privacy | PASS | Profile/records and monitoring preferences are excluded from cloud backup and device transfer. |
| App identity | PASS | The final phone build displayed the explicit Eşik launcher and foreground-notification identities. |
| CI | PASS / simplified | CI runs for PRs into `feature/final-integration` or `main`, plus pushes to `main`, avoiding duplicate feature-push + PR runs. |

## Combined QA gate — 2026-08-30

### Emulator

Android 16 Google emulator (`sdk_gphone64_x86_64`).

| Test | Result | Relevant source/result |
|---|---|---|
| Fresh onboarding | PASS | Grounded profile; no invented activities. `profile source=live`, ~2.2 s. |
| Real overlay / tired state | PASS | Low-effort suggestion; no workout mismatch. `card source=live`, ~1.1 s. |
| Procrastination text | PASS | Concrete micro-start distinct from tired flow. `card source=live`, ~1.0 s. |
| Voice + decisions | PASS with emulator limitation | Both decisions worked; recognizer failure degraded gracefully because emulator has no real microphone path. |
| Offline fallback | PASS | Instant safe fallback; no hang/crash. `source=local_fallback`, network failure. |
| Daily report | PASS after gateway fix | Evidence-grounded reflection and 2-minute micro-step; `report source=live`, ~5.8 s. |

### Report gateway issue found during QA

Two issues were reproduced directly against the API and fixed in commit `9538ed7`:

1. The report's previous 520-token budget could be consumed by model reasoning, producing `MAX_TOKENS` before the JSON answer. The report budget is now 2,048 tokens.
2. `additionalProperties` was not accepted by the tested report model's structured-output schema subset. The unsupported field was removed from the app schemas so structured output is used directly instead of unnecessarily falling back to schema-less generation.

The validated report model is now also the repository default, so a fresh local configuration matches the tested baseline unless explicitly overridden.

### Physical Android phone

The combined build was installed and smoke-tested after integration and the report gateway fixes.

Confirmed:

- Home/product UI loads correctly;
- foreground monitoring can be restarted after reinstall;
- real overlay triggers above the selected target app;
- quick-state card flow works;
- physical speech recognition returns to the intervention;
- both final decisions dismiss correctly;
- seeded daily report opens and generates successfully.

A finalization-only polish pass added launcher/notification icons, report loading state, visible/confirmed data deletion, standard system-Back routing, and Android 13+ monitoring-notification permission handling. PR #18 then reduced foreground polling from 60 seconds to 5 seconds without changing the threshold, cooldown, overlay, or AI decision policy.

### Post-merge finalization smoke — 2026-08-30

Device: Xiaomi 2311DRK48G, Android 15.

Confirmed on the current `feature/final-integration` build:

- latest debug APK installed and monitoring resumed after reopening Eşik;
- launcher identity and the persistent `Eşik aktif` notification were visible;
- Android Back returned from the report to Home;
- delete-data confirmation appeared and was cancelled without deleting data;
- with 178 minutes of Instagram use and the user-defined 30-minute threshold, the real overlay appeared after 6.0 seconds;
- reopening Instagram was suppressed by the existing cooldown; the monitor reported 866 seconds remaining;
- with four current-day records, the report correctly used the local insufficient-data route and did not call Gemini.

The live seven-record report was validated during the combined gate above. The post-polish loading label was not re-exercised in the last phone pass because the device had only four current-day records; the product owner ended further in-app testing and accepted the build. No app behavior changed after this post-merge smoke pass. The exact device result is also recorded on PR #18.

## Absolute feature freeze

- PR #16 is closed and was not integrated.
- PR #17 was superseded after a GitHub draft-transition failure.
- PR #18 contains the identical validated five-second polling fix and is merged into `feature/final-integration`.
- PR #7 is the only open product PR and remains the final candidate to `main`.
- Do not add behavior inference, reports, screens, redesigns, polling changes, prompt/model changes, or experiments.
- A new branch is permitted only for a reproduced crash or demo-blocking defect, with proportional validation.

## AI configuration used for the validated demo build

```properties
GEMINI_PROFILE_MODEL=gemini-2.5-flash-lite
GEMINI_CARD_MODEL=gemini-2.5-flash-lite
GEMINI_REPORT_MODEL=gemini-3.6-flash
```

The API key remains only in ignored `local.properties` and is never committed. The direct client is explicitly hackathon-only.

## Privacy and safety baseline

The current baseline intentionally includes these controls:

- no account or remote app database;
- profile and records stored in app-private local storage;
- relevant context may be sent to Gemini only for live AI generation, as disclosed in onboarding;
- sensitive persisted state excluded from Android backup/device transfer;
- visible in-app local-data deletion with confirmation;
- foreground tracking has explicit app/notification identity and optional notification permission;
- no API key in source control;
- crisis-signalling text bypasses Gemini;
- AI never sets or recommends the usage threshold;
- local policy constrains need, strategy, duration, and personalization anchors;
- generated language is checked for unsafe/judgmental content;
- report numeric facts are computed locally;
- debug AI diagnostics omit raw biography/custom/crisis text.

## Baseline repository workflow

The earlier role branches and parallel UI/AI branches are finished implementation history. PR #7 is the single final integration PR and targets protected `main` directly.

```text
main
  ^
  | PR #7 (single final submission PR; explicit owner authorization required to merge)
  |
feature/final-integration (frozen)
```

### Rules

1. `main` stays protected and receives no direct development commits.
2. `feature/final-integration` is the frozen known-good candidate.
3. Do not begin optional feature work. Reopen code only for a reproduced crash or demo-blocking defect.
4. Any blocker fix must use one narrow branch, pass `test` and `assembleDebug`, and retest the affected path.
5. PR #7 remains the single final PR to `main`.
6. Mark PR #7 ready after finalization documents are integrated and obtain the required teammate approval.
7. Do not merge PR #7 without the project owner's explicit final-merge request.
8. After the demo/submission, rotate or revoke the hackathon API key.

Current instructions are synchronized across `AGENTS.md`, `COPILOT_PROMPT.md`, `.github/copilot-instructions.md`, `.github/prompts/esik-build.prompt.md`, and `.github/instructions/android.instructions.md`. `docs/README.md` separates current source-of-truth documents from historical sprint/handoff material.

## Non-feature release gates

Completed finalization gates:

- GitHub Actions green on PR #18 and the updated PR #7 candidate;
- post-polish install/physical-device smoke pass completed;
- final version confirmed as `0.1.0` / `1`;
- `local.properties` is ignored and untracked, and no Google API-key literal or committed `local.properties` exists in Git history;
- final ethics regression passed through the automated safety/grounding suite plus the previously validated offline device route;
- the 1–2 page report and frozen five-minute demo runbook are in `HACKATHON_REPORT.md` and `DEMO_SCRIPT.md`.

Remaining human submission gates:

- run the exact frozen demo route twice consecutively;
- capture the primary and backup screen recordings;
- obtain the required teammate approval on PR #7;
- explicitly authorize and perform the final squash merge to `main`;
- submit the repository/prototype, report, and video;
- rotate or revoke the embedded hackathon key after the presentation/submission.

## Deliberately deferred edge cases

The following are useful robustness work but are not blockers for the baseline and remain deferred during feature freeze:

- OEM-specific background killing/battery restrictions;
- OEM-specific notification-channel/task-manager presentation differences;
- very long unattended service lifetime;
- unusual clock/timezone transitions;
- speech recognizer/provider-specific cancellation races;
- exhaustive migration/corrupt-file fuzzing;
- localization beyond the current Turkish product experience.

## Emergency blocker branch template

```powershell
git fetch origin
git switch feature/final-integration
git pull --ff-only
git switch -c fix/<demo-blocker>
```

After implementation:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
git push -u origin fix/<demo-blocker>
```

Use this only for a reproduced crash or demo-blocking defect. Open a narrow PR into `feature/final-integration`, let CI pass, and retest the affected path. PR #7 will automatically accumulate the blocker fix for the eventual final merge to `main`.
