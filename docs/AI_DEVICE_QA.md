# AI Personalization — Device QA

This branch is stacked on `work/android-core`. Validate both offline fallback and live AI before merging.

## 1. Check out the branch

```powershell
git fetch origin
git switch -c feature/ai-personalization --track origin/feature/ai-personalization
```

If the local branch already exists:

```powershell
git switch feature/ai-personalization
git pull --ff-only
```

## 2. Offline/fallback build

Leave `ANTHROPIC_API_KEY` blank in `local.properties`.

```properties
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
ANTHROPIC_API_KEY=
```

Then run:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

The full flow must work without a key or network connection through `MockAiGateway`.

## 3. Reset onboarding state

The new onboarding appears only when no profile is stored. Clear the app before the first test:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell pm clear com.thynatos.esik
```

Reopen Eşik and re-grant Usage Access and Draw Over Other Apps.

## 4. Offline acceptance path

1. Enter a name.
2. Speak or type a narrative that mentions a goal, a preferred activity, and a recurring context such as tiredness or procrastination.
3. Generate the profile.
4. Confirm the profile summary contains concise goals, contexts, activities, and three visible quick states.
5. Finish onboarding, select an already-used target app, and set its limit below today's use.
6. Start monitoring and open the target app.
7. Confirm the overlay immediately shows three quick-state buttons before any AI request.
8. Select a quick state and confirm a safe fallback reflection and micro-alternative appear.
9. Test **Deneyeceğim** and **Yine de gir**.
10. Reset the cooldown by changing the limit, then test custom text and custom voice.
11. Load the four-day demo data and verify the report is available after seven current-day records.

## 5. Live Anthropic test

Add a temporary hackathon API key only to the ignored local file:

```properties
ANTHROPIC_API_KEY=<your-local-demo-key>
ANTHROPIC_FAST_MODEL=claude-haiku-4-5-20251001
ANTHROPIC_REPORT_MODEL=claude-sonnet-5
```

Rebuild and reinstall. Never commit or paste the key into GitHub, chat, screenshots, or the demo recording.

Validate:

- onboarding profile output changes from the deterministic fallback while preserving the JSON contract;
- quick-state/card responses remain short and grounded in the stored profile;
- airplane mode, timeout, malformed output, or a blank key falls back without breaking the popup;
- daily report numbers remain locally computed;
- no diagnostic, judgmental, threshold-setting, or causal wording appears.

## 6. Voice checks

- Cancel speech recognition: editable text must remain available.
- Deny or remove the speech recognizer: the app must show a text fallback.
- Trigger voice from the real overlay: the overlay should hide while the system recognizer is visible and return with editable text.
- Dismiss the overlay while recognition is open: a late result must not recreate a duplicate overlay.

## 7. Crisis/safety checks

Use the internal test phrases maintained by the team; do not display them in demo footage.

Expected behavior:

- onboarding shows the local support message and does not call Anthropic;
- custom intervention text shows the support route and is not sent to Anthropic;
- crisis-signalling historical/profile context causes the report/card to use local fallback;
- unsafe generated output is discarded and replaced by the deterministic safe result.

## 8. Privacy statement to verify in the UI

- Account and intervention history are stored on the device.
- When live AI is configured, relevant text is sent to the Anthropic API for generation.
- Speech-to-text is handled by the phone's configured speech-recognition service and may not be fully on-device.
- The direct mobile API key is hackathon-only; production requires a backend proxy with server-held credentials, abuse controls, and an explicit retention/logging policy.

## 9. Final pass

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

The branch is ready for review only when CI is green and the offline path, live path, system overlay, voice bridge, and crisis short-circuit have each been observed on the physical demo phone.
