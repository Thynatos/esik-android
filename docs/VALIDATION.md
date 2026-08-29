# Starter Validation Record

This file separates checks performed on the generated starter from work that still requires a normal Android development machine or physical phone.

## Passed in the generation environment

- Bash syntax for the wrapper, repository-creation, and pure-Kotlin verification scripts
- XML parsing for the manifest and all resources
- TOML parsing for the version catalog
- JSON parsing for every JSON contract/example in the Markdown documentation
- Secret-pattern scan: no Anthropic key or private-key material is present
- Pure Kotlin compilation and execution; all 13 repository test methods also compiled and passed through a lightweight local runner, covering:
  - crisis-language detection
  - ordinary-text non-match
  - blocked generated-language checks
  - word-fragment safety (`küçük` must not be mistaken for `çok`)
  - first-show, pre-boundary, boundary, and clock-rollback cooldown behavior
  - four-day demo seeding with eight records on the current date
  - report unavailable below seven records and available at seven
  - display-safe deterministic card/report fallback output

Run the same lightweight logic check on macOS/Linux with:

```bash
./scripts/verify-pure-kotlin.sh
```

## Not run in the generation environment

- A full Gradle sync, `test`, or `assembleDebug`
- Android lint or instrumentation tests
- Usage Access behavior
- foreground-package detection
- foreground-service startup and persistence
- overlay input/focus behavior
- cooldown behavior across a real device clock change/reboot
- lock-screen, battery-optimization, or OEM-specific behavior

The environment used to generate this starter did not include an Android SDK or a Gradle dependency cache and could not safely materialize the Gradle Wrapper binary. Generate the checksum-verified standard wrapper with the included bootstrap script, then run:

```bash
./gradlew test
./gradlew assembleDebug
```

The Saturday noon checkpoint is not passed until the overlay is observed above the selected target app on the actual demo phone after the user-defined limit is reached.
