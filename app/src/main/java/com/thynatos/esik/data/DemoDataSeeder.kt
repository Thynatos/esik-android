package com.thynatos.esik.data

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

object DemoDataSeeder {
    /**
     * Creates records across four local dates while keeping eight non-future records on the
     * current date. The records include quick-state and generated-card metadata so the daily AI
     * report can demonstrate adaptation instead of analysing empty placeholders.
     */
    fun records(now: LocalDateTime = LocalDateTime.now()): List<InterventionRecord> {
        val zone = ZoneId.systemDefault()
        val historical = listOf(
            HistoricalSample(
                3, 21, 40, 71,
                "uyumadan önce bakacaktım",
                "late_night",
                "Uyumadan önce bakıyorum",
                UserChoice.CONTINUE,
            ),
            HistoricalSample(
                2, 18, 20, 74,
                "dersten sonra kafamı dağıtıyorum",
                "tired",
                "Biraz yoruldum",
                UserChoice.CONTINUE,
            ),
            HistoricalSample(
                1, 22, 55, 79,
                "bildirim geldi ama şimdi bakmam gerekmiyor",
                "habit",
                "Alışkanlıkla açtım",
                UserChoice.STOPPED,
            ),
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
            TodaySample(63, "alışkanlıkla açtım", "habit", "Alışkanlıkla açtım", UserChoice.STOPPED),
            TodaySample(68, "bir mesaja bakacaktım", "waiting", "Bir şeyi bekliyorum", UserChoice.CONTINUE),
            TodaySample(72, "ara verirken açtım", "relaxing", "Sadece kafa dağıtıyorum", UserChoice.CONTINUE),
            TodaySample(77, "biraz oyalanmak istedim", "bored", "Biraz sıkıldım", UserChoice.STOPPED),
            TodaySample(83, "çalışmaya başlamayı erteliyorum", "procrastinating", "Bir şeyi erteliyorum", UserChoice.CONTINUE),
            TodaySample(89, "bugün yoruldum", "tired", "Biraz yoruldum", UserChoice.CONTINUE),
            TodaySample(96, "ödeve başlamadan önce kaçıyorum", "procrastinating", "Bir şeyi erteliyorum", UserChoice.STOPPED),
            TodaySample(104, "uyumadan önce biraz bakacaktım", "late_night", "Uyumadan önce bakıyorum", UserChoice.CONTINUE),
        )
        val startOfToday = now.toLocalDate().atStartOfDay()
        val elapsedSeconds = Duration.between(startOfToday, now).seconds.coerceAtLeast(0L)
        val today = todaySamples.mapIndexed { index, sample ->
            val fractionNumerator = index + 1L
            val fractionDenominator = todaySamples.size + 1L
            val secondsFromStart = elapsedSeconds * fractionNumerator / fractionDenominator
            sample.toRecord(startOfToday.plusSeconds(secondsFromStart), zone)
        }

        // Answered moments from earlier days, so the local strategy preference has something real
        // to work with during a demo. One approach was repeatedly reported as helpful in the tired
        // state and one was repeatedly reported as unhelpful; nothing else is implied by them.
        val answered = listOf(
            FeedbackSample(
                3, 19, 10, "sensory_break",
                "Gözlerini iki dakika kapatıp yavaşça nefes alabilirsin.",
                InterventionOutcome.HELPED,
            ),
            FeedbackSample(
                3, 22, 5, "environment_change",
                "Telefonu iki dakika başka bir odaya bırakabilirsin.",
                InterventionOutcome.DID_NOT_HELP,
            ),
            FeedbackSample(
                2, 20, 30, "sensory_break",
                "İki dakika boyunca omuzlarını gevşetip pencereye bakabilirsin.",
                InterventionOutcome.HELPED,
            ),
            FeedbackSample(
                2, 23, 10, "environment_change",
                "Telefonu iki dakika masanın öbür ucuna koyabilirsin.",
                InterventionOutcome.DID_NOT_HELP,
            ),
            FeedbackSample(
                1, 19, 45, "sensory_break",
                "Bir bardak su içip iki dakika ekrandan uzaklaşabilirsin.",
                InterventionOutcome.HELPED,
            ),
            FeedbackSample(
                1, 21, 20, "environment_change",
                "İki dakika boyunca bulunduğun odayı değiştirebilirsin.",
                InterventionOutcome.DID_NOT_HELP,
            ),
        ).map { sample ->
            val dateTime = now
                .minusDays(sample.daysAgo.toLong())
                .withHour(sample.hour)
                .withMinute(sample.minute)
                .withSecond(0)
                .withNano(0)
            InterventionRecord(
                timestampEpochMillis = dateTime.atZone(zone).toInstant().toEpochMilli(),
                usageMinutes = DEMO_ANSWERED_USAGE_MINUTES,
                text = "bugün yoruldum",
                choice = UserChoice.STOPPED,
                stateId = "tired",
                stateLabel = "Biraz yoruldum",
                inputMethod = InterventionInputMethod.QUICK_REPLY,
                aiQuestion = questionFor("tired"),
                aiAlternative = sample.alternative,
                strategyId = sample.strategyId,
                outcome = sample.outcome,
                outcomeAtMillis = dateTime
                    .plusMinutes(DEMO_OUTCOME_DELAY_MINUTES)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli(),
            )
        }

        return (historical + answered + today).sortedBy { it.timestampEpochMillis }
    }

    private fun HistoricalSample.toRecord(
        dateTime: LocalDateTime,
        zone: ZoneId,
    ): InterventionRecord = enrichedRecord(
        dateTime = dateTime,
        zone = zone,
        usageMinutes = usageMinutes,
        text = text,
        stateId = stateId,
        stateLabel = stateLabel,
        choice = choice,
    )

    private fun TodaySample.toRecord(
        dateTime: LocalDateTime,
        zone: ZoneId,
    ): InterventionRecord = enrichedRecord(
        dateTime = dateTime,
        zone = zone,
        usageMinutes = usageMinutes,
        text = text,
        stateId = stateId,
        stateLabel = stateLabel,
        choice = choice,
    )

    private fun enrichedRecord(
        dateTime: LocalDateTime,
        zone: ZoneId,
        usageMinutes: Int,
        text: String,
        stateId: String,
        stateLabel: String,
        choice: UserChoice,
    ): InterventionRecord = InterventionRecord(
        timestampEpochMillis = dateTime.atZone(zone).toInstant().toEpochMilli(),
        usageMinutes = usageMinutes,
        text = text,
        choice = choice,
        stateId = stateId,
        stateLabel = stateLabel,
        inputMethod = InterventionInputMethod.QUICK_REPLY,
        aiQuestion = questionFor(stateId),
        aiAlternative = alternativeFor(stateId),
        strategyId = strategyFor(stateId),
    )

    private fun strategyFor(stateId: String): String = when (stateId) {
        "tired", "late_night" -> "low_energy_reset"
        "procrastinating" -> "micro_start"
        "relaxing" -> "timed_intentional_use"
        "bored", "waiting" -> "brief_activity"
        "habit" -> "environment_change"
        else -> "sensory_break"
    }

    private fun questionFor(stateId: String): String = when (stateId) {
        "tired" -> "Şu anda kısa bir dinlenme mi, yoksa otomatik bir kaydırma mı arıyorsun?"
        "procrastinating" -> "Ertelediğin şeyin yalnızca ilk iki dakikasını yapmak daha ulaşılabilir olabilir mi?"
        "relaxing" -> "Bu molayı ne kadar sürdürmek istediğini baştan seçmek işine yarar mı?"
        else -> "Bu açılışın bilinçli bir seçim olup olmadığını ayırmak yardımcı olabilir mi?"
    }

    private fun alternativeFor(stateId: String): String = when (stateId) {
        "tired" -> "Bir şarkı boyunca telefonu bırakıp gözlerini dinlendirebilirsin."
        "procrastinating" -> "Ertelediğin işin yalnızca ilk iki dakikasını yapabilirsin."
        "relaxing" -> "10 dakikalık bir zamanlayıcı kurup sonra yeniden karar verebilirsin."
        else -> "İki dakika boyunca telefonu bırakıp sonra yeniden karar verebilirsin."
    }

    private data class HistoricalSample(
        val daysAgo: Int,
        val hour: Int,
        val minute: Int,
        val usageMinutes: Int,
        val text: String,
        val stateId: String,
        val stateLabel: String,
        val choice: UserChoice,
    )

    private data class TodaySample(
        val usageMinutes: Int,
        val text: String,
        val stateId: String,
        val stateLabel: String,
        val choice: UserChoice,
    )

    private data class FeedbackSample(
        val daysAgo: Int,
        val hour: Int,
        val minute: Int,
        val strategyId: String,
        val alternative: String,
        val outcome: InterventionOutcome,
    )

    private const val DEMO_ANSWERED_USAGE_MINUTES = 76
    private const val DEMO_OUTCOME_DELAY_MINUTES = 12L
}
