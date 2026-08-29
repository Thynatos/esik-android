package com.thynatos.esik.ai

import com.thynatos.esik.data.AiCard
import com.thynatos.esik.data.DailyReport
import com.thynatos.esik.data.InterventionInput
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.PersonalizationDefaults
import com.thynatos.esik.data.PersonalizationProfile
import com.thynatos.esik.data.ProfileIntake
import com.thynatos.esik.data.ProfileTone
import com.thynatos.esik.data.QuickStateOption
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile

class MockAiGateway : AiGateway {
    override suspend fun generateProfile(intake: ProfileIntake): PersonalizationProfile {
        val combined = listOf(
            intake.biography,
            intake.reason,
            intake.improvementArea,
            intake.hobbies.joinToString(" "),
        ).joinToString(" ").lowercase()

        val goals = buildList {
            intake.improvementArea.trim().takeIf(String::isNotEmpty)?.let(::add)
            intake.reason.trim().takeIf(String::isNotEmpty)?.let(::add)
            if (isEmpty()) add("Telefonu daha bilinçli kullanmak")
        }.distinct().take(3)

        val contexts = buildList {
            if (combined.containsAny("yorgun", "bitkin", "enerjim yok", "tired")) add("yorgunluk")
            if (combined.containsAny("ertel", "procrast", "başlayam", "odaklan")) add("erteleme")
            if (combined.containsAny("sıkıl", "bored", "boş kald")) add("sıkılma")
            if (combined.containsAny("rahatla", "dinlen", "kafa dağıt", "relax")) add("dinlenme")
            if (combined.containsAny("uyku", "gece", "uyuyam")) add("gece kullanımı")
            if (isEmpty()) add("alışkanlıkla açma")
        }.distinct().take(4)

        val preferredActivities = buildList {
            addAll(intake.hobbies.map(String::trim).filter(String::isNotEmpty))
            intake.improvementArea.trim().takeIf(String::isNotEmpty)?.let(::add)
            if (isEmpty()) addAll(listOf("kısa yürüyüş", "müzik", "iki dakikalık mola"))
        }.distinct().take(5)

        val lowEnergyActivities = buildList {
            preferredActivities.firstOrNull()?.let { add("$it için iki dakikalık başlangıç") }
            add("bir bardak su içip ekrandan uzaklaşmak")
            add("bir şarkı boyunca telefonu bırakmak")
        }.distinct().take(3)

        val personalizedStates = contexts.mapNotNull(::stateForContext)
        val quickStates = (personalizedStates + PersonalizationDefaults.quickStates)
            .distinctBy(QuickStateOption::id)
            .take(6)

        return PersonalizationProfile(
            goals = goals,
            recurringContexts = contexts,
            preferredActivities = preferredActivities,
            lowEnergyActivities = lowEnergyActivities,
            quickStates = quickStates,
            tone = ProfileTone.SUPPORTIVE_DIRECT,
        )
    }

    override suspend fun generateCard(
        profile: UserProfile,
        currentUsageMinutes: Int,
        input: InterventionInput,
    ): AiCard {
        val state = input.stateId.ifBlank { inferState(input.text) }
        val preferred = profile.personalization.preferredActivities
            .firstOrNull()
            ?: profile.hobbies.firstOrNull()
            ?: profile.improvementArea.takeIf(String::isNotBlank)

        val question = when (state) {
            "tired" -> "Şu anda kısa bir dinlenme mi, yoksa otomatik bir kaydırma mı arıyorsun?"
            "procrastinating" -> "Ertelediğin şeyin yalnızca ilk iki dakikasını yapmak daha ulaşılabilir olabilir mi?"
            "relaxing" -> "Bu molayı ne kadar sürdürmek istediğini baştan seçmek işine yarar mı?"
            "bored" -> "Can sıkıntısını değiştirmek için daha küçük ve belirli bir şey seçmek ister misin?"
            "waiting" -> "Beklerken ekran dışında kısa bir seçenek denemek ister misin?"
            "habit" -> "Bu açılış bilinçli bir seçimden ziyade alışkanlığa benziyor olabilir mi?"
            else -> "Şu anda gerçekten neye ihtiyaç duyduğunu bir cümleyle ayırmak yardımcı olabilir mi?"
        }

        val alternative = when (state) {
            "tired" -> profile.personalization.lowEnergyActivities.firstOrNull()
                ?.let { "$it için yalnızca iki dakika ayırabilirsin." }
                ?: "Bir şarkı boyunca telefonu bırakıp gözlerini dinlendirebilirsin."
            "procrastinating" -> profile.personalization.goals.firstOrNull()
                ?.let { "$it için yalnızca ilk iki dakikalık adımı başlatabilirsin." }
                ?: "Ertelediğin işin yalnızca ilk iki dakikasını yapabilirsin."
            "relaxing" -> "Bunu bilinçli bir mola olarak seçiyorsan 10 dakikalık bir zamanlayıcı kurup sonra yeniden karar verebilirsin."
            "waiting" -> preferred
                ?.let { "Beklerken $it için iki dakikalık küçük bir adım deneyebilirsin." }
                ?: "Beklerken iki dakika boyunca bulunduğun ortamı gözlemleyebilirsin."
            else -> preferred
                ?.let { "$it için iki dakikalık küçük bir başlangıç yapabilirsin." }
                ?: "İki dakika boyunca telefonu bırakıp kısa bir mola verebilirsin."
        }

        return AiCard(question = question, alternative = alternative)
    }

    override suspend fun generateDailyReport(
        profile: UserProfile,
        records: List<InterventionRecord>,
        currentUsageMinutes: Int,
    ): DailyReport {
        val continued = records.count { it.choice == UserChoice.CONTINUE }
        val stopped = records.count { it.choice == UserChoice.STOPPED }
        if (records.size < REPORT_MINIMUM_RECORDS) {
            return DailyReport(
                totalUsageMinutes = currentUsageMinutes,
                limitMinutes = profile.dailyLimitMinutes,
                interventionCount = records.size,
                continuedCount = continued,
                stoppedCount = stopped,
                observationQuestion = "Yeterli veri yok.",
                microStep = "Yarın yalnızca bir kaydı tamamlamayı dene.",
                insufficientData = true,
            )
        }

        val commonState = records
            .mapNotNull { it.stateLabel.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.takeIf { SafetyLanguageValidator.isDisplaySafe(it) }

        val observation = commonState?.let {
            "“$it” seçtiğin anlarda verdiğin kararlar arasında bir örüntü olabilir mi?"
        } ?: "Akşam saatlerindeki girişler yorgunluk veya alışkanlıkla bağlantılı olabilir mi?"

        val safeGoal = profile.personalization.goals.firstOrNull()
            ?.takeIf { SafetyLanguageValidator.isDisplaySafe(it) }
        val microStep = safeGoal?.let {
            "Yarın $it için telefonu açmadan önce iki dakikalık tek bir başlangıç yap."
        } ?: "Yarın ilk müdahalede telefonu iki dakika uzağa bırakıp sonra yeniden karar ver."

        return DailyReport(
            totalUsageMinutes = currentUsageMinutes,
            limitMinutes = profile.dailyLimitMinutes,
            interventionCount = records.size,
            continuedCount = continued,
            stoppedCount = stopped,
            observationQuestion = observation,
            microStep = microStep,
        )
    }

    private fun inferState(text: String): String {
        val normalized = text.lowercase()
        return when {
            normalized.containsAny("yorgun", "bitkin", "tired") -> "tired"
            normalized.containsAny("ertel", "başlayam", "procrast") -> "procrastinating"
            normalized.containsAny("dinlen", "rahatla", "kafa dağıt") -> "relaxing"
            normalized.containsAny("sıkıl", "bored") -> "bored"
            normalized.containsAny("bekli", "waiting") -> "waiting"
            else -> "habit"
        }
    }

    private fun stateForContext(context: String): QuickStateOption? = when (context) {
        "yorgunluk" -> QuickStateOption("tired", "Biraz yoruldum", "😴", "low_energy")
        "erteleme" -> QuickStateOption("procrastinating", "Bir şeyi erteliyorum", "🫠", "avoidance")
        "dinlenme" -> QuickStateOption("relaxing", "Sadece kafa dağıtıyorum", "😌", "intentional_rest")
        "sıkılma" -> QuickStateOption("bored", "Biraz sıkıldım", "🥱", "boredom")
        "gece kullanımı" -> QuickStateOption("late_night", "Uyumadan önce bakıyorum", "🌙", "late_night")
        "alışkanlıkla açma" -> QuickStateOption("habit", "Alışkanlıkla açtım", "🔁", "habit")
        else -> null
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any(::contains)

    private companion object {
        const val REPORT_MINIMUM_RECORDS = 7
    }
}
