# Three-Person Two-Day Plan

No role is part-time. Each person has a full independent track, and the interfaces are arranged so one role does not block another.

## Saturday morning: freeze first

The first shared action is to approve `docs/DATA_SCHEMA.md` and post it to the group. Do not start parallel implementation with an unfrozen contract; otherwise three people will wait on one another or create incompatible fields.

## Roles

### A — Android core — highest-risk track

- Usage Access and Draw Over Other Apps permission flow
- `UsageStatsManager` readings per selected package
- User-started foreground service with 60-second polling
- Target-app foreground detection
- User-defined threshold trigger and 15-minute cooldown
- System overlay behavior
- **Yine de gir:** dismiss overlay and keep/return target app in front
- **Vazgeçtim:** dismiss overlay and move away from target app

Owned paths: `permissions/`, `usage/`, `monitor/`, `overlay/`, and coordinated manifest edits.

### B — UI and local UX

- Onboarding
- Home usage/limit state
- Intervention screen states
- Daily report layout
- App picker and limit input UX
- Device-local persistence UX
- Visual polish

Owned paths: `ui/`. Coordinate changes to `EsikApp.kt` and `data/`.

### C — AI, integration, report, and presentation

- Freeze and publish the data contract Saturday morning
- In the first 30 minutes, keep or improve the fixed-JSON mock gateway
- Card prompt and daily-report prompt
- HTTP client, current model configuration, strict JSON parsing, timeout/error fallback
- Crisis gate and generated-language validation
- Integration state, report/ethics write-up, demo data, presentation, backup flow

Owned paths: `ai/`, `docs/`, demo data, and coordinated integration files.

## Critical dependency rule

C must provide the mock AI path in the first 30 minutes. B builds against `AiGateway` and must never wait for network integration. A can work independently against the frozen profile/package/limit contract.

Do not put two people in the same file at the same time. In particular, coordinate edits to:

- `app/src/main/java/com/thynatos/esik/EsikApp.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- frozen data-model files

## Saturday

| Time | A — Android core | B — UI | C — AI/integration |
|---|---|---|---|
| Morning | Permissions + package usage reads | Onboarding + Home | Freeze schema, verify mock gateway, prompt v1 |
| Noon checkpoint | **Does the overlay appear above the selected app on the physical demo phone after the limit?** | Intervention UI | Real API connection behind existing interface |
| Afternoon | Cooldown + continue/stop app behavior | Finish intervention states | Prompt v2 from actual outputs, strict parser/fallback |
| Evening | Three-person end-to-end first pass | Same | Integration lead; record failures and owners |

If the noon checkpoint fails, A works only on that risk until it passes or a documented demo fallback is prepared. B and C continue against the mock implementation.

## Sunday

| Time | A — Android core | B — UI | C — AI/integration |
|---|---|---|---|
| Morning | Edge cases, lock screen, battery/OEM tests | Daily report UI | Daily report prompt and eligibility |
| Noon | Support UI/integration | Finish report | Crisis gate, output validator, fallback tests |
| Afternoon | Prepare demo phone | Visual corrections | Report + ethics section + seeded demo state |
| Evening | Three rehearsals, backup video, final submission | Same | Same |

C owns prepared data across four days, with enough current-day records for the report. Never reach the stage with an empty report screen.
