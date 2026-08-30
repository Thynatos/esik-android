package com.thynatos.esik.ai

import com.thynatos.esik.usage.UsagePatternSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NeedInferenceTest {
    @Test
    fun ordinaryMomentProducesNoGuess() {
        assertNull(
            NeedInference.infer(
                pattern = pattern(currentSessionMillis = minutes(4)),
                hourOfDay = 14,
            ),
        )
    }

    @Test
    fun lateHourAloneIsNotEnough() {
        assertNull(
            NeedInference.infer(
                pattern = pattern(continuousActivityMillis = minutes(10)),
                hourOfDay = 1,
            ),
        )
    }

    @Test
    fun sustainedLateNightUseOffersLateNightHypothesis() {
        val hypothesis = NeedInference.infer(
            pattern = pattern(continuousActivityMillis = minutes(55)),
            hourOfDay = 1,
        )

        assertEquals(NeedInference.STATE_LATE_NIGHT, hypothesis?.stateId)
        assertTrue(hypothesis!!.reasons.contains("late_hour"))
        assertTrue(hypothesis.reasons.contains("long_continuous_use"))
    }

    @Test
    fun repeatedFastReopensOfferHabitHypothesis() {
        val hypothesis = NeedInference.infer(
            pattern = pattern(
                targetOpenCount = 4,
                lastGapMillis = 20_000L,
            ),
            hourOfDay = 15,
        )

        assertEquals(NeedInference.STATE_HABIT, hypothesis?.stateId)
        assertTrue(hypothesis!!.reasons.contains("reopened_within_seconds"))
        assertTrue(hypothesis.reasons.contains("several_opens_in_short_window"))
    }

    @Test
    fun repeatedVeryShortSessionsCanSupportHabitWithoutFastLastGap() {
        val hypothesis = NeedInference.infer(
            pattern = pattern(
                targetOpenCount = 3,
                medianSessionMillis = 30_000L,
                lastGapMillis = 2 * 60_000L,
            ),
            hourOfDay = 15,
        )

        assertEquals(NeedInference.STATE_HABIT, hypothesis?.stateId)
    }

    @Test
    fun longSessionDoesNotInferBoredomOrFatigue() {
        assertNull(
            NeedInference.infer(
                pattern = pattern(
                    currentSessionMillis = minutes(45),
                    continuousActivityMillis = minutes(40),
                ),
                hourOfDay = 15,
            ),
        )
    }

    private fun pattern(
        targetOpenCount: Int = 1,
        currentSessionMillis: Long = 0L,
        medianSessionMillis: Long = 0L,
        lastGapMillis: Long = UsagePatternSnapshot.UNKNOWN_GAP,
        continuousActivityMillis: Long = 0L,
    ): UsagePatternSnapshot = UsagePatternSnapshot(
        targetOpenCount = targetOpenCount,
        isTargetForeground = true,
        currentSessionMillis = currentSessionMillis,
        completedSessionCount = 0,
        medianSessionMillis = medianSessionMillis,
        lastGapMillis = lastGapMillis,
        previousPackage = "",
        continuousActivityMillis = continuousActivityMillis,
    )

    private fun minutes(value: Long): Long = value * 60L * 1_000L
}
