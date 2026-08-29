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
        const val CURRENT_SCHEMA_VERSION: Int = 2
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
) {
    fun localTime(zoneId: ZoneId = ZoneId.systemDefault()): String =
        TIME_FORMATTER.format(Instant.ofEpochMilli(timestampEpochMillis).atZone(zoneId))

    fun localDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(timestampEpochMillis).atZone(zoneId).toLocalDate()

    fun occursOn(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean = localDate(zoneId) == date

    companion object {
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

data class AiCard(
    val question: String,
    val alternative: String,
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
