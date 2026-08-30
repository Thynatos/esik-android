# Eşik — Hackathon Report

## Problem and product

Digital-wellbeing tools often reduce a complex moment to a screen-time number or a hard block. That misses two important facts: the same amount of use can represent intentional rest or automatic avoidance, and only the user can decide what threshold matters to them.

Eşik is an Android prototype that creates a short pause after the user reaches a daily usage threshold they set for one selected app. It reads usage locally, waits until the selected app is foreground, and shows a real system overlay. The overlay asks what is happening now, then offers one short reflection and one small alternative. The user can intentionally continue or try the alternative; Eşik never decides for them and never labels their usage as addiction.

The prototype combines Jetpack Compose, `UsageStatsManager`, a foreground monitoring service, `TYPE_APPLICATION_OVERLAY`, Android speech recognition, device-local JSON storage, and Gemini. It has four in-app screens—onboarding, Home, intervention, and daily report—plus the system overlay.

## Models and three prompt tasks

The validated demo configuration is:

| Task | Model | Role |
|---|---|---|
| Profile structuring | `gemini-2.5-flash-lite` | Structure only the goals, contexts, and activities supplied during onboarding |
| Intervention card | `gemini-2.5-flash-lite` | Phrase one context-appropriate question and one short, actionable alternative |
| Daily reflection | `gemini-3.6-flash` | Turn locally computed evidence into one tentative question and one two-to-five-minute experiment |

The first task converts a natural Turkish narrative into a constrained profile. A grounding sanitizer rejects invented hobbies, goals, or personal facts and fills missing safe defaults locally. The second task receives a policy already compiled by Kotlin: need, energy level, objective, allowed strategies, maximum duration, and allowed personalization anchors. The model cannot freely choose a behavior policy. The third task receives counts and candidate patterns computed on-device; it does not calculate usage facts or discover patterns from raw history.

## Prompt and system design rationale

Eşik decomposes AI into narrow tasks instead of using one general chatbot prompt. Each live request uses named evidence fields, compact contrastive examples, schema-constrained JSON, and explicit negative constraints. For example, a tired state may allow a low-energy reset but not an unsupported workout, while procrastination permits a two-minute micro-start. Intentional relaxation must preserve the option to continue without shame.

Valid JSON is not sufficient. Application-side validators check policy fit, question form, actionability, duration, grounding, Turkish style, autonomy, and unsafe or judgmental language. One bounded repair attempt is allowed for an invalid card; provider errors, timeouts, malformed output, validation failure, or a failed repair immediately use the deterministic local gateway. Chain-of-thought is neither requested nor stored.

The Android trigger is also deterministic. The foreground service polls every five seconds and shows an overlay only when the selected app is foreground, local usage is at or above the user-defined threshold, the screen is active, permissions are available, no overlay is already visible, and the 15-minute cooldown has expired. AI never participates in trigger eligibility or threshold selection.

## Challenges and validation

The main engineering challenge was connecting Android system behavior, live generative output, and a safe fallback into one predictable route. Physical-device testing was essential for Usage Access, foreground detection, overlays, service restart behavior, notification identity, and speech recognition.

The final report path exposed a provider-specific issue: a 520-token budget could be consumed by model reasoning before JSON appeared, and the tested structured-output subset rejected `additionalProperties`. The report budget was raised to 2,048 tokens and unsupported schema fields were removed. The retest returned a live, evidence-grounded report with a two-minute micro-step.

The combined gate covered grounded onboarding, tired and procrastination interventions, custom text, physical voice input, both final choices, deterministic offline fallback, local report eligibility, and live daily reflection. The final Xiaomi 2311DRK48G / Android 15 smoke pass measured the Instagram overlay at 6.0 seconds and confirmed that reopening during the following cooldown did not create a second overlay. Unit tests and debug assembly pass locally and in GitHub Actions.

## Ethics, privacy, and limitations

The user owns the threshold. Eşik never recommends a limit, calls usage excessive, diagnoses addiction or another condition, or treats continuation as failure. Generated content is tentative and non-shaming. Sparse profiles must remain sparse instead of receiving fabricated personalization. Crisis-signalling text is detected locally and bypasses the normal Gemini and repair path. With fewer than seven current-day records, the daily-report model is not called.

Profiles, intervention records, numeric report facts, monitoring state, and crisis checks remain on-device. Backup and device-transfer rules exclude the stored profile, records, and monitoring preferences, and the app offers a visible one-action deletion flow with confirmation. Relevant text is sent to Gemini only for live generation, as disclosed during onboarding.

The demo architecture calls Gemini directly from the Android app. Although the key is stored only in ignored `local.properties` and no key exists in Git history, it is embedded in the demo APK. A production system would require a backend proxy or short-lived credentials, abuse controls, and an explicit retention policy. OEM background restrictions and unusual clock/timezone transitions remain deferred robustness work rather than claims of universal production readiness.

