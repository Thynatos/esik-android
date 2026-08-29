package com.thynatos.esik.usage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CooldownPolicyTest {
    @Test
    fun firstInterventionCanShow() {
        assertTrue(CooldownPolicy.shouldShow(nowMillis = 1_000L, lastShownAtMillis = null))
    }

    @Test
    fun interventionIsBlockedBeforeCooldownExpires() {
        assertFalse(
            CooldownPolicy.shouldShow(
                nowMillis = CooldownPolicy.DEFAULT_COOLDOWN_MILLIS - 1L,
                lastShownAtMillis = 0L,
            ),
        )
    }

    @Test
    fun interventionCanShowAtCooldownBoundary() {
        assertTrue(
            CooldownPolicy.shouldShow(
                nowMillis = CooldownPolicy.DEFAULT_COOLDOWN_MILLIS,
                lastShownAtMillis = 0L,
            ),
        )
    }

    @Test
    fun clockRollbackDoesNotLockTheUserOut() {
        assertTrue(CooldownPolicy.shouldShow(nowMillis = 500L, lastShownAtMillis = 1_000L))
    }
}
