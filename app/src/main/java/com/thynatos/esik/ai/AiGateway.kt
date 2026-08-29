package com.thynatos.esik.ai

import com.thynatos.esik.data.AiCard
import com.thynatos.esik.data.DailyReport
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.UserProfile

interface AiGateway {
    fun generateCard(
        profile: UserProfile,
        currentUsageMinutes: Int,
        userText: String,
    ): AiCard

    fun generateDailyReport(
        profile: UserProfile,
        records: List<InterventionRecord>,
        currentUsageMinutes: Int,
    ): DailyReport
}
