# Validation Record

This file records what has actually been validated for the current Eşik hackathon candidate. For the current baseline checklist and release workflow, also see `docs/FINALIZATION.md`.

## Automated validation

GitHub Actions runs on feature pushes and executes:

```bash
./gradlew test --stacktrace
./gradlew assembleDebug --stacktrace
```

The combined candidate passed both tasks before the finalization documentation/icon pass. The workflow is required to stay green after every subsequent feature or fix.

Current unit coverage includes the most important deterministic logic around:

- crisis-language detection;
- ordinary-text non-match;
- unsafe/generated-language checks;
- cooldown boundaries and remaining-time behavior;
- demo-data seeding;
- report eligibility below/at seven records;
- local context/strategy compilation;
- card semantic validation;
- profile grounding;
- report evidence aggregation and semantic validation;
- deterministic fallback behavior.

## Android core physical validation

Validated on a real Android phone:

- Usage Access permission flow;
- Draw over other apps permission flow;
- selected-app usage minutes;
- foreground monitoring service;
- target-app foreground detection;
- user-defined threshold trigger;
- real intervention overlay above the selected target app;
- 15-minute cooldown behavior;
- cooldown reset after changing the configured limit;
- lock/unlock handling from the core validation pass;
- monitoring persistence/restart behavior;
- both final intervention decisions.

The highest-risk Android requirement is therefore validated: Eşik can detect the selected app, observe that the user-defined threshold has been reached, and place the intervention over the real target app.

## Combined AI + UI emulator gate — 2026-08-30

Environment: Android 16 Google emulator (`sdk_gphone64_x86_64`).

| Scenario | Result |
|---|---|
| Fresh onboarding and grounded profile | PASS |
| Tired-state real overlay/card | PASS |
| Procrastination custom-text card | PASS |
| Both final decisions | PASS |
| Voice failure fallback in emulator | PASS; no real microphone path available |
| Airplane/offline deterministic fallback | PASS |
| Seeded daily report | PASS after report gateway fix |

Observed AI diagnostics during the combined gate included:

- profile live response around 2.2 s;
- tired card live response around 1.1 s;
- procrastination card live response around 1.0 s;
- offline card returned immediately through `local_fallback`;
- final live report around 5.8 s.

## Report gateway fix validated during gate

The daily report initially fell back locally. Direct API reproduction identified two independent causes:

1. a 520-token output limit could be exhausted by model reasoning before JSON output was emitted;
2. `additionalProperties` was rejected by the tested report model's structured-output schema subset.

The final gateway fix:

- raises the report budget to 2,048 tokens;
- removes unsupported `additionalProperties` fields from response schemas.

After reinstall/retest, the report completed live with `source=live outcome=ok` and produced an evidence-grounded question plus a two-minute micro-step.

## Final physical-phone smoke pass — 2026-08-30

After the combined integration and report fix, the final candidate was installed on the physical phone and the affected end-to-end paths were rechecked.

Confirmed:

- redesigned Home/product UI opens correctly;
- monitoring starts after reinstall/reopen;
- threshold overlay appears above the target app;
- quick-state intervention returns a card;
- physical speech recognition returns to the intervention;
- both final decisions dismiss correctly;
- seeded daily report opens and completes successfully.

## Post-merge polling/finalization smoke — 2026-08-30

Device: Xiaomi 2311DRK48G, Android 15.

- The current `feature/final-integration` APK installed successfully.
- The launcher icon and persistent `Eşik aktif` monitoring notification were visible.
- Android Back returned from the report to Home.
- The delete-data confirmation appeared and was cancelled without deleting data.
- Above the user-defined threshold, the Instagram overlay was logged 6.0 seconds after launch.
- Reopening Instagram during cooldown produced no second overlay; 866 seconds remained.
- At four current-day records, the report returned the local insufficient-data state and made no Gemini call.

The live seven-record report was validated in the combined AI + UI gate. The post-polish loading label was not re-exercised in the last phone pass because the device had only four current-day records; the product owner ended further in-app testing and accepted the build. No behavior code changed after this smoke pass.

## Final ethics regression

| Requirement | Evidence | Result |
|---|---|---|
| Intentional rest is not shamed | `InterventionContextBuilderTest.intentionalRestPreservesAutonomyWithTimedUseStrategy` plus the validated intentional-continue flow | PASS |
| Sparse profiles do not invent hobbies | `ProfileGroundingSanitizerTest` and `MockAiGatewayTest.sparseProfileFallbackDoesNotInventActivityPreferences` | PASS |
| Crisis language stays on the local route | Turkish/English crisis tests plus the ordinary-fatigue non-match test | PASS |
| Airplane/provider failure remains usable | Combined emulator gate returned immediate `local_fallback` without a crash | PASS |
| Repeated states avoid absurd repetition | Local fallback rotation and near-duplicate rejection tests | PASS |
| AI never defines a threshold or calls the user addicted | Safety-language tests, prompt constraints, and local trigger ownership | PASS |

The final local run executed 84 unit tests with zero failures, errors, or skipped tests.

## Privacy/storage validation from source

The current candidate stores profile/intervention state in app-private `esik_state.json`. Android backup rules explicitly exclude:

- `esik_state.json`;
- `esik_monitor.xml`.

The same data is excluded from device-transfer extraction rules. The in-app delete action stops monitoring, clears cooldown state, and deletes the repository file.

## Demo model configuration

Validated combined demo configuration:

```properties
GEMINI_PROFILE_MODEL=gemini-2.5-flash-lite
GEMINI_CARD_MODEL=gemini-2.5-flash-lite
GEMINI_REPORT_MODEL=gemini-3.6-flash
```

The API key itself is never committed.

## Final verification commands

Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

Monitor diagnostics:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" `
  logcat -s EsikUsageMonitor:D "*:S"
```

AI diagnostics:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" `
  logcat -s EsikAi:D "*:S"
```

## Deferred robustness checks

These are useful edge-case work, not baseline blockers:

- OEM-specific battery/background-killing behavior;
- notification-permission differences across devices;
- very long unattended service lifetime;
- recognizer-provider cancellation races;
- unusual timezone/clock transitions;
- corrupted/migrated local-file fuzzing.

Do not confuse these deferred robustness checks with unvalidated core functionality; the demo-critical end-to-end path has been validated on physical hardware.
