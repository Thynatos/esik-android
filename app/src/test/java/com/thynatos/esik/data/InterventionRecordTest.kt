package com.thynatos.esik.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterventionRecordTest {
    @Test
    fun groupsRecordsUsingTheSuppliedLocalZone() {
        val record = InterventionRecord(
            timestampEpochMillis = 0L,
            usageMinutes = 60,
            text = "örnek",
            choice = UserChoice.CONTINUE,
        )

        assertTrue(record.occursOn(LocalDate.of(1970, 1, 1), ZoneId.of("UTC")))
        assertFalse(record.occursOn(LocalDate.of(1970, 1, 2), ZoneId.of("UTC")))
    }

    @Test
    fun asksAboutARecentUnansweredAlternativeTheUserChoseToTry() {
        val record = tryingRecord()

        assertTrue(record.awaitsOutcomeFeedback(NOW + 30 * MINUTE))
    }

    @Test
    fun neverAsksTwiceAboutTheSameMoment() {
        val answered = tryingRecord().copy(
            outcome = InterventionOutcome.HELPED,
            outcomeAtMillis = NOW + 10 * MINUTE,
        )

        assertFalse(answered.awaitsOutcomeFeedback(NOW + 30 * MINUTE))
    }

    @Test
    fun doesNotAskAboutAMomentTheUserDeliberatelyContinued() {
        val continued = tryingRecord().copy(choice = UserChoice.CONTINUE)

        assertFalse(continued.awaitsOutcomeFeedback(NOW + 30 * MINUTE))
    }

    @Test
    fun stopsAskingOnceTheMomentIsTooOldToRemember() {
        val record = tryingRecord()

        assertFalse(
            record.awaitsOutcomeFeedback(
                NOW + InterventionRecord.OUTCOME_FEEDBACK_WINDOW_MILLIS,
            ),
        )
    }

    @Test
    fun doesNotAskWhenThereWasNoAlternativeToTry() {
        val withoutAlternative = tryingRecord().copy(aiAlternative = "")

        assertFalse(withoutAlternative.awaitsOutcomeFeedback(NOW + 30 * MINUTE))
    }

    private fun tryingRecord(): InterventionRecord = InterventionRecord(
        timestampEpochMillis = NOW,
        usageMinutes = 78,
        text = "bugün yoruldum",
        choice = UserChoice.STOPPED,
        stateId = "tired",
        stateLabel = "Biraz yoruldum",
        aiAlternative = "Bir şarkı boyunca telefonu bırakabilirsin.",
        strategyId = "sensory_break",
    )

    private companion object {
        const val NOW = 1_756_000_000_000L
        const val MINUTE = 60_000L
    }
}
