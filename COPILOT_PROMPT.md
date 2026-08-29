You are implementing the **Eşik Android hackathon prototype**. Before editing, inspect the repository and verify this prompt against the actual files. If any path, API, version, or existing behavior differs, briefly report the mismatch and update your execution plan before changing code.

## Goal

Complete an end-to-end Android prototype with exactly four product screens:

1. Onboarding
2. Home
3. Intervention card
4. Daily report

A user selects one installed app and a daily minute limit. After the user explicitly starts monitoring, the app reads that package’s usage. When today’s numeric use reaches the user’s own limit while the selected app is active, Eşik may show an overlay at most once every 15 minutes. The overlay asks **“Şu an ne oluyor?”**, runs a local crisis-language gate, produces one personalized AI reflection card or deterministic fallback, and records **Yine de gir** or **Vazgeçtim**. The daily report uses only the current local date’s records, shows locally computed numbers, asks one non-judgmental observation question, and gives one micro-step for tomorrow.

The prototype is not a medical product. It must never diagnose addiction, decide what “too much” means, shame the user, or recommend the threshold.

## Known repository evidence

- Frozen contract: `docs/DATA_SCHEMA.md`
- Full handoff: `docs/IMPLEMENTATION_HANDOFF.md`
- Local JSON repository: `app/src/main/java/com/thynatos/esik/data/JsonEsikRepository.kt`
- Deterministic mock: `app/src/main/java/com/thynatos/esik/ai/MockAiGateway.kt`
- Real AI seam: `app/src/main/java/com/thynatos/esik/ai/AnthropicAiGateway.kt`
- Crisis and output gates: `app/src/main/java/com/thynatos/esik/ai/CrisisFilter.kt` and `SafetyLanguageValidator.kt`
- Four Compose screens: `app/src/main/java/com/thynatos/esik/ui/`
- Usage reader: `app/src/main/java/com/thynatos/esik/usage/UsageStatsReader.kt`
- Foreground monitor: `app/src/main/java/com/thynatos/esik/monitor/UsageMonitorService.kt`
- Overlay: `app/src/main/java/com/thynatos/esik/overlay/OverlayController.kt`
- Pure tests: `app/src/test/`
- Validation boundary and completed checks: `docs/VALIDATION.md`

Do not assume these files are correct merely because they exist. Inspect them first.

## Required product behavior

### Onboarding

Collect and store only on the device:

- Name; no account or password
- Hobbies
- Department/field
- One self-development area
- One sentence in the user’s own words explaining why they want to reduce use
- One installed target app and package name
- Daily limit in minutes
- Guided access to Usage Access and Draw Over Other Apps Settings

### Home

Show today’s usage, the user’s numeric limit, a progress bar, limit editing, report entry, permission repair, and monitoring start/stop. Hackathon debug actions may stay on Home; do not create a fifth screen.

### Intervention

Example opening copy:

> Bugün 78 dakika oldu. Hedefin 60'tı. Şu an ne oluyor?

Flow:

1. Accept free text.
2. Run the local crisis filter before any network call.
3. On a crisis match, transmit nothing and show support-oriented local copy.
4. Otherwise request exactly one brief open question and one two-to-five-minute hobby/goal-derived alternative.
5. Validate the output before display.
6. Show exactly two final choices: **Yine de gir** and **Vazgeçtim**.
7. Save exactly one local record.
8. **Yine de gir:** dismiss and keep/return the selected app in front.
9. **Vazgeçtim:** dismiss and move away from the selected app.
10. Do not show another overlay until the 15-minute cooldown expires.

### Daily report

Use only records on the current local date. Compute all counts and numbers locally. With fewer than seven eligible records, do not call the report model and show **Yeterli veri yok**. With seven or more records, display:

1. Numeric facts without adjectives
2. One observation phrased as a question, never a claim
3. Exactly one micro-step for tomorrow

## Frozen JSON contract

Preserve the field names in `docs/DATA_SCHEMA.md`, including these implementation fields:

- `gelisim_alani`
- `hedef_paket`
- `zaman_ms`

Do not rename fields during the hackathon.

## AI contract

Keep `MockAiGateway` operational at all times. The real implementation must be asynchronous, timeout-bounded, and unable to break the user flow.

### Card input

- User free text
- Local time
- Current usage minutes
- User limit minutes
- Hobbies
- User’s stated reason
- Optional department/self-development context

Use a current fast **Haiku-class** Anthropic model. Verify the current official model ID at implementation time and keep it configurable.

Card output must be strict JSON with exactly:

```json
{
  "question": "Şu anda dinlenmeye mi, dikkatini başka yere vermeye mi ihtiyacın var?",
  "alternative": "İki dakika gitarını eline alıp tek bir akor geçişi deneyebilirsin."
}
```

### Report input

- Profile
- Today’s eligible records
- Locally computed numeric summary

Use a current **Sonnet-class** Anthropic model. Verify and configure the model ID rather than scattering a hardcoded string.

Report output must be strict JSON with exactly:

```json
{
  "observation_question": "Akşam saatlerindeki girişlerin yorgunlukla bağlantılı olabilir mi?",
  "micro_step": "Yarın 22:30'da telefonu şarja odanın diğer tarafında bırak."
}
```

### Mandatory AI rules

Generated interpretation must not contain behavior-directed uses of **çok**, **fazla**, **aşırı**, **too much**, or **excessive**. It must not diagnose, shame, accuse, moralize, claim causality, narrate failure, set a threshold, or pretend certainty about motives. It may reference only supplied context, numeric facts, and the user’s own stated goal.

Any timeout, non-2xx response, provider refusal, invalid JSON, unknown key, blank field, blocked wording, or parser error must return deterministic fallback content. After the core loop works, add a lightweight second-pass AI safety review for the demo; it supplements but never replaces the mandatory local deterministic validator.

Never commit an API key. A direct mobile request is hackathon-only. Add a clear production note that a backend proxy is required.

## Implementation plan

### Slice 1 — Verify the frozen contract and mock path

- Read `docs/DATA_SCHEMA.md` and all current models.
- Keep or fix the mock so all UI flows work offline.
- Run pure tests before networking changes.

### Slice 2 — Pass the Saturday noon system checkpoint

- Test on the actual physical demo phone.
- Grant Usage Access and overlay permission.
- Set a low temporary limit for a real selected app.
- Verify selected app active + threshold reached -> overlay appears within the polling window.
- Verify no overlay below threshold or in another app.
- Focus on this slice until it passes; do not hide failure behind AI/UI work.

### Slice 3 — Verify persistence, actions, and cooldown

- Save one record per completed intervention.
- Verify continue/stop behavior.
- Verify no duplicate overlay while one is open.
- Verify the 15-minute boundary and clock rollback behavior.

### Slice 4 — Implement real card AI

- Use one small HTTP/JSON dependency set.
- Do not block the main thread.
- Add strict parsing, timeouts, validation, and fallback tests.
- Prove crisis text never reaches the network layer.

### Slice 5 — Implement real daily-report AI

- Filter to current local date.
- Skip network below seven records.
- Compute all numeric fields locally.
- Validate one question and one micro-step.

### Slice 6 — Harden and prepare the demo

- Test permission denial/revocation, target app missing, screen locked, overlay visible, service stop, midnight, network failure, and OEM battery behavior.
- Keep four-day seed data with at least seven current-day records.
- Prepare a repeatable demo script and backup video.

## Team boundaries

- **A:** `permissions/`, `usage/`, `monitor/`, `overlay/`, coordinated manifest changes
- **B:** `ui/`, coordinated screen state and persistence UX
- **C:** `ai/`, data contract, demo data, integration, report/ethics, presentation

Do not have two people edit the same file simultaneously. Coordinate `EsikApp.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, and frozen model files.

## Validation commands

Generate the standard Gradle Wrapper first if it is absent:

- Windows: `powershell -ExecutionPolicy Bypass -File scripts/bootstrap-gradle-wrapper.ps1`
- macOS/Linux: `./scripts/bootstrap-gradle-wrapper.sh`

Then run:

```bash
./gradlew test
./gradlew assembleDebug
```

Also perform the physical-device checks in `docs/IMPLEMENTATION_HANDOFF.md`. Do not claim system integration is complete based only on compilation or emulator tests.

## Execution rules

1. Inspect the actual repository before editing; do not trust paths or versions blindly.
2. Report plan/code mismatches before modifying code.
3. Preserve existing behavior unless this prompt explicitly changes it.
4. Implement one slice at a time and validate after each meaningful slice.
5. Diagnose and fix relevant test failures before moving on.
6. Keep changes minimal and idiomatic for this codebase.
7. Never delete the mock fallback.
8. Never add a fifth product screen.
9. Never commit credentials, generated local data, signing keys, or `local.properties`.
10. Record physical-device model, Android version, permissions, and observed results for the noon checkpoint.

## Final response must include

- Files changed
- Slice-by-slice implementation summary
- Validation commands run and results
- Physical-device tests performed and exact results
- Any plan/repository mismatches found
- Remaining limitations or risks
- Whether the mock fallback and offline demo still work
