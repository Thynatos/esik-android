# Implementation Handoff: Eşik AI Quality v2

## Goal

Upgrade Eşik’s Gemini layer from a basic prompt-and-parse implementation into a product-specific behavior engine that consistently produces concise, context-sensitive, realistic, and ethically cautious outputs for three moments: onboarding profile creation, threshold interventions, and daily reflection. Improve quality without making the popup slower, removing offline fallback, changing the four-screen product, or coupling the AI layer to the UI redesign.

## Non-goals

- Do not change the visible UI, Compose hierarchy, theme, overlay styling, usage monitoring, cooldown, or Android permissions.
- Do not build a general-purpose chatbot, therapist, diagnosis system, web-search recommender, Spotify/podcast integration, or multi-turn cloud memory.
- Do not let Gemini choose the user’s limit or declare usage excessive.
- Do not remove `MockAiGateway`, crisis short-circuiting, local counts, output validation, or offline behavior.
- Do not commit an API key or treat direct mobile credentials as production architecture.
- Avoid changing `AiGateway` or shared data models unless a quality gain clearly justifies the integration cost; prefer internal AI-layer contracts first.

## Current evidence from the repo

- `app/src/main/java/com/thynatos/esik/ai/AiPrompts.kt`: the three system prompts are clear but short and generic; they contain no contrastive examples, state-specific strategy policy, or explicit semantic output contract beyond the visible fields.
- `app/src/main/java/com/thynatos/esik/ai/GeminiMessageClient.kt`: every task uses one fixed temperature (`0.2`), max-token limit, and JSON MIME type; task-specific generation settings and an explicit response schema are not currently used.
- `app/src/main/java/com/thynatos/esik/ai/GeminiAiGateway.kt`: intervention payload includes time, app, usage/limit, reason, goals, activities, selected state, user text, and input method, but it does not compile these into an explicit intervention strategy such as low-energy reset, micro-start, or intentional timed continuation.
- `GeminiAiGateway.parseCard`: validates non-empty strings and display safety, but not strategy fit, question form, realistic duration, invented content, repetition, or semantic alignment with the selected state.
- `GeminiAiGateway.generateDailyReport`: sends raw recent records and local counts, but does not provide a local state/choice cross-tab or evidence summary that makes cautious pattern synthesis easier.
- `app/src/main/java/com/thynatos/esik/ai/MockAiGateway.kt`: already contains useful deterministic state-specific behavior and can serve as the baseline specification for live AI quality.
- `app/src/test/java/com/thynatos/esik/ai/MockAiGatewayTest.kt`: covers basic mock behavior and the seven-record threshold, but there is no scenario corpus for live-prompt contracts, context compilation, parsing, repair, or semantic validation.
- `docs/PROMPT_DESIGN.md`: documents the current prompt version and should be updated rather than replaced when v2 is finalized.

## Assumptions and open questions

- Assumption: Turkish remains the output language and English/Turkish user input may both occur.
- Assumption: the current `generateContent` REST path remains acceptable for these single-shot tasks during the hackathon; API migration is not required to improve product quality.
- Assumption: the card UI continues to consume only `question` and `alternative`, even if the internal Gemini output contract contains additional fields used for validation.
- Assumption: a small number of short contrastive examples is acceptable and provides a legitimate few-shot element for the report.
- Assumption: model IDs remain configurable through `local.properties`; no model upgrade is accepted without a device A/B test for quality, latency, and fallback behavior.
- Open question: whether the final default card model should remain the current Flash-Lite model or move to a stronger Flash model must be decided from measured device results, not preference.

## Design approach

### 1. Treat the model as a constrained decision assistant, not a free-form coach

Before calling Gemini, local code should compile the current context into a small explicit policy:

- current need/state: tired, procrastinating, intentional rest, bored, waiting, habit, or other;
- energy expectation: low, normal, or unknown;
- interaction objective: pause, clarify intention, micro-start a goal, make rest intentional, or offer an environment change;
- allowed strategy set;
- maximum action duration;
- allowed personalization anchors taken only from the user profile;
- disallowed recommendation patterns for this state.

This local context should guide Gemini instead of asking it to infer the entire product policy from prose every time.

### 2. Use a richer internal card contract while preserving the UI contract

Request structured output similar to:

```json
{
  "need": "rest",
  "strategy": "low_energy_reset",
  "question": "Şu an gerçekten dinlenmeye mi, yoksa otomatik kaydırmaya mı ihtiyacın var?",
  "alternative": "Bir şarkı boyunca telefonu bırakıp yalnızca müzik dinlemeyi dene.",
  "duration_minutes": 4,
  "personalization_anchor": "müzik"
}
```

Suggested enums:

- `need`: `rest`, `activation`, `intentional_break`, `boredom`, `waiting`, `habit`, `other`
- `strategy`: `low_energy_reset`, `micro_start`, `timed_intentional_use`, `environment_change`, `sensory_break`, `brief_activity`, `other`

Only `question` and `alternative` need to reach the existing UI. The remaining fields make the output easier to validate.

### 3. Add short contrastive few-shot examples

Use a compact set of examples that encode the key product distinctions:

- tired + exercise goal -> do not prescribe a workout; offer low-energy recovery;
- procrastinating + study goal -> offer a two-to-five-minute first step;
- intentional relaxation -> do not shame or always stop the user; offer intentional timed continuation;
- unsupported content request -> do not invent a new episode/book/product.

Keep examples short enough to protect latency.

### 4. Add semantic validation and one repair attempt

After parsing, validate:

- question is short, open, uncertain, and ends as a question;
- alternative is a concrete action, not generic advice;
- duration is within the locally allowed range;
- strategy is compatible with the selected state;
- any personalization anchor exists in the supplied profile/context;
- no diagnosis, judgment, causal certainty, invented content, or model-defined threshold appears;
- output is not effectively identical to a recent fallback/template when avoidable.

On parse/semantic failure, make at most one short repair request containing the invalid JSON and specific validation errors. If repair fails or would exceed the latency budget, use `MockAiGateway`.

### 5. Make daily reflection evidence-driven

Compute local aggregates before the report call:

- count by `stateId`;
- continue/stop count by state;
- most frequent state;
- state with the highest continue ratio, only when sample size is adequate;
- broad time-of-day bucket counts;
- accepted/rejected alternatives where available.

Send these aggregates with the raw records. Ask Gemini to choose one evidence-backed observation and one realistic micro-experiment. Validate that the selected evidence exists and retain the existing seven-record minimum.

### 6. Separate task configuration and make model choice measurable

Allow task-specific model/settings configuration, for example:

- profile model;
- card model;
- report model;
- per-task timeout/max tokens;
- optional structured-schema mode.

Keep current working defaults until a manual A/B matrix shows a better choice. Record perceived quality, latency, failure, and fallback rate for several scenarios.

### 7. Add privacy-safe diagnostics

In debug builds, log only:

- task type;
- model;
- elapsed time;
- live/fallback source;
- high-level failure reason;
- validation/repair outcome.

Do not log raw biography, crisis text, free-form user input, or API keys.

## Files likely to change

| Path | Change |
|---|---|
| `app/src/main/java/com/thynatos/esik/ai/AiPrompts.kt` | Introduce v2 policy prompts and compact contrastive examples. |
| `app/src/main/java/com/thynatos/esik/ai/InterventionContextBuilder.kt` | New local compiler for state, energy, objective, allowed strategies, duration, and anchors. |
| `app/src/main/java/com/thynatos/esik/ai/AiOutputContracts.kt` | New internal enums/data contracts for profile/card/report output parsing and validation. |
| `app/src/main/java/com/thynatos/esik/ai/GeminiMessageClient.kt` | Support task-specific generation config and optional explicit schema payload while preserving current transport/fallback behavior. |
| `app/src/main/java/com/thynatos/esik/ai/GeminiAiGateway.kt` | Use compiled context, v2 contracts, semantic validation, one repair attempt, report aggregates, and debug diagnostics. |
| `app/src/main/java/com/thynatos/esik/ai/SafetyLanguageValidator.kt` | Add semantic/product constraints where appropriate without duplicating the crisis filter. |
| `app/src/main/java/com/thynatos/esik/ai/MockAiGateway.kt` | Keep deterministic behavior aligned with the v2 policy and use it as the fallback quality floor. |
| `app/build.gradle.kts` | Add separate profile/card/report model and optional tuning configuration only if required. |
| `local.properties.example` | Document task-specific model overrides without real credentials. |
| `app/src/test/java/com/thynatos/esik/ai/*` | Add context, contract, parser, validator, repair, report-aggregate, and scenario tests. |
| `docs/PROMPT_DESIGN.md` | Update with v2 messages, few-shot rationale, policy compiler, and structured validation. |
| `docs/AI_EVALUATION.md` | Add scenario corpus, rubric, A/B table, and device results. |

## Implementation slices

### Slice 1: Define the AI quality rubric and golden scenarios

**Intent:** Improve measurable behavior instead of endlessly rewriting prompts by intuition.

**Steps:**
1. Create `docs/AI_EVALUATION.md` with a concise quality rubric: groundedness, state fit, actionability, autonomy, tone, safety, concision, and latency.
2. Define at least 12 representative scenarios covering tiredness, procrastination, intentional rest, boredom, habit, waiting, custom English/Turkish text, sparse profile, crisis short-circuit, offline fallback, and daily patterns.
3. Record the expected strategy and unacceptable outputs for each scenario.
4. Add unit-test fixtures for the pure context/validation pieces before modifying transport.

**Validation:** `./gradlew test`.

**Acceptance criteria:** Every later prompt change can be evaluated against explicit scenarios and expected strategy behavior.

### Slice 2: Compile a local intervention policy/context

**Intent:** Make the API call product-specific before adding more prompt text.

**Steps:**
1. Add `InterventionContextBuilder` and internal enums.
2. Map known quick-state IDs and custom text cues to need, energy, objective, allowed strategies, duration, and forbidden patterns.
3. Select only safe user-provided anchors from profile goals/activities/low-energy activities.
4. Keep ambiguous cases cautious and avoid labeling the person.

**Validation:** Unit tests for every known state plus ambiguous/custom input.

**Acceptance criteria:** The same user profile produces different explicit strategy constraints for tired, procrastinating, and intentional-rest contexts.

### Slice 3: Implement prompt v2 and internal structured card output

**Intent:** Encode the behavior policy and obtain machine-checkable responses.

**Steps:**
1. Rewrite the card prompt around role, evidence, policy, allowed strategies, forbidden behaviors, concise output, and autonomy.
2. Add three or four compact contrastive examples.
3. Request the richer internal card contract with enums and duration.
4. Keep the existing visible `AiCard(question, alternative)` interface unchanged.
5. Add explicit schema support only behind a tested implementation path; preserve current JSON MIME/parsing as a fallback if the provider rejects schema configuration.

**Validation:** Parser/contract tests and a live device call with current model configuration.

**Acceptance criteria:** Scenario outputs are structurally parseable and strategy-aligned while the UI integration remains untouched.

### Slice 4: Semantic validation, repair, and diagnostics

**Intent:** Prevent superficially valid but weak or inappropriate JSON from reaching the user.

**Steps:**
1. Add semantic validators for question form, actionability, duration, state/strategy compatibility, allowed anchors, invented content, and safety language.
2. Add one bounded repair attempt for invalid live output.
3. Fall back immediately after failed repair, timeout, blocked generation, or crisis signal.
4. Add privacy-safe debug diagnostics for source, latency, and failure category.

**Validation:** Unit tests for valid, malformed, semantically wrong, unsafe, invented-content, and repair-failure cases.

**Acceptance criteria:** Invalid live output cannot bypass validation; fallback remains fast and deterministic.

### Slice 5: Upgrade profile generation

**Intent:** Make onboarding produce a more useful foundation for later interventions without changing the stored schema.

**Steps:**
1. Clarify the distinction between goals, recurring situations, preferred activities, low-energy activities, and quick states.
2. Add grounding rules and short positive/negative examples.
3. Validate that activities are user-grounded, contexts are situations rather than traits, and quick states are concise first-person options.
4. Merge missing/invalid fields with deterministic fallback defaults as today.

**Validation:** Scenario tests for detailed, sparse, Turkish, English, and mixed-language narratives.

**Acceptance criteria:** Generated profiles are more specific and less generic without inventing hobbies, goals, or diagnoses.

### Slice 6: Upgrade daily report evidence and synthesis

**Intent:** Make the report more insightful while preventing unsupported claims.

**Steps:**
1. Compute local state/choice/time aggregates.
2. Send aggregates plus bounded raw records to Gemini.
3. Ask for one evidence-backed tentative question and one micro-experiment.
4. Validate that any referenced state/pattern exists in the aggregate and that numbers remain locally computed.
5. Preserve the seven-record minimum and fallback report.

**Validation:** Tests for below-seven, exactly-seven, dominant state, mixed state, insufficient subgroup evidence, and unsafe/causal language.

**Acceptance criteria:** The report uses real recorded patterns, avoids causal certainty, and provides one specific next-day experiment.

### Slice 7: Model/settings A/B and prompt freeze

**Intent:** Select settings from evidence and stop changing prompts before the demo.

**Steps:**
1. Make profile/card/report model IDs separately configurable if they are not already.
2. Test the current defaults and at most two alternative configurations on the physical phone.
3. Record median perceived latency, output quality, fallback/failure, and scenario score.
4. Choose the simplest stable configuration and freeze prompt/model versions.
5. Update `docs/PROMPT_DESIGN.md` and `docs/AI_EVALUATION.md` with the final rationale.

**Validation:** `./gradlew test`, `./gradlew assembleDebug`, `./gradlew installDebug`, manual scenario matrix.

**Acceptance criteria:** The final configuration has documented evidence, the exact demo route works twice, and no further prompt changes are planned.

## Tests and verification

- Unit tests: context mapping, anchor selection, output-contract parsing, semantic validation, repair decisions, report aggregates, profile grounding, crisis short-circuit, and fallback.
- Integration tests: gateway live-client failure -> repair/fallback; structured-mode rejection -> safe fallback path.
- Manual QA: run the scenario corpus on the demo phone with live Gemini and airplane mode.
- Commands to run: `./gradlew test`, `./gradlew assembleDebug`, `./gradlew installDebug`.
- Quality checks: record state fit, personalization, actionability, autonomy, safety, concision, and approximate response time.

## Edge cases and failure modes

- Sparse profile: use generic but concrete local-safe anchors; do not invent details.
- Tired user with exercise goal: avoid high-effort exercise unless the user explicitly asks for it.
- Intentional rest: allow a neutral timed continuation option rather than always pushing the user away.
- Custom text conflicts with selected state: prioritize explicit custom text while keeping uncertainty.
- English input: classify safely and return Turkish output for the demo.
- Long profile/history: bound and prioritize context; do not send unnecessary records.
- Provider schema incompatibility: fall back to current JSON MIME/parsing rather than breaking the app.
- Slow network: respect the latency budget; repair is optional when insufficient time remains.
- Crisis signal: no live call, no repair, local support route only.
- Unsafe or invented output: reject and use deterministic fallback.
- Model outage/quota: preserve instant local quick states and fallback card/report.

## Rollback plan

Implement each slice in separate commits. The existing `MockAiGateway` remains the quality floor. If v2 structured output or repair logic destabilizes live calls, disable that capability and revert only the relevant slice while retaining the context builder, evaluation corpus, and improved prompts. Model changes remain local configuration overrides until proven.

## Executor prompt for fresh session

You are implementing Eşik AI Quality v2 on branch `feature/ai-quality-v2`. Before editing, inspect the repository and verify this plan against the actual code. If the plan is wrong or stale, briefly update it and explain the mismatch before editing.

Goal:
Upgrade Eşik’s Gemini layer from a basic prompt-and-parse implementation into a product-specific behavior engine that consistently produces concise, context-sensitive, realistic, and ethically cautious outputs for onboarding profiles, threshold interventions, and daily reflection. Preserve fast popup behavior, crisis short-circuiting, offline fallback, local counts, and the existing four-screen/UI contracts.

Known repo evidence:
- The branch is based on the current validated `feature/ai-personalization` flow.
- `AiPrompts.kt` contains three short generic prompts with no few-shot examples or explicit state strategy policy.
- `GeminiMessageClient.kt` uses one fixed temperature and JSON MIME mode for every task.
- `GeminiAiGateway.kt` sends useful context but does not compile it into explicit need/energy/objective/strategy constraints.
- Card parsing validates strings and banned language but not semantic state fit, duration, actionability, or invented content.
- Daily reporting sends records and counts but no local state/choice aggregate.
- `MockAiGateway` is a useful deterministic baseline and must remain the fallback.
- Existing tests do not provide a live-prompt scenario corpus or semantic contract tests.

Plan:
1. Define an AI quality rubric and at least 12 golden scenarios.
2. Add a pure local intervention context/policy builder.
3. Implement prompt v2 with compact contrastive few-shot examples and a richer internal structured card contract while preserving visible `AiCard` output.
4. Add semantic validation, one bounded repair attempt, and privacy-safe diagnostics.
5. Improve profile-generation grounding without changing the stored schema.
6. Add local report aggregates and evidence-backed report validation.
7. A/B at most three model/settings configurations on-device, document results, and freeze.

Ownership boundaries:
- You own `ai/**`, AI tests, `docs/PROMPT_DESIGN.md`, a new `docs/AI_EVALUATION.md`, and narrowly necessary AI configuration in Gradle/local-properties examples.
- Do not edit `ui/**`, theme/resources, overlay visuals, monitoring, cooldown, permissions, or Compose layout.
- Avoid shared interface/data-model changes unless essential; report any required cross-branch change before making it.
- Never commit an API key or raw sensitive test input.

Validation commands:
- `./gradlew test`
- `./gradlew assembleDebug`
- `./gradlew installDebug`

Manual validation scenarios:
- tired user with exercise goal
- procrastinating user with study goal
- intentional relaxation
- boredom/habit/waiting
- sparse profile
- custom Turkish and English text
- offline fallback
- blocked/malformed response
- crisis short-circuit
- seven-record evidence-backed report

Execution rules:
1. Do not assume file paths or API behavior are correct; verify them first.
2. Report plan/code mismatches before changing code.
3. Preserve existing visible behavior unless the plan explicitly improves AI output quality.
4. Implement one slice at a time and commit in reviewable increments.
5. Run the most relevant validation after each meaningful slice.
6. If tests fail, diagnose and fix before moving on unless clearly unrelated.
7. Keep direct mobile credentials clearly demo-only.
8. Do not overwrite or undo concurrent UI-redesign work.

Final response must include:
- Files changed
- Summary of each completed AI-quality slice
- Prompt/contract changes and rationale
- Validation commands run and results
- Scenario/A-B evaluation results
- Live/fallback behavior observed
- Any unresolved issues or follow-up work
