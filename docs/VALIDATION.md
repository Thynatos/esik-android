# Validation Record

This file separates checks performed on the generated starter from validation completed on a normal Android development machine and physical device.

## Passed in the generation environment

- Bash syntax for the wrapper, repository-creation, and pure-Kotlin verification scripts
- XML parsing for the manifest and all resources
- TOML parsing for the version catalog
- JSON parsing for every JSON contract/example in the Markdown documentation
- Secret-pattern scan: no Anthropic key or private-key material is present
- Pure Kotlin compilation and execution; repository tests compiled and passed through a lightweight local runner, covering:
  - crisis-language detection
  - ordinary-text non-match
  - blocked generated-language checks
  - word-fragment safety (`küçük` must not be mistaken for `çok`)
  - cooldown first-show, pre-boundary, boundary, and clock-rollback behavior
  - four-day demo seeding with eight records on the current date
  - report unavailable below seven records and available at seven
  - display-safe deterministic card/report fallback output

Run the same lightweight logic check on macOS/Linux with:

```bash
./scripts/verify-pure-kotlin.sh
```

## Physical-device validation — 2026-08-29

Completed on a real Android phone from the `work/android-core` development setup:

- `./gradlew test` / `gradlew.bat test`: passed
- `./gradlew assembleDebug` / `gradlew.bat assembleDebug`: passed
- Debug APK installed successfully through ADB/Gradle
- Usage Access permission flow works
- Draw-over-other-apps permission flow works
- Selected-app usage minutes are read and displayed with believable values
- Foreground monitoring service starts successfully
- User-selected target app is detected while in the foreground
- A limit below current usage triggers the intervention overlay over the target app
- The overlay can accept text and show the intervention result
- The 15-minute cooldown suppresses immediate repeat interventions as designed
- Changing the configured limit resets the cooldown, allowing immediate retesting

This means the Saturday noon checkpoint is passed: the overlay has been observed above the selected target app on a physical phone after the user-defined limit was exceeded.

## Android Core hardening added after physical validation

- Monitoring enabled/disabled state is persisted so reopening Eşik does not automatically present monitoring as stopped
- Debug-only Logcat diagnostics identify the current monitor state without repeating identical messages continuously
- Poll failures are logged instead of silently swallowed
- Cooldown policy exposes remaining cooldown time for diagnostics while preserving the 15-minute product behavior
- Unit coverage includes cooldown remaining-time and non-positive-cooldown edge cases

## Still to validate on the demo phone

- Monitoring remains effective after leaving Eşik unused for several minutes
- Lock/unlock behavior does not surface an overlay over the lock screen
- Both intervention choices (`Yine de gir` and `Vazgeçtim`) are rechecked after the hardening changes
- Process/service restart behavior under battery optimization or OEM task killing
- Android notification-permission behavior on devices where foreground-service notifications are restricted
- A final physical-device pass after pulling the latest `work/android-core` commits

## Final verification commands

```bash
./gradlew test
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```
