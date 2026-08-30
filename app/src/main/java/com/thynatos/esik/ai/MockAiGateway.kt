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

        val focusTargets = buildList {
            if (combined.containsAny("ders", "çalışma", "derslere", "study")) {
                add("ders çalışmak")
            }
            if (combined.containsAny("ödev", "homework", "assignment")) {
                add("ödeve başlamak")
            }
            if (combined.containsAny("uyku", "uyumak", "sleep", "yatmadan")) {
                add("uykuya geçmek")
            }
            if (combined.containsAny("işi", "işe", "work")) {
                add("işe odaklanmak")
            }
        }.distinct().take(4)

        val goals = buildList {
            intake.improvementArea.trim().takeIf(String::isNotEmpty)?.let(::add)
            intake.reason.trim().takeIf(String::isNotEmpty)?.let(::add)
            if (isEmpty()) add("Telefonu daha bilinçli kullanmak")
        }.distinct().take(3)

        val contexts = buildList {
            if (combined.containsAny("yorgun", "yorul", "bitkin", "enerjim yok", "tired", "exhausted")) {
                add("yorgunluk")
            }
            if (
                combined.containsAny(
                    "ertel",
                    "procrast",
                    "başlayam",
                    "başlamak yerine",
                    "oyalan",
                    "odaklan",
                    "avoiding",
                )
            ) {
                add("erteleme")
            }
            if (combined.containsAny("motivasyon", "başlayasım", "canım istemiyor", "unmotivated")) {
                add("motivasyon düşüklüğü")
            }
            if (combined.containsAny("bunald", "nereden başlaya", "gözümde büyü", "çok fazla iş", "overwhelmed")) {
                add("bunalmışlık")
            }
            if (combined.containsAny("sıkıl", "bored", "boş kald")) add("sıkılma")
            if (combined.containsAny("rahatla", "dinlen", "kafa dağıt", "relax")) add("dinlenme")
            if (combined.containsAny("uyku", "gece", "uyuyam", "sleep", "bedtime")) {
                add("gece kullanımı")
            }
            if (isEmpty()) add("alışkanlıkla açma")
        }.distinct().take(4)

        val preferredActivities = intake.hobbies
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .take(5)

        val lowEnergyActivities = buildList {
            preferredActivities.firstOrNull(::isExerciseActivity)?.let {
                add("iki dakika hafifçe esnemek")
            }
            preferredActivities.firstOrNull { !isExerciseActivity(it) }?.let { hobby ->
                add(lowEnergyVersion(hobby))
            }
            add("bir bardak su içip ekrandan uzaklaşmak")
            add("bir şarkı boyunca telefonu bırakmak")
        }.distinct().take(3)

        val personalizedStates = contexts.mapNotNull(::stateForContext)
        val quickStates = (personalizedStates + PersonalizationDefaults.quickStates)
            .distinctBy(QuickStateOption::id)
            .take(6)

        return PersonalizationProfile(
            profileSummary = ProfileGroundingSanitizer.buildLocalSummary(
                focusTargets = focusTargets,
                goals = goals,
                contexts = contexts,
                activities = preferredActivities,
            ),
            focusTargets = focusTargets,
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
        val policy = InterventionContextBuilder.build(profile, input)

        val question = when (policy.objective) {
            InterventionObjective.PAUSE_AND_RECOVER ->
                "Şu anda kısa bir dinlenme mi, yoksa otomatik bir kaydırma mı arıyorsun?"
            InterventionObjective.MICRO_START ->
                "Ertelediğin şeyin yalnızca ilk iki dakikasını yapmak daha ulaşılabilir olabilir mi?"
            InterventionObjective.MAKE_BREAK_INTENTIONAL ->
                "Bu molayı ne kadar sürdürmek istediğini baştan seçmek işine yarar mı?"
            InterventionObjective.CHANGE_STIMULUS ->
                "Can sıkıntısını değiştirmek için küçük ve belirli bir seçenek denemek ister misin?"
            InterventionObjective.USE_WAIT_BRIEFLY ->
                "Beklerken ekran dışında kısa bir seçenek denemek ister misin?"
            InterventionObjective.WIND_DOWN ->
                "Uyumadan önce ekrandan kısa bir süre uzaklaşmak sana iyi gelebilir mi?"
            InterventionObjective.CLARIFY_INTENTION ->
                "Bu açılışın bilinçli bir seçim mi, yoksa alışkanlık mı olduğunu ayırmak ister misin?"
            InterventionObjective.CLARIFY_NEED ->
                "Şu anda gerçekten neye ihtiyaç duyduğunu bir cümleyle ayırmak yardımcı olabilir mi?"
        }

        val alternative = when (policy.objective) {
            InterventionObjective.PAUSE_AND_RECOVER,
            InterventionObjective.WIND_DOWN,
            -> lowEnergyAlternative(policy)

            InterventionObjective.MICRO_START -> policy.anchors.goals.firstOrNull()
                ?.let { goal ->
                    "$goal için yalnızca ilk iki dakikalık adımı başlatmayı deneyebilirsin."
                }
                ?: "Ertelediğin işin yalnızca ilk iki dakikasını yapmayı deneyebilirsin."

            InterventionObjective.MAKE_BREAK_INTENTIONAL ->
                "Bunu bilinçli bir mola olarak seçiyorsan 10 dakikalık bir zamanlayıcı kurup sonra yeniden karar verebilirsin."

            InterventionObjective.CHANGE_STIMULUS -> policy.anchors.activities.firstOrNull()
                ?.let { activity ->
                    "$activity için iki dakikalık küçük bir başlangıç yapmayı deneyebilirsin."
                }
                ?: "İki dakika boyunca bulunduğun ortamı değiştirip sonra yeniden karar verebilirsin."

            InterventionObjective.USE_WAIT_BRIEFLY -> policy.anchors.activities.firstOrNull()
                ?.let { activity ->
                    "Beklerken $activity için iki dakikalık küçük bir adım deneyebilirsin."
                }
                ?: "Beklerken iki dakika boyunca bulunduğun ortamı gözlemleyebilirsin."

            InterventionObjective.CLARIFY_INTENTION ->
                "Telefonu iki dakika masaya bırakıp ne için açtığını netleştirdikten sonra yeniden karar verebilirsin."

            InterventionObjective.CLARIFY_NEED ->
                "İki dakika boyunca telefonu bırakıp kısa bir ekran molası vermeyi deneyebilirsin."
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

        val evidence = DailyReportEvidenceBuilder.build(records)
        val selectedStateId = evidence.higherContinueStateId ?: evidence.dominantStateId
        val selectedState = evidence.states.firstOrNull { it.stateId == selectedStateId }
        val observation = selectedState?.stateLabel
            ?.takeIf(String::isNotBlank)
            ?.takeIf { SafetyLanguageValidator.isDisplaySafe(it) }
            ?.let { label ->
                "“$label” dediğin anlarda verdiğin kararlar arasında bir örüntü olabilir mi?"
            }
            ?: "Bugünkü farklı Eşik anlarından hangisi daha bilinçli bir seçim gibi hissettirdi?"

        val safeGoal = profile.personalization.goals.firstOrNull {
            SafetyLanguageValidator.isDisplaySafe(it)
        }
        val microStep = when (selectedStateId) {
            "procrastinating" -> safeGoal
                ?.let { goal ->
                    "Yarın ilk erteleme anında $goal için yalnızca iki dakika başla."
                }
                ?: "Yarın ilk erteleme anında işi iki dakika açıp sonra yeniden karar ver."
            "tired", "late_night" ->
                "Yarın ilk yorgunluk anında telefonu iki dakika uzağa bırakıp dinlenmeyi dene."
            "relaxing" ->
                "Yarın ilk molada beş dakikalık bir zamanlayıcı kurup sonra yeniden karar ver."
            else -> safeGoal
                ?.let { goal ->
                    "Yarın $goal için telefonu açmadan önce iki dakikalık tek bir başlangıç yap."
                }
                ?: "Yarın ilk müdahalede telefonu iki dakika uzağa bırakıp sonra yeniden karar ver."
        }

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

    private fun lowEnergyAlternative(policy: InterventionPolicy): String {
        val anchor = policy.anchors.lowEnergyActivities.firstOrNull()
            ?: policy.anchors.activities.firstOrNull()
        return when {
            anchor == null ->
                "Bir şarkı boyunca telefonu bırakıp gözlerini dinlendirmeyi deneyebilirsin."
            anchor.containsAny("müzik", "şarkı", "music", "song") ->
                "Bir şarkı boyunca telefonu bırakıp yalnızca müzik dinlemeyi deneyebilirsin."
            anchor.containsAny("su", "water") ->
                "Bir bardak su içip iki dakika ekrandan uzaklaşmayı deneyebilirsin."
            anchor.containsAny("yürüyüş", "walk") ->
                "İki dakikalık yavaş bir yürüyüş yapıp sonra yeniden karar verebilirsin."
            anchor.containsAny("esneme", "esnemek", "stretch") ->
                "İki dakika hafifçe esneyip sonra yeniden karar verebilirsin."
            else ->
                "$anchor için iki dakika ayırmayı deneyebilirsin."
        }
    }

    private fun lowEnergyVersion(hobby: String): String = when {
        hobby.containsAny("müzik", "şarkı", "music", "song") -> "bir şarkı dinlemek"
        hobby.containsAny("kitap", "oku", "book", "read") -> "iki sayfa okumak"
        hobby.containsAny("gitar", "guitar") -> "iki dakika gitar çalmak"
        isExerciseActivity(hobby) -> "iki dakika hafifçe esnemek"
        else -> "$hobby için iki dakika ayırmak"
    }

    private fun isExerciseActivity(value: String): Boolean = value.containsAny(
        "koşu",
        "koşmak",
        "spor",
        "egzersiz",
        "run",
        "running",
        "exercise",
        "workout",
        "gym",
    )

    private fun stateForContext(context: String): QuickStateOption? = when (context) {
        "yorgunluk" -> QuickStateOption("tired", "Biraz yoruldum", "😴", "low_energy")
        "erteleme" -> QuickStateOption("procrastinating", "Bir şeyi erteliyorum", "🫠", "avoidance")
        "motivasyon düşüklüğü" -> QuickStateOption("low_motivation", "Motivasyonum düşük", "🪫", "activation")
        "bunalmışlık" -> QuickStateOption("overwhelmed", "Her şey bunaltıyor", "🌀", "activation")
        "dinlenme" -> QuickStateOption("relaxing", "Sadece kafa dağıtıyorum", "😌", "intentional_rest")
        "sıkılma" -> QuickStateOption("bored", "Biraz sıkıldım", "🥱", "boredom")
        "gece kullanımı" -> QuickStateOption("late_night", "Uyumadan önce bakıyorum", "🌙", "late_night")
        "alışkanlıkla açma" -> QuickStateOption("habit", "Alışkanlıkla açtım", "🔁", "habit")
        else -> null
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any { needle -> contains(needle, ignoreCase = true) }

    private companion object {
        const val REPORT_MINIMUM_RECORDS = 7
    }
}
