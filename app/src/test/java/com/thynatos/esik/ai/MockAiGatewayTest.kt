package com.thynatos.esik.ai

import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAiGatewayTest {
    private val profile = UserProfile(
        name = "Ayşe",
        department = "İstatistik",
        hobbies = listOf("gitar"),
        improvementArea = "İngilizce",
        reason = "gece uyuyamıyorum",
        targetAppLabel = "Instagram",
        targetPackage = "com.instagram.android",
        dailyLimitMinutes = 60,
    )

    @Test
    fun reportIsUnavailableBelowSevenRecords() {
        val report = MockAiGateway().generateDailyReport(
            profile = profile,
            records = List(6, ::record),
            currentUsageMinutes = 78,
        )

        assertTrue(report.insufficientData)
    }

    @Test
    fun reportIsAvailableAtSevenRecords() {
        val report = MockAiGateway().generateDailyReport(
            profile = profile,
            records = List(7, ::record),
            currentUsageMinutes = 78,
        )

        assertFalse(report.insufficientData)
        assertTrue(
            SafetyLanguageValidator.isDisplaySafe(
                report.observationQuestion,
                report.microStep,
            ),
        )
    }

    private fun record(index: Int): InterventionRecord = InterventionRecord(
        timestampEpochMillis = index.toLong(),
        usageMinutes = 60 + index,
        text = "örnek",
        choice = if (index % 2 == 0) UserChoice.CONTINUE else UserChoice.STOPPED,
    )
}
