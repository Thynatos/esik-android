package com.thynatos.esik.usage

/** A foreground/background transition, decoupled from Android event classes for unit testing. */
data class UsageEventSample(
    val packageName: String,
    val timestampMillis: Long,
    val type: UsageEventType,
)

enum class UsageEventType {
    FOREGROUND,
    BACKGROUND,
    SCREEN_OFF,
}

data class AppSession(
    val packageName: String,
    val startMillis: Long,
    val endMillis: Long?,
) {
    val isOpen: Boolean
        get() = endMillis == null

    fun durationMillis(nowMillis: Long): Long =
        ((endMillis ?: nowMillis) - startMillis).coerceAtLeast(0L)
}

/**
 * Describes the shape of recent use without deciding whether an intervention should happen.
 *
 * Eşik still intervenes only after the user-defined threshold. This snapshot is context that can
 * make the threshold intervention lower-friction and more relevant.
 */
data class UsagePatternSnapshot(
    val targetOpenCount: Int,
    val isTargetForeground: Boolean,
    val currentSessionMillis: Long,
    val completedSessionCount: Int,
    val medianSessionMillis: Long,
    val lastGapMillis: Long,
    val continuousActivityMillis: Long,
) {
    companion object {
        val EMPTY = UsagePatternSnapshot(
            targetOpenCount = 0,
            isTargetForeground = false,
            currentSessionMillis = 0L,
            completedSessionCount = 0,
            medianSessionMillis = 0L,
            lastGapMillis = UNKNOWN_GAP,
            continuousActivityMillis = 0L,
        )

        const val UNKNOWN_GAP: Long = -1L
    }
}

object UsageSessionAnalyzer {
    const val DEFAULT_IDLE_GAP_MILLIS: Long = 3L * 60L * 1_000L
    const val DEFAULT_OPEN_WINDOW_MILLIS: Long = 10L * 60L * 1_000L

    /**
     * Android can emit PAUSED -> RESUMED while navigating between Activities inside the same app.
     * Adjacent same-package sessions separated by only a few seconds are therefore coalesced unless
     * the screen turned off in between. A genuine leave/reopen normally has another foreground
     * package between the two sessions and is not coalesced.
     */
    const val SAME_PACKAGE_TRANSITION_GRACE_MILLIS: Long = 5_000L

    fun analyze(
        events: List<UsageEventSample>,
        targetPackage: String,
        nowMillis: Long,
        idleGapMillis: Long = DEFAULT_IDLE_GAP_MILLIS,
        openWindowMillis: Long = DEFAULT_OPEN_WINDOW_MILLIS,
    ): UsagePatternSnapshot {
        if (targetPackage.isBlank()) return UsagePatternSnapshot.EMPTY

        val ordered = events.sortedBy(UsageEventSample::timestampMillis)
        val rawSessions = buildSessions(ordered)
        val sessions = coalesceSamePackageTransitions(rawSessions, ordered)
        val targetSessions = sessions.filter { it.packageName == targetPackage }

        if (targetSessions.isEmpty()) {
            return UsagePatternSnapshot.EMPTY.copy(
                continuousActivityMillis = continuousActivityMillis(
                    sessions = sessions,
                    ordered = ordered,
                    nowMillis = nowMillis,
                    idleGapMillis = idleGapMillis,
                ),
            )
        }

        val current = targetSessions.lastOrNull()?.takeIf(AppSession::isOpen)
        val completed = targetSessions.filterNot(AppSession::isOpen)
        val openWindowStart = nowMillis - openWindowMillis

        return UsagePatternSnapshot(
            targetOpenCount = targetSessions.count { it.startMillis >= openWindowStart },
            isTargetForeground = current != null,
            currentSessionMillis = current?.durationMillis(nowMillis) ?: 0L,
            completedSessionCount = completed.size,
            medianSessionMillis = medianMillis(completed.map { it.durationMillis(nowMillis) }),
            lastGapMillis = lastGapMillis(targetSessions, current),
            continuousActivityMillis = continuousActivityMillis(
                sessions = sessions,
                ordered = ordered,
                nowMillis = nowMillis,
                idleGapMillis = idleGapMillis,
            ),
        )
    }

    private fun buildSessions(ordered: List<UsageEventSample>): List<AppSession> {
        val sessions = mutableListOf<AppSession>()
        var openPackage: String? = null
        var openStart = 0L

        fun close(atMillis: Long) {
            val packageName = openPackage ?: return
            sessions += AppSession(packageName, openStart, atMillis.coerceAtLeast(openStart))
            openPackage = null
        }

        ordered.forEach { event ->
            when (event.type) {
                UsageEventType.FOREGROUND -> {
                    if (openPackage == event.packageName) return@forEach
                    close(event.timestampMillis)
                    openPackage = event.packageName
                    openStart = event.timestampMillis
                }

                UsageEventType.BACKGROUND -> {
                    if (openPackage == event.packageName) close(event.timestampMillis)
                }

                UsageEventType.SCREEN_OFF -> close(event.timestampMillis)
            }
        }

        openPackage?.let { packageName ->
            sessions += AppSession(packageName, openStart, null)
        }
        return sessions
    }

    private fun coalesceSamePackageTransitions(
        sessions: List<AppSession>,
        ordered: List<UsageEventSample>,
    ): List<AppSession> {
        if (sessions.size < 2) return sessions
        val merged = mutableListOf<AppSession>()

        sessions.forEach { session ->
            val previous = merged.lastOrNull()
            val previousEnd = previous?.endMillis
            val gap = if (previousEnd != null) session.startMillis - previousEnd else Long.MAX_VALUE
            val screenOffBetween = previousEnd != null && ordered.any { event ->
                event.type == UsageEventType.SCREEN_OFF &&
                    event.timestampMillis in previousEnd..session.startMillis
            }

            if (
                previous != null &&
                previous.packageName == session.packageName &&
                previousEnd != null &&
                gap in 0..SAME_PACKAGE_TRANSITION_GRACE_MILLIS &&
                !screenOffBetween
            ) {
                merged[merged.lastIndex] = previous.copy(endMillis = session.endMillis)
            } else {
                merged += session
            }
        }

        return merged
    }

    private fun lastGapMillis(
        targetSessions: List<AppSession>,
        current: AppSession?,
    ): Long {
        val reference = current ?: return UsagePatternSnapshot.UNKNOWN_GAP
        val previous = targetSessions
            .filterNot(AppSession::isOpen)
            .lastOrNull { it.endMillis != null && it.endMillis <= reference.startMillis }
            ?: return UsagePatternSnapshot.UNKNOWN_GAP
        val previousEnd = previous.endMillis ?: return UsagePatternSnapshot.UNKNOWN_GAP
        return (reference.startMillis - previousEnd).coerceAtLeast(0L)
    }

    private fun medianMillis(values: List<Long>): Long {
        if (values.isEmpty()) return 0L
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2L
        }
    }

    private fun continuousActivityMillis(
        sessions: List<AppSession>,
        ordered: List<UsageEventSample>,
        nowMillis: Long,
        idleGapMillis: Long,
    ): Long {
        if (sessions.isEmpty()) return 0L

        val lastSession = sessions.last()
        val lastEnd = lastSession.endMillis ?: nowMillis
        if (nowMillis - lastEnd > idleGapMillis) return 0L

        val lastScreenOff = ordered.lastOrNull { it.type == UsageEventType.SCREEN_OFF }?.timestampMillis
        var runStart = lastSession.startMillis

        for (index in sessions.lastIndex downTo 1) {
            val earlier = sessions[index - 1]
            val earlierEnd = earlier.endMillis ?: continue
            if (sessions[index].startMillis - earlierEnd > idleGapMillis) break
            runStart = earlier.startMillis
        }

        if (lastScreenOff != null && lastScreenOff > runStart) {
            runStart = lastScreenOff
        }
        return (nowMillis - runStart).coerceAtLeast(0L)
    }
}
