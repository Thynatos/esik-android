# Frozen Data Contract — Schema v3

This document defines the persisted device-local contract and the current structured AI contracts. Existing schema-v1 and schema-v2 files remain readable: newer fields have defaults and old records are never deleted or rewritten.

## Persisted device state

```json
{
  "profil": {
    "sema_surum": 3,
    "isim": "Ayşe",
    "bolum": "İstatistik",
    "hobiler": ["gitar", "koşu"],
    "gelisim_alani": "daha düzenli çalışmak",
    "neden": "gece daha rahat uyumak istiyorum",
    "hedef_app": "Instagram",
    "hedef_paket": "com.instagram.android",
    "limit_dk": 60,
    "biyografi": "Derslerden sonra yoruluyorum ve çalışmaya başlamak yerine Instagram'da oyalanıyorum. Gitarı ve koşmayı seviyorum.",
    "kisisellestirme": {
      "hedefler": ["daha düzenli çalışmak", "gece daha rahat uyumak"],
      "tekrarlayan_baglamlar": ["yorgunluk", "erteleme"],
      "tercih_edilen_aktiviteler": ["gitar", "koşu"],
      "dusuk_enerji_aktiviteleri": [
        "bir şarkı boyunca telefonu bırakmak",
        "bir bardak su içip ekrandan uzaklaşmak"
      ],
      "ton": "supportive_direct",
      "hizli_durumlar": [
        {
          "id": "tired",
          "etiket": "Biraz yoruldum",
          "emoji": "😴",
          "kategori": "low_energy"
        },
        {
          "id": "procrastinating",
          "etiket": "Bir şeyi erteliyorum",
          "emoji": "🫠",
          "kategori": "avoidance"
        },
        {
          "id": "relaxing",
          "etiket": "Sadece kafa dağıtıyorum",
          "emoji": "😌",
          "kategori": "intentional_rest"
        }
      ]
    }
  },
  "kayitlar": [
    {
      "zaman_ms": 1788034200000,
      "saat": "23:10",
      "kullanim_dk": 78,
      "metin": "Bir şeyi erteliyorum",
      "durum_id": "procrastinating",
      "durum_etiket": "Bir şeyi erteliyorum",
      "girdi_yontemi": "quick_reply",
      "ai_soru": "Ertelediğin şeyin yalnızca ilk iki dakikasını yapmak daha ulaşılabilir olabilir mi?",
      "ai_alternatif": "Ödevin için yalnızca ilk iki dakikalık adımı başlatabilirsin.",
      "secim": "vazgectim",
      "strateji": "micro_start",
      "sonuc": "yardimci_oldu",
      "sonuc_zaman_ms": 1788034920000
    }
  ]
}
```

The persisted intervention record stores the visible AI question and alternative, plus the strategy identifier the shown copy actually describes. It never stores prompts, model reasoning, or internal policy objects.

`strateji` is written by the application, not by the model: it is the strategy the displayed alternative implements. `sonuc` and `sonuc_zaman_ms` are absent until the user answers the follow-up question, and are written exactly once.

## Stable persisted enums

### `girdi_yontemi`

- `quick_reply`
- `text`
- `voice`

### `secim`

- `yine_de_gir`
- `vazgectim`

### `sonuc`

- `bilinmiyor` — the user has not answered yet, or was never asked
- `yardimci_oldu`
- `yardimci_olmadi`
- `denenmedi`

Only `yardimci_oldu` and `yardimci_olmadi` count as evidence about a strategy. `denenmedi` closes the question without saying anything about the suggestion, and `bilinmiyor` is never inferred into one of the others.

### `strateji`

- `low_energy_reset`
- `micro_start`
- `timed_intentional_use`
- `environment_change`
- `sensory_break`
- `brief_activity`
- `other`

Strategy identifiers are stable and shared with the intervention AI contract below.

### `ton`

- `supportive_direct`
- `gentle`
- `practical`

Quick-state IDs are stable lowercase ASCII identifiers. Labels and emoji are display values and may change without breaking historical grouping.

## Profile AI input

Representative payload:

```json
{
  "name": "Ayşe",
  "department": "İstatistik",
  "biography": "Derslerden sonra yoruluyorum...",
  "explicit_hobbies": ["gitar", "koşu"],
  "explicit_improvement_area": "daha düzenli çalışmak",
  "explicit_reason": "gece daha rahat uyumak istiyorum"
}
```

## Profile AI output

```json
{
  "goals": ["daha düzenli çalışmak", "gece daha rahat uyumak"],
  "recurring_contexts": ["yorgunluk", "erteleme"],
  "preferred_activities": ["gitar", "koşu"],
  "low_energy_activities": ["bir şarkı boyunca telefonu bırakmak"],
  "tone": "supportive_direct",
  "quick_states": [
    {
      "id": "tired",
      "label": "Biraz yoruldum",
      "emoji": "😴",
      "category": "low_energy"
    }
  ]
}
```

Generated profile content is passed through application-side grounding/sanitization. Missing safe defaults may be completed deterministically; at most six quick states are stored.

## Intervention local policy contract

Before Gemini is called, the app resolves the current context locally. The dynamic payload includes an authoritative `compiled_policy` with fields equivalent to:

```json
{
  "resolved_state_id": "procrastinating",
  "need": "activation",
  "energy": "normal",
  "objective": "micro_start",
  "allowed_strategies": ["micro_start"],
  "max_duration_minutes": 5,
  "user_reported_helpful_strategy": "",
  "anchors": {
    "goals": ["daha düzenli çalışmak"],
    "activities": ["gitar", "koşu"],
    "low_energy_activities": ["bir şarkı boyunca telefonu bırakmak"]
  },
  "forbidden_patterns": [],
  "evidence_summary": "..."
}
```

Exact enum availability is defined in the AI implementation; this JSON illustrates the contract shape rather than a persisted object.

## Local strategy preference

`allowed_strategies` and `user_reported_helpful_strategy` are derived locally from the user's own answered records, before any request is made.

Rules, all enforced in `StrategyEffectivenessBuilder`:

- Only records in the same `durum_id` with a `strateji` and a `sonuc` of `yardimci_oldu`/`yardimci_olmadi` count as attempts.
- A strategy influences nothing until it has at least three answered attempts in that state.
- At a helpful ratio of 0.6 or higher it becomes `user_reported_helpful_strategy`, which is a prompt hint only. The semantic validator still accepts every allowed strategy.
- At a helpful ratio of 0.25 or lower it is removed from `allowed_strategies`.
- The allowed set is never emptied: if every acceptable strategy would be removed, none is.
- Only the most recent 60 answered attempts per state are considered, so old behaviour does not bind the user permanently.
- `other` is never promoted to a preference.

This signal reorders options the policy already considered acceptable. It never changes the need, the objective, the duration ceiling, the anchors, or the user-defined threshold, and it is never shown to the user as a score.

## Intervention AI structured output

The current internal model response is:

```json
{
  "need": "activation",
  "strategy": "micro_start",
  "question": "Ertelediğin şeyin yalnızca ilk iki dakikasını yapmak daha ulaşılabilir olabilir mi?",
  "alternative": "İlk adımı iki dakika boyunca açıp yapmayı deneyebilirsin.",
  "duration_minutes": 2,
  "personalization_anchor": "daha düzenli çalışmak"
}
```

Application-side semantic validation checks the returned need/strategy/duration/anchor against the local policy. One bounded repair attempt is allowed. Only `question` and `alternative` become the visible `AiCard` and are persisted with a completed intervention record.

The overlay opens with three stored quick states before any network request. Crisis-signalling external text is blocked locally and never enters the normal Gemini/repair path.

## Daily report local evidence

The application computes all counts and candidate patterns locally from current-date records. The model does not discover or recalculate numeric facts.

If fewer than seven current-date records exist, the model is not called.

## Daily report AI structured output

```json
{
  "evidence_state_id": "procrastinating",
  "observation_question": "“Bir şeyi erteliyorum” dediğin anlarla devam kararların arasında bir örüntü olabilir mi?",
  "micro_step": "Yarın ilk erteleme anında yalnızca iki dakikalık bir başlangıç yapmayı dene."
}
```

`evidence_state_id` must be a locally supplied supported candidate or empty when evidence is mixed/weak. The application validates the reflection before display. The persisted repository currently stores intervention records, not generated daily reports.

## Compatibility rules

- Do not rename/remove persisted v2 or v3 fields during the hackathon.
- New stored fields require backward-compatible defaults and a schema/documentation update.
- Internal Gemini structured fields may evolve without changing persisted schema only when the visible/persisted contract remains compatible.
- Numeric usage, threshold, counts, dates, and evidence aggregates remain application-computed.
- The model never sets a threshold, diagnoses the user, or states unsupported causation as fact.
