package com.thynatos.esik.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrisisFilterTest {
    @Test
    fun catchesTurkishCrisisPhraseWithDiacritics() {
        assertTrue(CrisisFilter.check("Kendimi öldürmek istiyorum.").isCrisisSignal)
    }

    @Test
    fun doesNotFlagOrdinaryFatigueText() {
        assertFalse(CrisisFilter.check("Bugün yoruldum, biraz kafamı dağıtacağım.").isCrisisSignal)
    }
}
