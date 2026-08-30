package com.thynatos.esik.ai

import com.thynatos.esik.data.InterventionOutcome
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.UserChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyEffectivenessTest {
    @Test
    fun singleAnswerDoesNotChangeAnything() {
        val effectiveness = StrategyEffectivenessBuilder.build(
            records = listOf(answered("tired", "sensory_break", InterventionOutcome.HELPED)),
            stateId = "tired",
        )

        assertNull(effectiveness.preferred)
        assertTrue(effectiveness.discouraged.isEmpty())
    }

    @Test
    fun repeatedlyHelpfulStrategyBecomesPreferred() {
        val effectiveness = StrategyEffectivenessBuilder.build(
            records = repeat("tired", "sensory_break", InterventionOutcome.HELPED, times = 3),
            stateId = "tired",
        )

        assertEquals(InterventionStrategy.SENSORY_BREAK, effectiveness.preferred)
        assertTrue(effectiveness.discouraged.isEmpty())
    }

    @Test
    fun repeatedlyUnhelpfulStrategyBecomesDiscouraged() {
        val effectiveness = StrategyEffectivenessBuilder.build(
            records = repeat(
                stateId = "tired",
                strategyId = "environment_change",
                outcome = InterventionOutcome.DID_NOT_HELP,
                times = 3,
            ),
            stateId = "tired",
        )

        assertNull(effectiveness.preferred)
        assertTrue(
            effectiveness.discouraged.contains(InterventionStrategy.ENVIRONMENT_CHANGE),
        )
    }

    @Test
    fun narrowingNeverLeavesTheUserWithoutAnOption() {
        val allowed = setOf(InterventionStrategy.SENSORY_BREAK)
        val effectiveness = StrategyEffectivenessBuilder.build(
            records = repeat(
                stateId = "tired",
                strategyId = "sensory_break",
                outcome = InterventionOutcome.DID_NOT_HELP,
                times = 4,
            ),
            stateId = "tired",
        )

        assertTrue(effectiveness.discouraged.contains(InterventionStrategy.SENSORY_BREAK))
        assertEquals(allowed, effectiveness.narrow(allowed))
    }

    @Test
    fun onlyExplicitTriedAnswersCountAsEvidence() {
        val records = repeat("tired", "sensory_break", InterventionOutcome.NOT_TRIED, times = 5) +
            repeat("tired", "sensory_break", InterventionOutcome.UNKNOWN, times = 5)

        val effectiveness = StrategyEffectivenessBuilder.build(records, "tired")

        assertNull(effectiveness.preferred)
        assertTrue(effectiveness.discouraged.isEmpty())
        assertTrue(effectiveness.scores.isEmpty())
    }

    @Test
    fun evidenceFromOneStateDoesNotLeakIntoAnother() {
        val records = repeat("tired", "sensory_break", InterventionOutcome.HELPED, times = 4)

        val other = StrategyEffectivenessBuilder.build(records, "procrastinating")

        assertNull(other.preferred)
        assertTrue(other.scores.isEmpty())
    }

    @Test
    fun unknownStrategyIdentifiersAreIgnored() {
        val effectiveness = StrategyEffectivenessBuilder.build(
            records = repeat("tired", "telepathy", InterventionOutcome.HELPED, times = 5),
            stateId = "tired",
        )

        assertTrue(effectiveness.scores.isEmpty())
        assertNull(effectiveness.preferred)
    }

    @Test
    fun catchAllStrategyIsNeverPreferred() {
        val effectiveness = StrategyEffectivenessBuilder.build(
            records = repeat("habit", "other", InterventionOutcome.HELPED, times = 5),
            stateId = "habit",
        )

        assertNull(effectiveness.preferred)
        assertFalse(effectiveness.scores.isEmpty())
    }

    @Test
    fun mixedEvidenceRanksTheMoreHelpfulStrategyFirst() {
        val records = repeat("tired", "sensory_break", InterventionOutcome.HELPED, times = 3) +
            repeat("tired", "low_energy_reset", InterventionOutcome.HELPED, times = 1) +
            repeat("tired", "low_energy_reset", InterventionOutcome.DID_NOT_HELP, times = 2)

        val effectiveness = StrategyEffectivenessBuilder.build(records, "tired")

        assertEquals(InterventionStrategy.SENSORY_BREAK, effectiveness.preferred)
        assertEquals(
            InterventionStrategy.SENSORY_BREAK,
            effectiveness.scores.first().strategy,
        )
    }

    @Test
    fun preferenceIsDroppedWhenTheCurrentContextDisallowsIt() {
        val effectiveness = StrategyEffectivenessBuilder.build(
            records = repeat("tired", "sensory_break", InterventionOutcome.HELPED, times = 3),
            stateId = "tired",
        )

        assertNull(
            effectiveness.preferenceWithin(setOf(InterventionStrategy.MICRO_START)),
        )
        assertEquals(
            InterventionStrategy.SENSORY_BREAK,
            effectiveness.preferenceWithin(
                setOf(InterventionStrategy.SENSORY_BREAK, InterventionStrategy.MICRO_START),
            ),
        )
    }

    @Test
    fun blankStateIdProducesNoSignal() {
        val effectiveness = StrategyEffectivenessBuilder.build(
            records = repeat("tired", "sensory_break", InterventionOutcome.HELPED, times = 4),
            stateId = "  ",
        )

        assertEquals(StrategyEffectiveness.EMPTY, effectiveness)
    }

    private fun repeat(
        stateId: String,
        strategyId: String,
        outcome: InterventionOutcome,
        times: Int,
    ): List<InterventionRecord> = (1..times).map { index ->
        answered(stateId, strategyId, outcome, offsetMinutes = index.toLong())
    }

    private fun answered(
        stateId: String,
        strategyId: String,
        outcome: InterventionOutcome,
        offsetMinutes: Long = 0L,
    ): InterventionRecord = InterventionRecord(
        timestampEpochMillis = BASE_MILLIS + offsetMinutes * 60_000L,
        usageMinutes = 78,
        text = "demo",
        choice = UserChoice.STOPPED,
        stateId = stateId,
        stateLabel = stateId,
        aiAlternative = "iki dakikalık küçük bir adım",
        strategyId = strategyId,
        outcome = outcome,
        outcomeAtMillis = if (outcome.isReported) {
            BASE_MILLIS + offsetMinutes * 60_000L + 600_000L
        } else {
            null
        },
    )

    private companion object {
        const val BASE_MILLIS = 1_756_000_000_000L
    }
}
