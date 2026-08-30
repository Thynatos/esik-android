package com.thynatos.esik.ai

import com.thynatos.esik.usage.UsagePatternSnapshot

/**
 * A tentative reading of the current moment. It is never applied silently: the overlay asks the
 * user to confirm it with one tap and falls back to the normal choices when rejected.
 */
data class NeedHypothesis(
    val stateId: String,
    val defaultLabel: String,
    val reasons: List<String>,
)

/**
 * Conservative context inference used only after the user-defined threshold has already fired.
 *
 * Eşik deliberately does not infer procrastination, boredom, overwhelm, or fatigue from usage
 * behaviour alone. The two supported hypotheses are comparatively observable: repeated reopening
 * and a sustained late-night usage context.
 */
object NeedInference {
    const val STATE_LATE_NIGHT: String = "late_night"
    const val STATE_HABIT: String = "habit"

    private const val RAPID_REOPEN_MIN_OPENS = 3
    private const val IMMEDIATE_REOPEN_MILLIS = 30_000L
    private const val VERY_SHORT_SESSION_MILLIS = 45_000L
    private const val LATE_NIGHT_RUN_MILLIS = 45L * 60L * 1_000L

    fun infer(
        pattern: UsagePatternSnapshot,
        hourOfDay: Int,
    ): NeedHypothesis? {
        val hour = hourOfDay.coerceIn(0, 23)
        val lateNightReasons = buildList {
            if (hour >= 23 || hour <= 4) add("late_hour")
            if (pattern.continuousActivityMillis >= LATE_NIGHT_RUN_MILLIS) {
                add("long_continuous_use")
            }
        }
        if (lateNightReasons.size >= 2) {
            return NeedHypothesis(
                stateId = STATE_LATE_NIGHT,
                defaultLabel = "Uyumadan önce bakıyorum",
                reasons = lateNightReasons,
            )
        }

        val habitReasons = buildList {
            if (
                pattern.lastGapMillis != UsagePatternSnapshot.UNKNOWN_GAP &&
                pattern.lastGapMillis <= IMMEDIATE_REOPEN_MILLIS
            ) {
                add("reopened_within_seconds")
            }
            if (pattern.targetOpenCount >= RAPID_REOPEN_MIN_OPENS) {
                add("several_opens_in_short_window")
            }
            if (pattern.medianSessionMillis in 1 until VERY_SHORT_SESSION_MILLIS) {
                add("very_short_sessions")
            }
        }
        return if (habitReasons.size >= 2) {
            NeedHypothesis(
                stateId = STATE_HABIT,
                defaultLabel = "Alışkanlıkla açtım",
                reasons = habitReasons,
            )
        } else {
            null
        }
    }
}
