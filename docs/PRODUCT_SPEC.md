# Product Specification: Eşik

## One sentence

An Android app that intervenes after the user exceeds their own screen-time target, asks “What is happening right now?”, and generates a personalized reflection card based on the answer.

## Screens — exactly four

### 1. Onboarding

- Name; no account or password
- Hobbies, department/field, and one self-development area
- One sentence in the user’s own words: “Why I want to reduce this”
- Target app and daily time limit
- Usage Access and Draw Over Other Apps permission guidance
- Data remains on the device

### 2. Home

- Today’s numeric usage versus the user-defined limit
- Change the limit
- Open the daily report
- Start/stop monitoring and permission repair controls
- Hackathon debug controls may exist here but must not become extra product screens

### 3. Intervention card

Initial copy example:

> Bugün 78 dakika oldu. Hedefin 60'tı. Şu an ne oluyor?

The user enters free text. Before any network request, the app runs a local crisis-language gate. A normal result displays:

- One neutral, open question
- One two-to-five-minute alternative derived from the user’s hobbies or goals
- Two final choices: **Yine de gir** / **Vazgeçtim**

After the limit is exceeded, the intervention appears at most once every 15 minutes while the selected app is in the foreground. It must not appear on every app open.

### 4. Daily report

- Numeric facts without adjectives
- One observation phrased as a question, not a claim
- Exactly one micro-step for tomorrow
- Fewer than seven records on the current local date: show **Yeterli veri yok** and do not call the report model

## Runtime flow

```text
one-time setup
  -> user explicitly starts monitoring
  -> foreground service reads usage every 60 seconds
  -> selected app is active and the user-defined limit is reached
  -> overlay appears only if the 15-minute cooldown has expired
  -> user writes free text
  -> local crisis gate
  -> AI card call or deterministic fallback
  -> user chooses continue/stop
  -> record saved on-device
  -> eligible daily report uses only the current local date’s records
```

## AI responsibilities

| Place | Model class | Input |
|---|---|---|
| Card generation | Current fast Haiku-class model | User text, local time, usage/limit, hobbies, reason, optional profile context |
| Daily report | Current Sonnet-class model | Profile, today’s eligible records, locally computed numeric summary |

Model identifiers are configuration values because provider model IDs change. Verify current official Anthropic model IDs when implementing the HTTP client.

## AI language rules

The AI must not:

- Set or recommend the threshold
- Say the user’s behavior was “too much,” “a lot,” “çok,” “fazla,” or “aşırı”
- Diagnose addiction, anxiety, depression, or another condition
- Accuse, shame, moralize, or narrate past behavior as failure
- Claim causality from a small number of observations
- Pretend certainty about the user’s motives

The AI may reference only the user’s stated goal, supplied context, and numeric facts computed by the app.

## Safety and ethics

| Risk | Mitigation |
|---|---|
| Judgmental tone | Deterministic banned-language validator and fallback output; after the core loop works, add a lightweight second-pass AI review |
| Medical framing | Block diagnosis terms; phrase observations as questions |
| Overclaiming | No report below seven records for today; no causal claims |
| Crisis language | Local crisis gate; skip all AI calls and show support-oriented copy |
| Privacy | No account, device-local storage, one-action data deletion |
| Secret exposure | No key in Git; mobile-direct request is demo-only and replaced by a proxy in production |

## Stretch goals

Only after the core loop is stable:

- Multi-day trend chart
- “Bu yorum yargılayıcıydı” regenerate control
- Multiple target apps
