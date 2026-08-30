package com.thynatos.esik.ai

import com.thynatos.esik.data.AiCard
import com.thynatos.esik.data.DailyReport
import com.thynatos.esik.data.InterventionInput
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.PersonalizationProfile
import com.thynatos.esik.data.ProfileIntake
import com.thynatos.esik.data.UserProfile

interface AiGateway {
    suspend fun generateProfile(intake: ProfileIntake): PersonalizationProfile

    /**
     * [history] is the device-local record list. It only lets the locally compiled policy prefer
     * strategies this user has already reported as helpful; it is never sent as raw text.
     */
    suspend fun generateCard(
        profile: UserProfile,
        currentUsageMinutes: Int,
        input: InterventionInput,
        history: List<InterventionRecord> = emptyList(),
    ): AiCard

    suspend fun generateDailyReport(
        profile: UserProfile,
        records: List<InterventionRecord>,
        currentUsageMinutes: Int,
    ): DailyReport
}
