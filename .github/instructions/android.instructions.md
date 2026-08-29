---
applyTo: "app/src/main/**/*.kt,app/src/main/AndroidManifest.xml,app/build.gradle.kts"
---

Use Android platform APIs conservatively and verify behavior against the project target SDK. Keep foreground-service startup user-initiated. `SYSTEM_ALERT_WINDOW` and Usage Access are Settings-granted special permissions, not ordinary runtime permissions. Do not use Accessibility Service as a shortcut for this prototype.

Any background/overlay change must include a physical-device validation note covering: permission denied, service stopped, target app not installed, screen locked, midnight boundary, overlay already visible, and cooldown not expired.
