package com.thynatos.esik.ai

import com.thynatos.esik.data.InterventionRecord

/**
 * Stops Eşik from repeating a context guess the user keeps rejecting.
 * Only explicit yes/no answers count; a dismissed overlay is not treated as a rejection.
 */
object NeedInferenceCalibration {
    const val MIN_ANSWERS: Int = 3
    const val MIN_ACCEPTANCE_RATIO: Double = 0.34
    const val MAX_CONSIDERED_ANSWERS: Int = 30

    fun isTrusted(
        records: List<InterventionRecord>,
        stateId: String,
    ): Boolean {
        val normalized = stateId.trim().lowercase()
        if (normalized.isEmpty()) return false

        val answers = records
            .asSequence()
            .filter { it.hypothesisStateId.trim().lowercase() == normalized }
            .mapNotNull { it.hypothesisAccepted }
            .toList()
            .takeLast(MAX_CONSIDERED_ANSWERS)

        if (answers.size < MIN_ANSWERS) return true
        val acceptanceRatio = answers.count { it }.toDouble() / answers.size.toDouble()
        return acceptanceRatio > MIN_ACCEPTANCE_RATIO
    }

    fun filter(
        hypothesis: NeedHypothesis?,
        records: List<InterventionRecord>,
    ): NeedHypothesis? = hypothesis?.takeIf { isTrusted(records, it.stateId) }
}
