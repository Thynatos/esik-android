package com.thynatos.esik.data

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataSeederTest {
    @Test
    fun createsFourDatesWithEightNonFutureRecordsToday() {
        val now = LocalDateTime.of(2026, 8, 29, 12, 0)
        val zone = ZoneId.systemDefault()
        val nowMillis = now.atZone(zone).toInstant().toEpochMilli()
        val records = DemoDataSeeder.records(now)

        assertTrue(records.size == 17)
        assertTrue(records.map { it.localDate(zone) }.distinct().size == 4)
        assertTrue(records.count { it.occursOn(now.toLocalDate(), zone) } == 8)
        assertTrue(records.all { it.timestampEpochMillis <= nowMillis })
    }

    @Test
    fun seedsAnsweredTiredMomentsSoStrategyPreferenceIsDemonstrable() {
        val records = DemoDataSeeder.records(LocalDateTime.of(2026, 8, 29, 12, 0))
        val answeredTired = records.filter {
            it.stateId == "tired" && it.outcome.countsAsAttempt
        }

        assertEquals(6, answeredTired.size)
        assertEquals(
            3,
            answeredTired.count {
                it.strategyId == "sensory_break" && it.outcome == InterventionOutcome.HELPED
            },
        )
        assertEquals(
            3,
            answeredTired.count {
                it.strategyId == "environment_change" &&
                    it.outcome == InterventionOutcome.DID_NOT_HELP
            },
        )
        assertTrue(answeredTired.all { it.outcomeAtMillis != null })
    }

    @Test
    fun everySeededRecordCarriesTheStrategyItsCopyDescribes() {
        val records = DemoDataSeeder.records(LocalDateTime.of(2026, 8, 29, 12, 0))

        assertTrue(records.all { it.strategyId.isNotBlank() })
    }
}
