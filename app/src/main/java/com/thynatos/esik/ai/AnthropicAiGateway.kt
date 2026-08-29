package com.thynatos.esik.ai

import com.thynatos.esik.data.AiCard
import com.thynatos.esik.data.DailyReport
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.UserProfile

/**
 * Integration seam for person C.
 *
 * Keep the real HTTP client behind [AiGateway]. Do not commit an API key. The mobile-direct
 * approach is acceptable only for the hackathon demo; production requires a backend proxy.
 */
class AnthropicAiGateway(
    private val fallback: AiGateway = MockAiGateway(),
) : AiGateway {
    override fun generateCard(
        profile: UserProfile,
        currentUsageMinutes: Int,
        userText: String,
    ): AiCard {
        // TODO(C): call the current Haiku-class model, strictly parse JSON, validate language,
        // and return fallback output on timeout, HTTP error, parse error, or blocked wording.
        return fallback.generateCard(profile, currentUsageMinutes, userText)
    }

    override fun generateDailyReport(
        profile: UserProfile,
        records: List<InterventionRecord>,
        currentUsageMinutes: Int,
    ): DailyReport {
        // TODO(C): call the current Sonnet-class model only when records.size >= 7.
        return fallback.generateDailyReport(profile, records, currentUsageMinutes)
    }
}
