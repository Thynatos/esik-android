package com.thynatos.esik.data

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

object DemoDataSeeder {
    /**
     * Creates records across four local dates while keeping eight non-future records on the
     * current date. This lets the daily-report path be demonstrated without losing multi-day
     * seed data.
     */
    fun records(now: LocalDateTime = LocalDateTime.now()): List<InterventionRecord> {
        val zone = ZoneId.systemDefault()
        val historical = listOf(
            HistoricalSample(3, 21, 40, 71, "uyumadan önce bakacaktım", UserChoice.CONTINUE),
            HistoricalSample(2, 18, 20, 74, "dersten sonra kafamı dağıtıyorum", UserChoice.CONTINUE),
            HistoricalSample(1, 22, 55, 79, "bildirim geldi", UserChoice.STOPPED),
        ).map { sample ->
            val dateTime = now
                .minusDays(sample.daysAgo.toLong())
                .withHour(sample.hour)
                .withMinute(sample.minute)
                .withSecond(0)
                .withNano(0)
            sample.toRecord(dateTime, zone)
        }

        val todaySamples = listOf(
            TodaySample(63, "alışkanlıkla açtım", UserChoice.STOPPED),
            TodaySample(68, "bir mesaja bakacaktım", UserChoice.CONTINUE),
            TodaySample(72, "ara verirken açtım", UserChoice.CONTINUE),
            TodaySample(77, "biraz oyalanmak istedim", UserChoice.STOPPED),
            TodaySample(83, "ne yapacağımı düşünmeden açtım", UserChoice.CONTINUE),
            TodaySample(89, "bugün yoruldum", UserChoice.CONTINUE),
            TodaySample(96, "arkadaşımın paylaşımına bakacaktım", UserChoice.STOPPED),
            TodaySample(104, "uyumadan önce biraz bakacaktım", UserChoice.CONTINUE),
        )
        val startOfToday = now.toLocalDate().atStartOfDay()
        val elapsedSeconds = Duration.between(startOfToday, now).seconds.coerceAtLeast(0L)
        val today = todaySamples.mapIndexed { index, sample ->
            val fractionNumerator = index + 1L
            val fractionDenominator = todaySamples.size + 1L
            val secondsFromStart = elapsedSeconds * fractionNumerator / fractionDenominator
            sample.toRecord(startOfToday.plusSeconds(secondsFromStart), zone)
        }

        return (historical + today).sortedBy { it.timestampEpochMillis }
    }

    private fun HistoricalSample.toRecord(
        dateTime: LocalDateTime,
        zone: ZoneId,
    ): InterventionRecord = InterventionRecord(
        timestampEpochMillis = dateTime.atZone(zone).toInstant().toEpochMilli(),
        usageMinutes = usageMinutes,
        text = text,
        choice = choice,
    )

    private fun TodaySample.toRecord(
        dateTime: LocalDateTime,
        zone: ZoneId,
    ): InterventionRecord = InterventionRecord(
        timestampEpochMillis = dateTime.atZone(zone).toInstant().toEpochMilli(),
        usageMinutes = usageMinutes,
        text = text,
        choice = choice,
    )

    private data class HistoricalSample(
        val daysAgo: Int,
        val hour: Int,
        val minute: Int,
        val usageMinutes: Int,
        val text: String,
        val choice: UserChoice,
    )

    private data class TodaySample(
        val usageMinutes: Int,
        val text: String,
        val choice: UserChoice,
    )
}
