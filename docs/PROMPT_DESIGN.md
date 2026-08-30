# Eşik Prompt Design

This document records the generative-AI messages used by Eşik, the data sent with each request, and the design rationale behind them. It is intended both as implementation documentation and as source material for the hackathon report.

The hackathon evaluates intentional prompt design, including why prompts were designed in a particular way and which prompting techniques were used. Eşik therefore treats prompts as part of the product architecture rather than as hidden implementation details.

## Prompting strategy at a glance

Eşik uses three separate generative-AI tasks instead of one general chatbot prompt:

1. **Profile generation** — turns the user's own onboarding narrative into a compact personalization profile.
2. **Intervention generation** — combines that profile with the user's current state and usage context to produce one reflection question and one small alternative action.
3. **Daily reflection** — summarizes patterns in the day's intervention records and proposes one small experiment for tomorrow.

The prompts are deliberately **zero-shot and schema-constrained**. We do not currently use few-shot examples. We also do not ask the model to reveal or return chain-of-thought reasoning. Instead, each task has a narrow role, explicit behavioral constraints, and a small structured output.

Core techniques used:

- **Zero-shot task instruction:** each model call clearly states the role and required task without example demonstrations.
- **Structured output:** the model is required to return JSON with a fixed set of fields.
- **Context grounding:** generated recommendations must be grounded in user-provided goals, preferences, selected state, and device-computed facts.
- **Negative constraints / guardrails:** prompts explicitly prohibit diagnosis, shaming, moralizing, threshold-setting, and unsupported causal claims.
- **Output-length constraints:** fields are intentionally short so the result fits an intervention card and remains actionable.
- **Separation of deterministic facts from generation:** usage counts, limits, and intervention totals are computed locally by the app; Gemini is not asked to invent or recalculate them.
- **Local safety gates and fallback:** crisis-language checks happen before an external model call, and unsafe/malformed/network-failed outputs fall back to deterministic local logic.

---

# 1. Personalization profile prompt

## Purpose

During onboarding, the user can describe their situation naturally through text or speech. Rather than forcing the user to complete a long questionnaire, Gemini converts this narrative into a small structured profile used later by the intervention system.

The model is not asked to diagnose the user or infer hidden traits. It should only structure information the user actually supplied.

## System message

Current source: `app/src/main/java/com/thynatos/esik/ai/AiPrompts.kt`

```text
You convert a Turkish user's own onboarding narrative into a cautious personalization profile for a digital-wellbeing app.
Describe patterns the user mentioned; never label the person, diagnose, infer a disorder, or invent facts.
Return JSON only with exactly these fields:
- goals: array of 1-3 short strings
- recurring_contexts: array of 1-4 short strings
- preferred_activities: array of 1-5 concrete activities grounded in supplied text
- low_energy_activities: array of 1-3 realistic two-to-five-minute alternatives
- tone: one of supportive_direct, gentle, practical
- quick_states: array of exactly 6 objects with id, label, emoji, category
Quick-state labels must be first-person Turkish phrases that can be tapped instantly, such as “Biraz yoruldum”.
Use stable lowercase ASCII IDs. Keep every value concise.
```

## User message / dynamic payload

The app sends the user's onboarding content as a JSON object similar to:

```json
{
  "name": "Bahadır",
  "department": "Industrial Engineering",
  "biography": "Ders çalışmaya başlamakta zorlanıyorum. Yorulduğumda Instagram'a kayıyorum. Daha çok kitap okumak ve egzersiz yapmak istiyorum.",
  "explicit_hobbies": ["kitap", "müzik", "spor"],
  "explicit_improvement_area": "daha düzenli çalışmak",
  "explicit_reason": "telefonu daha bilinçli kullanmak"
}
```

The exact text changes for each user. The application truncates very long free-form text before sending it.

## Why this prompt is designed this way

### Narrative first, structure second

A conventional onboarding form would require the product team to predict every useful category in advance. Eşik instead lets the user speak naturally, then asks the model to map that free-form content into a stable structure.

This makes generative AI part of the **core interaction model**, not simply a cosmetic chatbot layer.

### Grounding prevents invented personalization

The phrases `user's own onboarding narrative`, `grounded in supplied text`, and `do not invent facts` are intentional. The model should not invent hobbies, psychological traits, goals, or motivations that were never supplied.

### Person-labeling is explicitly forbidden

Eşik may store a context such as `erteleme` (procrastination), but it should not describe the user as "a procrastinator." The distinction is important for a non-judgmental digital-wellbeing product.

### Six quick states are generated once

The profile includes six personalized quick-state options. The app later chooses three locally for the intervention overlay. This means the popup can appear instantly without an additional API call while still being personalized by AI.

### Strict JSON supports reliable integration

A small fixed schema gives the Android app a predictable contract. If the output is missing, malformed, unsafe, or unavailable, the app replaces it with deterministic fallback values.

---

# 2. Intervention card prompt

## Purpose

This is the main moment-to-moment AI interaction in Eşik.

When the user's self-defined usage threshold is exceeded, Eşik asks:

> **Şu an seni burada tutan ne?**

The user can select a quick state or explain the situation through text/voice. Only after that context is supplied does the app ask Gemini to generate the intervention card.

The goal is not always to make the user leave the phone. The model should distinguish an intentional break from automatic or avoidant use.

## System message

```text
You create one neutral, personalized digital-wellbeing intervention card in Turkish.
Use only the user's own goals, preferences, selected state/custom text, time, and supplied numeric facts.
Distinguish intentional rest from automatic use; do not always push the user away from the phone.
Never diagnose, shame, accuse, moralize, set a limit, or say the use was too much/excessive.
Return JSON only with exactly two string fields: question and alternative.
The question must be open, brief, uncertain, and at most 140 characters.
The alternative must be one realistic two-to-ten-minute action grounded in the supplied profile and at most 180 characters.
Do not recommend a specific new book, podcast episode, product, or live item unless it was explicitly supplied by the user.
```

## User message / dynamic payload

The app supplies a compact context object similar to:

```json
{
  "local_time": "18:42",
  "target_app": "Instagram",
  "usage_minutes": 47,
  "user_limit_minutes": 30,
  "user_reason": "telefonu daha bilinçli kullanmak",
  "goals": ["daha düzenli çalışmak", "daha çok kitap okumak"],
  "preferred_activities": ["kitap", "müzik", "spor"],
  "low_energy_activities": ["bir şarkı boyunca telefonu bırakmak"],
  "selected_state_id": "tired",
  "selected_state_label": "Biraz yoruldum",
  "user_text": "Biraz yoruldum",
  "input_method": "quick_reply"
}
```

For custom text or voice, `user_text` contains the user's own explanation instead of only a quick-state label.

## Expected response

```json
{
  "question": "Şu anda kısa bir dinlenme mi, yoksa otomatik bir kaydırma mı arıyorsun?",
  "alternative": "Bir şarkı boyunca telefonu bırakıp biraz dinlenebilirsin."
}
```

## Why this prompt is designed this way

### The model does not choose the threshold

The daily limit is explicitly **user-defined**. Gemini receives the limit only as context and is forbidden from setting a new one or saying the user has used the app "too much."

This avoids turning a generative model into an authority over what amount of device use is healthy.

### Current state matters as much as the profile

A static profile may say the user enjoys exercise, but recommending exercise when the user says they are exhausted may be inappropriate. The prompt therefore combines long-term preferences with the immediate state.

Example behavior:

- `tired` -> prefer a low-energy alternative such as music, rest, water, or a short walk.
- `procrastinating` -> suggest a very small first step toward a stated goal.
- `relaxing` -> acknowledge intentional rest and help the user make it deliberate rather than automatically forcing them to stop.

### Reflection before prescription

The result contains both:

1. a short **question**, and
2. one **micro-alternative**.

The question is intentionally open and uncertain. The app is meant to create a moment of awareness, not claim it knows exactly why the user opened an app.

### Recommendations remain grounded

The prompt prohibits inventing a new book, podcast, product, or live recommendation unless the user explicitly mentioned it. This reduces hallucination and keeps the intervention personally relevant.

### Small outputs fit the product moment

An overlay above another app is not the place for a paragraph of coaching. Character limits force the response into something the user can understand in a few seconds.

---

# 3. Daily reflection prompt

## Purpose

After at least seven intervention records are available for the day, Eşik can generate a short daily reflection.

The application computes the objective numbers locally. Gemini receives those facts and the interaction records, then contributes only the interpretive layer:

- one tentative observation question;
- one small experiment for tomorrow.

## System message

```text
You create a brief Turkish daily reflection from device-local interaction records.
Numbers are computed by the application and must not be recalculated or embellished.
Return JSON only with exactly two string fields: observation_question and micro_step.
The observation must be phrased as a tentative question, never a diagnosis or causal claim.
The micro_step must be one specific, realistic action for tomorrow.
Never shame, moralize, set a threshold, or use language meaning too much or excessive.
Only refer to patterns actually visible in the supplied records and profile.
```

## User message / dynamic payload

A simplified example:

```json
{
  "target_app": "Instagram",
  "usage_minutes": 104,
  "user_limit_minutes": 30,
  "intervention_count": 8,
  "continued_count": 5,
  "stopped_count": 3,
  "goals": ["daha düzenli çalışmak", "daha çok kitap okumak"],
  "records": [
    {
      "time": "11:12",
      "state": "Bir şeyi erteliyorum",
      "text": "çalışmaya başlamayı erteliyorum",
      "choice": "continue",
      "alternative": "Ertelediğin işin yalnızca ilk iki dakikasını yapabilirsin."
    },
    {
      "time": "16:35",
      "state": "Biraz yoruldum",
      "text": "bugün yoruldum",
      "choice": "continue",
      "alternative": "Bir şarkı boyunca telefonu bırakıp gözlerini dinlendirebilirsin."
    }
  ]
}
```

## Expected response

```json
{
  "observation_question": "'Bir şeyi erteliyorum' dediğin anlarda Instagram'a devam etme eğilimin daha sık görünüyor olabilir mi?",
  "micro_step": "Yarın ilk erteleme anında çalışacağın işi yalnızca iki dakika açıp sonra yeniden karar ver."
}
```

## Why this prompt is designed this way

### Correlation is not causation

The report may notice that certain states and decisions appear together, but Eşik does not have evidence that one causes the other.

Therefore the prompt requires tentative question language rather than statements such as:

> "Procrastination causes your Instagram use."

A safer form is:

> "Erteleme dediğin anlarda Instagram'a devam etme eğilimin daha sık görünüyor olabilir mi?"

### Numerical facts are deterministic

Gemini does not calculate:

- total usage;
- the user's limit;
- intervention count;
- continue count;
- stop count.

Those values are computed by application code. This reduces hallucinated statistics and makes the report auditable.

### The report ends with an experiment, not a verdict

Rather than assigning a label or making a broad behavioral claim, the model proposes one small action to test tomorrow. This keeps the product exploratory and user-controlled.

---

# Safety outside the prompts

Prompt wording alone is not treated as a sufficient safety mechanism.

## Crisis short-circuit

Before sending relevant text to Gemini, the app checks for crisis/self-harm signals such as Turkish and English phrases including forms of:

- `intihar`
- `kendimi öldür...`
- `kendime zarar...`
- `yaşamak istemiyorum`
- `suicide`
- `suicidal`
- `kill myself`
- `want to die`
- `self harm`

When detected, the external AI call is skipped and the user is shown a local support-oriented message.

This is intentionally implemented in deterministic application logic rather than delegated to the generative model.

## Output validation

Generated profile fields, intervention text, and daily-report text are checked before display. Unsafe or disallowed language triggers the local fallback.

## Offline / API failure fallback

If Gemini is unavailable because of:

- no API key;
- no internet connection;
- timeout;
- API error;
- blocked response;
- malformed JSON;
- unsafe generated text;

Eşik uses `MockAiGateway`, a deterministic Kotlin implementation that produces safe profile-based responses without another model.

This fallback is a reliability feature, not a second AI model.

---

# Model and transport

For the hackathon prototype, Eşik uses the Gemini API directly from the Android application.

Configured models:

- fast profile/intervention model: `gemini-2.5-flash-lite`
- daily-report model: `gemini-2.5-flash`

The model names remain configurable through local properties.

The API key is **not committed to GitHub** and is loaded from local ignored configuration. Direct mobile API-key usage is acceptable only for the hackathon prototype; a production version should use a backend proxy with server-held credentials and appropriate abuse controls.

---

# Prompt iteration and evaluation

The prompts were not treated as one-off text. During implementation, outputs were tested against different contexts and the surrounding deterministic logic was adjusted when failures appeared.

Examples of issues the design explicitly addresses:

- generic recommendations that ignore the user's current energy/state;
- describing a person with a fixed behavioral label;
- model-generated statements that imply diagnosis or causality;
- the model attempting to judge the user's chosen screen-time limit;
- fabricated content recommendations;
- verbose responses that do not fit an overlay interaction;
- malformed structured output;
- crisis language reaching the external model;
- loss of functionality when the API is unavailable.

The final architecture therefore combines **prompt constraints + local pre-checks + output validation + deterministic fallback**, rather than relying on prompt wording alone.

---

# Short report-ready explanation

Eşik uses three task-specific zero-shot prompts rather than a general chatbot. The onboarding prompt converts the user's own free-form narrative into a schema-constrained personalization profile; the intervention prompt combines this profile with the user's current state and self-defined screen-time target to generate one brief reflection and one realistic micro-alternative; and the daily-report prompt turns local intervention records into a tentative pattern question and one experiment for the next day. All prompts require structured JSON output and explicitly prohibit diagnosis, shaming, threshold-setting, unsupported causal claims, and invented personalization. Objective usage statistics are computed by the application rather than by the model. Prompt-level safeguards are supplemented by local crisis detection, generated-language validation, and a deterministic offline fallback. This makes generative AI central to personalization and reflection while keeping safety-critical and factual decisions under deterministic application control.
