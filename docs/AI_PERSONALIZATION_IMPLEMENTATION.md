# Implementation Handoff: AI-Native Personalization

## Goal

Turn Eşik from a generic screen-time interruption into an AI-native digital-wellbeing flow that: (1) converts a spoken or typed onboarding narrative into a structured, device-local personalization profile; (2) shows three fast, profile-aware state choices when an intervention appears; (3) accepts custom text or voice; (4) generates one neutral reflection and one realistic micro-alternative; and (5) uses accumulated records to create a cautious daily reflection.

## Non-goals

- Do not diagnose addiction, infer mental-health conditions, or label the user as a type of person.
- Do not make the model choose limits or decide whether usage is excessive.
- Do not add live Spotify, podcast, book, web-search, or recommendation-provider integrations during the hackathon.
- Do not require network access for the popup to open.
- Do not remove the deterministic mock/fallback path.
- Do not add a fifth product screen.
- Do not treat the mobile-direct Anthropic key pattern as production architecture; production requires a proxy.

## Current evidence from the repo

- `AiGateway` has only synchronous card/report methods; it needs profile generation, richer intervention context, and suspend-safe networking.
- `AnthropicAiGateway` currently delegates every request to `MockAiGateway`; no HTTP request or JSON parsing exists.
- `UserProfile` stores manual fields only; there is no biography, structured AI profile, or personalized quick-state set.
- `InterventionRecord` stores only free text and the final continue/stop choice.
- `OnboardingScreen` is a form with separate fields and no voice capture.
- `InterventionScreen` requires typed text before generating a card.
- `OverlayController` is the real system intervention surface and must remain instant even when the API is unavailable.
- `JsonEsikRepository` is already device-local and can be extended with backward-compatible optional JSON fields.
- Android Core has been physically validated on the demo phone and this branch starts from `work/android-core`.

## Assumptions and open questions

- Assumption: Turkish is the first demo language; stored IDs remain language-independent where practical.
- Assumption: Android speech recognition is used only to obtain text; the LLM receives text, not audio.
- Assumption: onboarding/profile and intervention calls use a fast Haiku-class model; the daily synthesis uses a Sonnet-class model. Model IDs remain BuildConfig values so they can be changed without editing business logic.
- Assumption: the user can edit the AI-derived profile before it is saved, or continue with a deterministic fallback if the API is unavailable.
- Open question: a production release must replace the embedded demo key with a backend proxy and an explicit retention/logging policy.

## Design approach

### Data model v2

Keep the existing `UserProfile` fields for Android Core compatibility and add:

- `biography`: the user's original spoken/typed description.
- `personalization`: goals, recurring contexts, preferred activities, low-energy alternatives, quick-state options, and tone preference.
- `schemaVersion`: enables backward-compatible migration from existing device JSON.

Add `QuickStateOption`, `PersonalizationProfile`, `ProfileIntake`, `InterventionInput`, and `InputMethod` models. Extend `InterventionRecord` with selected state ID/label, input method, generated reflection, and generated alternative while preserving defaults for older records.

### AI flow

1. **Profile call:** biography + existing fields -> structured personalization profile and six candidate quick states.
2. **Popup open:** choose three stored quick states locally; no API call and no loading delay.
3. **State/custom input:** run the crisis filter locally first. If safe, call the fast model for one reflection question and one micro-alternative.
4. **Fallback:** timeout, missing key, HTTP failure, refusal, parse failure, or blocked wording -> deterministic `MockAiGateway` output.
5. **Daily report:** only after seven same-day records; local code computes all numbers and the model returns only a cautious question and one micro-step.

### Voice flow

Use Android `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` through the Activity Result API. Voice produces editable text. Onboarding and in-app intervention expose Talk/Type options. The overlay uses the same recognizer through a tiny transparent bridge activity so the system popup can receive a result without owning an Activity.

### Security and privacy

- Never send crisis-signaling text to Anthropic.
- Never commit an API key.
- Read a demo key from `local.properties` into BuildConfig only for the hackathon APK.
- Keep all profile and intervention records in `esik_state.json` on the device.
- Provide fallback behavior when the key is blank.
- Document that a production build requires a backend proxy and should not embed provider credentials.

## Files likely to change

| Path | Change |
|---|---|
| `app/src/main/java/com/thynatos/esik/data/Models.kt` | Add schema-v2 profile and intervention models with defaults. |
| `app/src/main/java/com/thynatos/esik/data/JsonEsikRepository.kt` | Backward-compatible read/write for new fields. |
| `app/src/main/java/com/thynatos/esik/ai/AiGateway.kt` | Add profile generation and suspend methods. |
| `app/src/main/java/com/thynatos/esik/ai/MockAiGateway.kt` | Personalized deterministic quick states/cards/profile/report. |
| `app/src/main/java/com/thynatos/esik/ai/AiPrompts.kt` | Profile, intervention, and report prompt contracts. |
| `app/src/main/java/com/thynatos/esik/ai/AnthropicAiGateway.kt` | Timeout-bounded Messages API client and strict parser. |
| `app/src/main/java/com/thynatos/esik/ai/AnthropicMessageClient.kt` | Isolated HTTP transport and response extraction. |
| `app/src/main/java/com/thynatos/esik/voice/*` | Speech-intent builder, transparent bridge, and result coordination. |
| `app/src/main/java/com/thynatos/esik/ui/OnboardingScreen.kt` | Narrative-first voice/text onboarding and profile-generation state. |
| `app/src/main/java/com/thynatos/esik/ui/InterventionScreen.kt` | Three quick replies, custom voice/text, loading/error/fallback state. |
| `app/src/main/java/com/thynatos/esik/overlay/OverlayController.kt` | Fast quick replies and async card generation in the real overlay. |
| `app/src/main/java/com/thynatos/esik/EsikApp.kt` | Wire suspend profile/card/report flows and richer records. |
| `app/src/main/AndroidManifest.xml` | Register the transparent voice bridge activity. |
| `app/build.gradle.kts` | BuildConfig values for blank/default API configuration. |
| `local.properties.example` | Document local demo-key/model configuration. |
| `docs/DATA_SCHEMA.md` | Replace the frozen v1 contract with versioned v2 examples. |

## Implementation slices

### Slice 1: Freeze schema v2 and backward-compatible persistence

**Intent:** Give UI, AI, overlay, and reporting one stable contract before changing behavior.

**Steps:**
1. Add profile-personalization, quick-state, intervention-input, and enriched-record models with safe defaults.
2. Extend JSON serialization while continuing to read existing v1 files.
3. Update schema documentation and model/repository unit tests.

**Validation:** `./gradlew test`

**Acceptance criteria:** Existing v1 profile/records still load; v2 data round-trips without losing quick states or generated-card metadata.

### Slice 2: Expand the AI gateway with deterministic behavior first

**Intent:** Let every screen and overlay build against a working API before real networking.

**Steps:**
1. Convert AI operations to `suspend` and add `generateProfile`.
2. Make `MockAiGateway` derive a useful profile and quick states from the narrative and existing fields.
3. Add explicit input/output prompt contracts and strict parser tests.

**Validation:** unit tests for profile, card, report, and fallback outputs.

**Acceptance criteria:** The complete flow works offline with deterministic, safe Turkish output.

### Slice 3: Narrative-first onboarding with voice

**Intent:** Make AI personalization visible during setup without adding a fifth screen.

**Steps:**
1. Replace the many profile text fields with a prominent biography prompt plus Talk/Type controls; retain name, target app, limit, and permissions.
2. Generate a profile asynchronously and show an editable summary before final save.
3. Fall back locally if recognition or AI is unavailable.

**Validation:** Compose/manual tests for typing, speech result, cancellation, no recognizer app, loading, fallback, and save.

**Acceptance criteria:** A user can speak or type a narrative and complete onboarding with a structured profile saved locally.

### Slice 4: AI-first intervention interaction

**Intent:** Make the popup useful within seconds rather than forcing a paragraph.

**Steps:**
1. Show three stored quick-state choices immediately.
2. Offer custom voice/text as a fourth route.
3. Run the local crisis filter before any model call.
4. Generate one short reflection plus one profile-grounded alternative, then record both the input and output.

**Validation:** quick choice, custom text, voice, crisis, API failure, unsafe output, continue, and stop scenarios.

**Acceptance criteria:** The user can reach a useful personalized card in one tap, with no network dependency for the popup's first state.

### Slice 5: Real Anthropic transport with hard fallback

**Intent:** Enable the AI demo without allowing network fragility to break it.

**Steps:**
1. Add an isolated Messages API client using connect/read timeouts and a blank-key short circuit.
2. Configure fast/report model IDs through BuildConfig.
3. Parse JSON strictly, validate lengths/enums/language, handle refusal and HTTP errors, and fall back on any failure.
4. Keep the direct mobile request clearly marked demo-only.

**Validation:** parser/transport unit tests with representative success/error bodies; manual live-key test.

**Acceptance criteria:** A valid key returns real personalized output; no key or any failure remains fully usable through the mock path.

### Slice 6: Daily memory and cautious synthesis

**Intent:** Demonstrate adaptation over repeated interventions.

**Steps:**
1. Include selected states, input methods, generated alternatives, and final choices in daily context.
2. Keep all numeric counts local.
3. Return one tentative observation question and one micro-experiment only after seven records.

**Validation:** below-seven, exactly-seven, mixed states/choices, and blocked-language tests.

**Acceptance criteria:** The report can reference repeated contexts without diagnosis or unsupported causal claims.

### Slice 7: End-to-end demo hardening

**Intent:** Make the stage path deterministic.

**Steps:**
1. Seed a narrative, structured profile, quick states, and four days of enriched records.
2. Verify cold start, offline mode, live mode, speech cancellation, overlay lifecycle, and data deletion.
3. Update README/demo instructions and capture a backup video.

**Validation:** `./gradlew test`, `./gradlew assembleDebug`, physical-device script.

**Acceptance criteria:** The same demo succeeds with live AI and with airplane mode.

## Tests and verification

- Unit tests: model defaults, v1/v2 JSON migration, prompt payloads, JSON parsing, crisis short-circuit, safety fallback, minimum record rule.
- Integration tests: repository round-trip and gateway fallback chain.
- Manual QA: spoken onboarding -> AI profile -> low limit -> overlay quick choice -> personalized card -> continue/stop -> daily report.
- Commands to run: `./gradlew test`, `./gradlew assembleDebug`, `./gradlew installDebug`.

## Edge cases and failure modes

- Missing/blank API key: use mock immediately and show no blocking error.
- Speech recognizer unavailable/cancelled: keep editable text input.
- Slow network: time out quickly; do not hold the popup indefinitely.
- Malformed or extra model fields: reject and fall back.
- Crisis signal: never call the model; show the support route.
- Unsafe/judgmental output: discard and use the deterministic safe card.
- Empty AI profile: generate local default quick states from reason/hobbies.
- Existing v1 state file: migrate by defaults without deleting user data.
- Overlay destroyed during request: ignore late callbacks and avoid duplicate windows.
- Intentional relaxation: allow a neutral recommendation or an intentional timed continuation rather than always pushing the user away.

## Rollback plan

Every slice keeps `MockAiGateway` functional. If a later slice fails, disable the live client through a blank key/model setting and revert only that slice; schema-v2 fields have defaults, so older behavior remains usable.

## Executor prompt for fresh session

You are implementing AI-native personalization for Eşik. Before editing, inspect the repository and verify this plan against the actual code. If the plan is wrong or stale, briefly update it and explain the mismatch before editing.

Goal:
Create narrative/voice onboarding that produces a structured device-local profile, instant personalized quick states in the intervention popup, profile-grounded AI micro-alternatives, and cautious daily memory. The app must remain fully usable offline through deterministic fallback behavior.

Known repo evidence:
- Android Core is validated and this work starts from `work/android-core`.
- The app has exactly four Compose screens plus a real `TYPE_APPLICATION_OVERLAY` intervention.
- `AiGateway` currently exposes synchronous card/report methods only.
- `AnthropicAiGateway` is currently a fallback stub.
- Persistence is local JSON and must remain backward compatible.

Plan:
1. Freeze schema v2 and migration.
2. Expand the gateway and mock first.
3. Add voice/narrative onboarding.
4. Add quick-state intervention and enriched records.
5. Add timeout-bounded Anthropic transport with strict parsing/fallback.
6. Extend daily synthesis and demo data.
7. Run unit/build/device verification after each meaningful slice.

Validation commands:
- `./gradlew test`
- `./gradlew assembleDebug`
- `./gradlew installDebug`

Execution rules:
1. Do not assume file paths are correct; verify them first.
2. Preserve Android Core behavior.
3. Keep exactly four product screens.
4. Implement one slice at a time.
5. Run the most relevant validation after each meaningful slice.
6. Never commit an API key.
7. Never send crisis-signaling text to the model.
8. Keep changes minimal and idiomatic for this codebase.

Final response must include:
- Files changed
- Summary of implementation
- Validation commands run and results
- Live-AI and offline-fallback results
- Any unresolved device or production-security issues
