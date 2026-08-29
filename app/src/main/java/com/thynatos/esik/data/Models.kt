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
)

enum class UserChoice(val storageValue: String) {
    CONTINUE("yine_de_gir"),
    STOPPED("vazgectim");

    companion object {
        fun fromStorage(value: String): UserChoice =
            entries.firstOrNull { it.storageValue == value } ?: CONTINUE
    }
}

data class InterventionRecord(
    val timestampEpochMillis: Long,
    val usageMinutes: Int,
    val text: String,
    val choice: UserChoice,
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
