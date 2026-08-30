# Eşik — Frozen Five-Minute Demo

This is the single frozen route for rehearsal, the primary recording, and the backup recording. Do not change code, prompts, models, polling, or UI between runs.

## Frozen setup

- Device: the validated Android phone with the current `feature/final-integration` build.
- Target app: Instagram.
- User-defined threshold: 30 minutes.
- Models: profile/card `gemini-2.5-flash-lite`; report `gemini-3.6-flash`.
- Monitoring: enabled; Usage Access, overlay, and notification permissions ready.
- Report: prepare at least seven current-day records using the existing demo-data control before recording.
- Network: live for the main route; keep the already validated offline fallback as a spoken reliability point unless a separate fallback clip is required.

### Frozen persona

Name: Ayşe  
Field: İstatistik  
Narrative:

> İstatistik öğrencisiyim. Ders çalışmaya başlarken bazen erteliyorum; yorulduğumda Instagram'a kayıyorum. Müzik ve gitar seviyorum. Daha düzenli çalışmak ve gece daha rahat uyumak istiyorum.

This explicitly supplies the study goal, fatigue/procrastination contexts, music, and guitar, so any personalization shown in the demo can be checked against evidence.

## Timed route

### 0:00–0:30 — Problem and boundary

Show Eşik briefly and say:

> Eşik, ekran süresini yargılayan veya uygulamayı zorla engelleyen bir araç değil. Kullanıcının kendi belirlediği sınıra ulaşıldığında kısa bir düşünme alanı açıyor. Kullanıcı ister bilinçli biçimde devam ediyor, ister küçük bir alternatif deniyor.

Point out that Eşik never defines the threshold and does not diagnose addiction.

### 0:30–1:20 — Narrative onboarding and profile

Enter or speak the frozen narrative. Show the generated summary, focus targets, recurring moments, and alternatives.

Say:

> Gemini serbestçe bir kişilik yorumu yapmıyor. Yalnızca Ayşe'nin verdiği bilgileri yapılandırıyor; uygulama da uydurulmuş hobi veya hedefleri yerel olarak eliyor.

Select Instagram and set the threshold to 30 minutes.

### 1:20–1:55 — Home and monitoring

Show today’s locally measured usage, the 30-minute user-defined threshold, monitoring state, and the persistent Eşik notification.

Say:

> Kullanım sayıları Android'den ve cihaz içinde geliyor. Arka plan servisi hedef uygulamayı beş saniyede bir kontrol ediyor; eşik ve 15 dakikalık bekleme kuralı tamamen yerel.

Start monitoring if it is not already active.

### 1:55–3:10 — Real overlay and personalized card

Open Instagram. Keep the transition visible until the overlay appears within roughly 5–10 seconds.

Choose the procrastination quick state or enter:

> Ders çalışmaya başlamayı erteliyorum.

Show the generated card and say:

> Uygulama önce ihtiyacı, enerji düzeyini, izin verilen stratejiyi ve en uzun süreyi yerel olarak belirliyor. Gemini yalnızca bu sınırlar içinde kısa soruyu ve uygulanabilir alternatifi yazıyor.

Choose **Bunu deneyeceğim**. Mention that **Yine de devam et** is equally valid and records an intentional continuation without shame.

### 3:10–4:10 — Local record and daily reflection

Return to Eşik and show that the intervention decision is stored locally. Open the daily report prepared with seven or more current-day records.

Say:

> Sayılar ve aday örüntüler cihazda hesaplanıyor. Gemini yalnızca kanıtla sınırlı, soru biçiminde bir gözlem ve iki-beş dakikalık tek bir deney yazıyor. Yedi kaydın altında model hiç çağrılmıyor.

Show the tentative observation and the single micro-step.

### 4:10–4:40 — Safety and reliability

Say:

> Kriz dili normal yapay zekâ yoluna gönderilmiyor; yerel güvenlik akışı devreye giriyor. Ağ, kota, şema veya doğrulama hatasında uygulama deterministik yerel karta dönüyor. Profil ve kayıtlar cihazda kalıyor ve tek onaylı işlemle silinebiliyor.

Mention that airplane-mode fallback, sparse-profile grounding, intentional-rest tone, and both final choices were validated before freeze.

### 4:40–5:00 — Close

Finish with:

> Eşik'in amacı kullanıcı adına karar vermek değil; kullanıcının kendi eşiğinde, kendi bağlamıyla, daha bilinçli bir karar verebilmesi için birkaç saniyelik alan açmak.

## Recording gate

Before submission:

1. Run this exact route twice consecutively without code or configuration changes.
2. Record the third clean run as the primary video.
3. Immediately record one backup run.
4. Check that the overlay arrival, Gemini card, saved choice, and daily reflection are legible.
5. Keep both raw recordings until the submission is accepted.
6. After the presentation/submission, rotate or revoke the embedded hackathon key.

