# Frozen Data Contract

Freeze this contract at the start of Saturday and share it with the team. Android core, UI, and AI code should depend on this contract rather than one another’s unfinished implementation.

```json
{
  "profil": {
    "isim": "Ayşe",
    "bolum": "İstatistik",
    "hobiler": ["gitar", "koşu"],
    "gelisim_alani": "İngilizce konuşma",
    "neden": "gece uyuyamıyorum",
    "hedef_app": "Instagram",
    "hedef_paket": "com.instagram.android",
    "limit_dk": 60
  },
  "kayitlar": [
    {
      "zaman_ms": 1788034200000,
      "saat": "23:10",
      "kullanim_dk": 78,
      "metin": "bugün yoruldum kafamı boşaltıcam",
      "secim": "yine_de_gir"
    }
  ]
}
```

Implementation extensions retained in the frozen contract:

- `gelisim_alani`: needed because onboarding explicitly collects a self-development area.
- `hedef_paket`: needed for Android usage lookup and launching the target app.
- `zaman_ms`: authoritative timestamp for reliable local-date grouping; `saat` is display-oriented.

Do not rename the semantic fields during the hackathon.

## Card AI output

```json
{
  "question": "Şu anda dinlenmeye mi, dikkatini başka yere vermeye mi ihtiyacın var?",
  "alternative": "İki dakika gitarını eline alıp tek bir akor geçişi deneyebilirsin."
}
```

## Daily report AI output

```json
{
  "observation_question": "Akşam saatlerindeki girişlerin yorgunlukla bağlantılı olabilir mi?",
  "micro_step": "Yarın 22:30'da telefonu şarja odanın diğer tarafında bırak."
}
```

All counts and numeric summaries are computed locally. The model never counts records, calculates durations, or chooses the user’s threshold.
