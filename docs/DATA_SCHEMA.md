# Frozen Data Contract — Schema v2

Freeze this contract before parallel UI, Android Core, and AI work. All fields are device-local. Existing schema-v1 files remain readable: every v2 field has a default and old records are not deleted.

## Device state

```json
{
  "profil": {
    "sema_surum": 2,
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
      "secim": "vazgectim"
    }
  ]
}
```

## Stable enums

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

Quick-state IDs must be stable lowercase ASCII identifiers. Labels and emoji are display values and may change without breaking historical grouping.

## Profile AI input

```json
{
  "name": "Ayşe",
  "department": "İstatistik",
  "biography": "Derslerden sonra çok yoruluyorum...",
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

The application fills missing quick states from a deterministic safe list and stores at most six.

## Intervention AI output

```json
{
  "question": "Ertelediğin şeyin yalnızca ilk iki dakikasını yapmak daha ulaşılabilir olabilir mi?",
  "alternative": "Ödevin için yalnızca ilk iki dakikalık adımı başlatabilirsin."
}
```

The popup opens with three stored quick states before any network request. Crisis-signalling custom text is blocked locally and never sent to the model.

## Daily report AI output

```json
{
  "observation_question": "“Bir şeyi erteliyorum” seçtiğin anlarda verdiğin kararlar arasında bir örüntü olabilir mi?",
  "micro_step": "Yarın çalışmaya başlamadan önce yalnızca iki dakikalık tek bir adım yap."
}
```

All counts, durations, dates, and the user's limit are computed locally. The model never sets a threshold, recounts records, diagnoses the user, or states causation as fact.
