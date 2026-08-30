# Frozen Data Contract — Schema v4

Schema v4 extends the existing v3 personalization/intervention contract with optional local context-hypothesis fields. Existing v1/v2/v3 files remain readable: new fields have defaults and old records are never deleted or rewritten.

## Persisted device state

Representative shape:

```json
{
  "profil": {
    "sema_surum": 4,
    "isim": "Ayşe",
    "bolum": "İstatistik",
    "hobiler": ["gitar", "koşu"],
    "gelisim_alani": "daha düzenli çalışmak",
    "neden": "gece daha rahat uyumak istiyorum",
    "hedef_app": "Instagram",
    "hedef_paket": "com.instagram.android",
    "limit_dk": 60,
    "biyografi": "Derslerden sonra yoruluyorum ve çalışmaya başlamak yerine Instagram'da oyalanıyorum.",
    "kisisellestirme": {
      "profil_ozeti": "Özellikle bir işe başlamadan önce Instagram'a kaydığını anlattın.",
      "odak_hedefleri": ["ders çalışmak"],
      "hedefler": ["daha düzenli çalışmak"],
      "tekrarlayan_baglamlar": ["erteleme"],
      "tercih_edilen_aktiviteler": ["gitar", "koşu"],
      "dusuk_enerji_aktiviteleri": ["bir şarkı boyunca telefonu bırakmak"],
      "ton": "supportive_direct",
      "hizli_durumlar": [
        {
          "id": "procrastinating",
          "etiket": "Bir şeyi erteliyorum",
          "emoji": "🫠",
          "kategori": "avoidance"
        }
      ]
    }
  },
  "kayitlar": [
    {
      "zaman_ms": 1788034200000,
      "saat": "23:10",
      "kullanim_dk": 78,
      "metin": "Alışkanlıkla açtım",
      "durum_id": "habit",
      "durum_etiket": "Alışkanlıkla açtım",
      "girdi_yontemi": "quick_reply",
      "ai_soru": "Burada kalmayı şu anda bilinçli olarak seçiyor musun?",
      "ai_alternatif": "Telefonu iki dakika bırakıp sonra yeniden karar verebilirsin.",
      "ai_yansima": "Bu açılış biraz otomatik gelmiş olabilir.",
      "ai_aktivite_basligi": "2 dakikalık ara",
      "ai_sure_dk": 2,
      "ai_strateji": "environment_change",
      "tetikleyici": "threshold",
      "tahmin_durum": "habit",
      "tahmin_kabul": true,
      "secim": "vazgectim"
    }
  ]
}
```

## Schema-v4 context fields

### `tetikleyici`

The current product writes:

```text
threshold
```

The user-defined daily threshold is the only runtime intervention trigger. Experimental historical values such as `immediate_reopen`, `rapid_reopen_loop`, and `session_drift` remain parseable for backward compatibility, but the current monitor does not generate them.

### `tahmin_durum`

Optional canonical quick-state ID for the tentative context shown before the user selected a state. The current inference layer can produce only:

```text
habit
late_night
```

It does not infer procrastination, boredom, fatigue, overwhelm, low motivation, or other motives from usage events alone.

### `tahmin_kabul`

- `true`: the user explicitly confirmed the tentative context;
- `false`: the user explicitly rejected it and continued with normal context selection;
- absent: no hypothesis was offered or no explicit answer was recorded.

Absence is never interpreted as rejection.

Calibration uses only explicit answers. After at least three answers for the same hypothesis state, a recent acceptance ratio at or below `0.34` suppresses that hypothesis. At most the latest 30 explicit answers per state are considered.

## Usage-shape data

Recent `UsageEvents` may be read locally only **after** the normal threshold and cooldown conditions are already satisfied. They provide context; they do not trigger an intervention.

Derived transient values include:

- target-app open count in a short window;
- current/completed session durations;
- median completed-session duration;
- gap since the previous target session;
- previous foreground package;
- current continuous device-use run;
- local hour.

These raw/derived signals are not persisted and are not sent to Gemini. Only the optional canonical hypothesis ID and the user's explicit confirmation/rejection are stored.

To reduce false reopen signals from Android Activity transitions, adjacent same-package sessions separated by at most five seconds are coalesced unless a screen-off event occurred between them.

## Stable persisted enums

### `girdi_yontemi`

- `quick_reply`
- `text`
- `voice`

### `secim`

- `yine_de_gir`
- `vazgectim`

### `ton`

- `supportive_direct`
- `gentle`
- `practical`

Quick-state IDs are stable lowercase ASCII identifiers. Labels and emoji are display values and may change without breaking historical grouping.

## Canonical quick-state IDs

```text
tired
procrastinating
relaxing
bored
habit
waiting
low_motivation
overwhelmed
late_night
other
```

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
  "profile_summary": "Özellikle bir işe başlamadan önce Instagram'a kaydığını anlattın.",
  "focus_targets": ["ders çalışmak"],
  "goals": ["daha düzenli çalışmak"],
  "recurring_contexts": ["erteleme"],
  "preferred_activities": ["gitar", "koşu"],
  "low_energy_activities": ["bir şarkı boyunca telefonu bırakmak"],
  "tone": "supportive_direct",
  "quick_states": [
    {
      "id": "procrastinating",
      "label": "Bir şeyi erteliyorum",
      "emoji": "🫠",
      "category": "avoidance"
    }
  ]
}
```

Generated profile content is passed through application-side grounding/sanitization. Missing safe defaults may be completed deterministically; at most six quick states are stored. Quick-state IDs come only from the canonical taxonomy; labels and emoji may be personalized.

## Intervention local policy contract

Before Gemini is called, the app resolves the confirmed/user-selected context locally. The dynamic payload contains an authoritative `compiled_policy`, conceptually:

```json
{
  "resolved_state_id": "procrastinating",
  "need": "activation",
  "energy": "normal",
  "objective": "micro_start",
  "allowed_strategies": ["micro_start"],
  "max_duration_minutes": 5,
  "anchors": {
    "goals": ["daha düzenli çalışmak"],
    "activities": ["gitar", "koşu"],
    "low_energy_activities": ["bir şarkı boyunca telefonu bırakmak"]
  },
  "forbidden_patterns": [],
  "evidence_summary": "..."
}
```

The optional context hypothesis never changes the user threshold, need taxonomy, duration ceiling, or allowed strategy policy by itself; it only supplies a canonical state after explicit confirmation.

## Intervention AI structured output

```json
{
  "need": "activation",
  "strategy": "micro_start",
  "reflection": "Başlama anı şu anda işin kendisinden daha zor geliyor olabilir.",
  "question": "İlk küçük adımı yapmak daha ulaşılabilir olabilir mi?",
  "activity_title": "İlk 3 dakika",
  "alternative": "Yalnızca ilk adımı üç dakika boyunca açıp yapmayı deneyebilirsin.",
  "duration_minutes": 3,
  "personalization_anchor": "daha düzenli çalışmak"
}
```

Application-side semantic validation checks need/strategy/duration/anchor, field length, actionability, style, safety, and near-duplicate recent history. One bounded repair attempt is allowed; otherwise the deterministic local gateway is used.

Only the latest six intervention records are summarized for card generation. The model receives bounded state/choice/previous-alternative context, not the full raw history or old custom text.

## Daily report

Numeric usage, threshold, counts, dates, and candidate evidence are computed locally. If fewer than seven current-date records exist, the report model is not called.

Representative output:

```json
{
  "evidence_state_id": "procrastinating",
  "observation_question": "Erteleme dediğin anlarla devam kararların arasında bir örüntü olabilir mi?",
  "micro_step": "Yarın ilk erteleme anında yalnızca iki dakikalık bir başlangıç yapmayı dene."
}
```

## Compatibility rules

- Existing schema-v1/v2/v3 files remain readable.
- New stored fields require backward-compatible defaults.
- Missing `tahmin_kabul` means no explicit answer, never `false`.
- Numeric facts remain application-computed.
- Raw usage-shape signals remain device-local and transient.
- The user-defined threshold remains the only runtime trigger.
- The model never sets a threshold, diagnoses the user, or states unsupported causation as fact.
