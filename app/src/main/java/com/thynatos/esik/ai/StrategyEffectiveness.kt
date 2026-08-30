package com.thynatos.esik.ai

import com.thynatos.esik.data.InterventionOutcome
import com.thynatos.esik.data.InterventionRecord

/**
 * How often one strategy was reported as helpful by this user, in one state.
 *
 * [attempts] counts only moments the user explicitly answered "helped" or "did not help". A moment
 * the user skipped, ignored, or never saw is not evidence about the strategy.
 */
internal data class StrategyScore(
    val strategy: InterventionStrategy,
    val attempts: Int,
    val helpedCount: Int,
) {
    val helpfulRatio: Double
        get() = if (attempts == 0) 0.0 else helpedCount.toDouble() / attempts.toDouble()
}

/**
 * Locally computed preference signal for one intervention state.
 *
 * This is deliberately not a model, a score shown to the user, or a judgement about the person. It
 * only reorders options the local policy already considered acceptable, and it stays inert until
 * the same strategy has been answered for at least [StrategyEffectivenessBuilder.MIN_ATTEMPTS]
 * times, so a single bad evening cannot rewrite the user's options.
 */
internal data class StrategyEffectiveness(
    val stateId: String,
    val scores: List<StrategyScore>,
    val preferred: InterventionStrategy?,
    val discouraged: Set<InterventionStrategy>,
) {
    /**
     * Removes strategies this user has repeatedly said did not help.
     *
     * The allowed set is never emptied: when every acceptable strategy is discouraged, the original
     * set is returned unchanged rather than leaving the user with nothing.
     */
    fun narrow(allowed: Set<InterventionStrategy>): Set<InterventionStrategy> {
        if (discouraged.isEmpty()) return allowed
        val narrowed = allowed - discouraged
        return narrowed.ifEmpty { allowed }
    }

    /** The learned preference, but only when the current context still allows it. */
    fun preferenceWithin(allowed: Set<InterventionStrategy>): InterventionStrategy? =
        preferred?.takeIf(allowed::contains)

    companion object {
        val EMPTY = StrategyEffectiveness(
            stateId = "",
            scores = emptyList(),
            preferred = null,
            discouraged = emptySet(),
        )
    }
}

internal object StrategyEffectivenessBuilder {
    /** Minimum answered attempts before one strategy may influence anything. */
    const val MIN_ATTEMPTS: Int = 3

    fun build(
        records: List<InterventionRecord>,
        stateId: String,
    ): StrategyEffectiveness {
        val normalizedState = stateId.trim().lowercase()
        if (normalizedState.isEmpty()) return StrategyEffectiveness.EMPTY

        val attempts = records
            .asSequence()
            .filter { it.stateId.trim().lowercase() == normalizedState }
            .filter { it.outcome.countsAsAttempt }
            .mapNotNull { record ->
                val strategy = InterventionStrategy.fromWire(record.strategyId)
                    ?: return@mapNotNull null
                record to strategy
            }
            .sortedBy { (record, _) -> record.timestampEpochMillis }
            .toList()
            .takeLast(MAX_CONSIDERED_ATTEMPTS)

        if (attempts.isEmpty()) return StrategyEffectiveness.EMPTY

        val scores = attempts
            .groupBy({ (_, strategy) -> strategy }, { (record, _) -> record })
            .map { (strategy, strategyRecords) ->
                StrategyScore(
                    strategy = strategy,
                    attempts = strategyRecords.size,
                    helpedCount = strategyRecords.count {
                        it.outcome == InterventionOutcome.HELPED
                    },
                )
            }
            .sortedWith(
                compareByDescending<StrategyScore> { it.helpfulRatio }
                    .thenByDescending { it.attempts }
                    .thenBy { it.strategy.wireValue },
            )

        val preferred = scores.firstOrNull { score ->
            score.strategy != InterventionStrategy.OTHER &&
                score.attempts >= MIN_ATTEMPTS &&
                score.helpfulRatio >= PREFER_RATIO
        }?.strategy

        val discouraged = scores
            .filter { it.attempts >= MIN_ATTEMPTS && it.helpfulRatio <= DISCOURAGE_RATIO }
            .mapTo(linkedSetOf(), StrategyScore::strategy)

        return StrategyEffectiveness(
            stateId = normalizedState,
            scores = scores,
            preferred = preferred,
            discouraged = discouraged,
        )
    }

    private const val MAX_CONSIDERED_ATTEMPTS = 60
    private const val PREFER_RATIO = 0.6
    private const val DISCOURAGE_RATIO = 0.25
}
