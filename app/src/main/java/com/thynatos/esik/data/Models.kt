package com.thynatos.esik.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class UserProfile(
    val name: String,
    val department: String,
    val hobbies: List<String>,
    val improvementArea: String,
    val reason: String,
    val targetAppLabel: String,
    val targetPackage: String,
    val dailyLimitMinutes: Int,
    val biography: String = "",
    val personalization: PersonalizationProfile = PersonalizationProfile(),
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 3
    }
}

data class ProfileIntake(
    val name: String,
    val department: String = "",
    val biography: String,
    val hobbies: List<String> = emptyList(),
    val improvementArea: String = "",
    val reason: String = "",
)

data class PersonalizationProfile(
    val goals: List<String> = emptyList(),
    val recurringContexts: List<String> = emptyList(),
    val preferredActivities: List<String> = emptyList(),
    val lowEnergyActivities: List<String> = emptyList(),
    val quickStates: List<QuickStateOption> = emptyList(),
    val tone: ProfileTone = ProfileTone.SUPPORTIVE_DIRECT,
) {
    fun quickStatesOrDefault(): List<QuickStateOption> =
        quickStates
            .filter { it.id.isNotBlank() && it.label.isNotBlank() }
            .distinctBy { it.id }
            .takeIf { it.size >= 3 }
            ?: PersonalizationDefaults.quickStates
}

data class QuickStateOption(
    val id: String,
    val label: String,
    val emoji: String = "",
    val category: String = id,
)

enum class ProfileTone(val storageValue: String) {
    SUPPORTIVE_DIRECT("supportive_direct"),
    GENTLE("gentle"),
    PRACTICAL("practical");

    companion object {
        fun fromStorage(value: String): ProfileTone =
            entries.firstOrNull { it.storageValue == value } ?: SUPPORTIVE_DIRECT
    }
}

object PersonalizationDefaults {
    val quickStates: List<QuickStateOption> = listOf(
        QuickStateOption("tired", "Biraz yoruldum", "😴", "low_energy"),
        QuickStateOption("procrastinating", "Bir şeyi erteliyorum", "🫠", "avoidance"),
        QuickStateOption("relaxing", "Sadece kafa dağıtıyorum", "😌", "intentional_rest"),
        QuickStateOption("bored", "Biraz sıkıldım", "🥱", "boredom"),
        QuickStateOption("habit", "Alışkanlıkla açtım", "🔁", "habit"),
        QuickStateOption("waiting", "Bir şeyi bekliyorum", "⏳", "waiting"),
    )
}

enum class UserChoice(val storageValue: String) {
    CONTINUE("yine_de_gir"),
    STOPPED("vazgectim");

    companion object {
        fun fromStorage(value: String): UserChoice =
            entries.firstOrNull { it.storageValue == value } ?: CONTINUE
    }
}

enum class InterventionInputMethod(val storageValue: String) {
    QUICK_REPLY("quick_reply"),
    TEXT("text"),
    VOICE("voice");

    companion object {
        fun fromStorage(value: String): InterventionInputMethod =
            entries.firstOrNull { it.storageValue == value } ?: TEXT
    }
}

/**
 * What the user reported about the alternative they said they would try.
 *
 * This is the user's own read of a single moment, not a success metric. It never becomes a score,
 * a streak, or a judgement, and a missing answer stays [UNKNOWN] instead of being inferred.
 */
enum class InterventionOutcome(val storageValue: String) {
    UNKNOWN("bilinmiyor"),
    HELPED("yardimci_oldu"),
    DID_NOT_HELP("yardimci_olmadi"),
    NOT_TRIED("denenmedi");

    val isReported: Boolean
        get() = this != UNKNOWN

    /** Only an explicit tried/not-tried answer says anything about the strategy itself. */
    val countsAsAttempt: Boolean
        get() = this == HELPED || this == DID_NOT_HELP

    companion object {
        fun fromStorage(value: String): InterventionOutcome =
            entries.firstOrNull { it.storageValue == value } ?: UNKNOWN
    }
}

data class InterventionInput(
    val text: String,
    val stateId: String = "",
    val stateLabel: String = "",
    val method: InterventionInputMethod = InterventionInputMethod.TEXT,
)

data class InterventionRecord(
    val timestampEpochMillis: Long,
    val usageMinutes: Int,
    val text: String,
    val choice: UserChoice,
    val stateId: String = "",
    val stateLabel: String = "",
    val inputMethod: InterventionInputMethod = InterventionInputMethod.TEXT,
    val aiQuestion: String = "",
    val aiAlternative: String = "",
    val strategyId: String = "",
    val outcome: InterventionOutcome = InterventionOutcome.UNKNOWN,
    val outcomeAtMillis: Long? = null,
) {
    fun localTime(zoneId: ZoneId = ZoneId.systemDefault()): String =
        TIME_FORMATTER.format(Instant.ofEpochMilli(timestampEpochMillis).atZone(zoneId))

    fun localDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(timestampEpochMillis).atZone(zoneId).toLocalDate()

    fun occursOn(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean = localDate(zoneId) == date

    /**
     * Whether it is still meaningful to ask the user how the alternative went.
     *
     * Asking is limited to moments where the user said they would try something, has not answered
     * yet, and the moment is recent enough to remember. Nothing is asked twice.
     */
    fun awaitsOutcomeFeedback(
        nowMillis: Long,
        windowMillis: Long = OUTCOME_FEEDBACK_WINDOW_MILLIS,
    ): Boolean = choice == UserChoice.STOPPED &&
        outcome == InterventionOutcome.UNKNOWN &&
        aiAlternative.isNotBlank() &&
        nowMillis - timestampEpochMillis in 0 until windowMillis

    companion object {
        const val OUTCOME_FEEDBACK_WINDOW_MILLIS: Long = 6L * 60L * 60L * 1_000L

        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

/** Where a displayed card came from, so the app can explain itself to the user. */
enum class AiCardSource(val storageValue: String) {
    LIVE("live"),
    REPAIRED("repaired"),
    LOCAL_FALLBACK("local_fallback");

    companion object {
        fun fromStorage(value: String): AiCardSource =
            entries.firstOrNull { it.storageValue == value } ?: LOCAL_FALLBACK
    }
}

/**
 * The locally compiled constraints a card was produced under.
 *
 * This exists so the product can show the user what the model was and was not allowed to do. It
 * carries no user text: only identifiers the application itself decided.
 */
data class CardProvenance(
    val source: AiCardSource = AiCardSource.LOCAL_FALLBACK,
    val stateId: String = "",
    val needId: String = "",
    val strategyId: String = "",
    val durationMinutes: Int = 0,
    val maxDurationMinutes: Int = 0,
    val anchor: String = "",
    val learnedPreferenceApplied: Boolean = false,
)

data class AiCard(
    val question: String,
    val alternative: String,
    val provenance: CardProvenance = CardProvenance(),
)

data class DailyReport(
    val totalUsageMinutes: Int,
    val limitMinutes: Int,
    val interventionCount: Int,
    val continuedCount: Int,
    val stoppedCount: Int,
    val observationQuestion: String,
    val microStep: String,
    val insufficientData: Boolean = false,
)
