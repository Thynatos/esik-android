package com.thynatos.esik.usage

object CooldownPolicy {
    const val DEFAULT_COOLDOWN_MILLIS: Long = 15L * 60L * 1_000L

    fun shouldShow(
        nowMillis: Long,
        lastShownAtMillis: Long?,
        cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    ): Boolean {
        if (lastShownAtMillis == null) return true
        if (nowMillis < lastShownAtMillis) return true
        return nowMillis - lastShownAtMillis >= cooldownMillis.coerceAtLeast(0L)
    }
}
