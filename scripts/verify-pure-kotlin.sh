#!/usr/bin/env bash
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(CDPATH= cd -- "${script_dir}/.." && pwd)"

command -v kotlinc >/dev/null 2>&1 || {
  echo "kotlinc is required for this lightweight check. Use ./gradlew test otherwise." >&2
  exit 1
}
command -v java >/dev/null 2>&1 || {
  echo "java is required." >&2
  exit 1
}

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

cat > "${work_dir}/Verify.kt" <<'KOTLIN'
import com.thynatos.esik.ai.CrisisFilter
import com.thynatos.esik.ai.MockAiGateway
import com.thynatos.esik.ai.SafetyLanguageValidator
import com.thynatos.esik.data.DemoDataSeeder
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.usage.CooldownPolicy
import java.time.LocalDate
import java.time.LocalDateTime

fun main() {
    check(CrisisFilter.check("Kendimi öldürmek istiyorum.").isCrisisSignal)
    check(!CrisisFilter.check("Bugün yoruldum, biraz dinleneceğim.").isCrisisSignal)

    check(!SafetyLanguageValidator.isDisplaySafe("Bugün çok kullandın."))
    check(SafetyLanguageValidator.isDisplaySafe("İki dakikalık küçük bir başlangıç yapabilirsin."))

    check(CooldownPolicy.shouldShow(1_000L, null))
    check(!CooldownPolicy.shouldShow(CooldownPolicy.DEFAULT_COOLDOWN_MILLIS - 1L, 0L))
    check(CooldownPolicy.shouldShow(CooldownPolicy.DEFAULT_COOLDOWN_MILLIS, 0L))
    check(CooldownPolicy.shouldShow(500L, 1_000L))

    val fixedNow = LocalDateTime.of(2026, 8, 29, 12, 0)
    val records = DemoDataSeeder.records(fixedNow)
    check(records.size == 11)
    check(records.map { it.localDate() }.distinct().size == 4)
    check(records.count { it.occursOn(LocalDate.of(2026, 8, 29)) } == 8)
    check(records.all { it.timestampEpochMillis <= fixedNow.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() })

    val profile = UserProfile(
        name = "Ayşe",
        department = "İstatistik",
        hobbies = listOf("gitar"),
        improvementArea = "İngilizce",
        reason = "gece uyuyamıyorum",
        targetAppLabel = "Instagram",
        targetPackage = "com.instagram.android",
        dailyLimitMinutes = 60,
    )
    val gateway = MockAiGateway()
    check(gateway.generateDailyReport(profile, records.take(6), 78).insufficientData)
    val report = gateway.generateDailyReport(profile, records.take(7), 78)
    check(!report.insufficientData)
    check(SafetyLanguageValidator.isDisplaySafe(report.observationQuestion, report.microStep))

    val card = gateway.generateCard(profile, 78, "bugün yoruldum")
    check(SafetyLanguageValidator.isDisplaySafe(card.question, card.alternative))

    println("Pure Kotlin verification passed.")
}
KOTLIN

cd "$repo_root"
kotlinc \
  app/src/main/java/com/thynatos/esik/data/Models.kt \
  app/src/main/java/com/thynatos/esik/data/DemoDataSeeder.kt \
  app/src/main/java/com/thynatos/esik/ai/AiGateway.kt \
  app/src/main/java/com/thynatos/esik/ai/MockAiGateway.kt \
  app/src/main/java/com/thynatos/esik/ai/CrisisFilter.kt \
  app/src/main/java/com/thynatos/esik/ai/SafetyLanguageValidator.kt \
  app/src/main/java/com/thynatos/esik/usage/CooldownPolicy.kt \
  "${work_dir}/Verify.kt" \
  -include-runtime \
  -d "${work_dir}/verify.jar"
java -jar "${work_dir}/verify.jar"
