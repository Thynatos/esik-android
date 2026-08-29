package com.thynatos.esik.ai

import com.thynatos.esik.data.InterventionInput
import com.thynatos.esik.data.InterventionInputMethod
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.ProfileIntake
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAiGatewayTest {
    private val gateway = MockAiGateway()
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
    fun profileGenerationProducesSixQuickStates() = runBlocking {
        val result = gateway.generateProfile(
            ProfileIntake(
                name = "Ayşe",
                biography = "Derslerden sonra çok yoruluyorum ve çalışmaya başlamak yerine Instagram'da oyalanıyorum.",
                hobbies = listOf("gitar", "koşu"),
                improvementArea = "daha düzenli çalışmak",
                reason = "gece daha rahat uyumak",
            ),
        )

        assertEquals("six quick states", 6, result.quickStates.size)
        assertTrue(
            "yoruluyorum should map to yorgunluk",
            result.recurringContexts.contains("yorgunluk"),
        )
        assertTrue(
            "başlamak yerine oyalanıyorum should map to erteleme",
            result.recurringContexts.contains("erteleme"),
        )
        assertTrue("explicit hobby should be retained", result.preferredActivities.contains("gitar"))
    }

    @Test
    fun quickReplyCardUsesSelectedState() = runBlocking {
        val card = gateway.generateCard(
            profile = profile,
            currentUsageMinutes = 78,
            input = InterventionInput(
                text = "Bir şeyi erteliyorum",
                stateId = "procrastinating",
                stateLabel = "Bir şeyi erteliyorum",
                method = InterventionInputMethod.QUICK_REPLY,
            ),
        )

        assertTrue(card.question.contains("iki dakika"))
        assertTrue(SafetyLanguageValidator.isDisplaySafe(card.question, card.alternative))
    }

    @Test
    fun reportIsUnavailableBelowSevenRecords() = runBlocking {
        val report = gateway.generateDailyReport(
            profile = profile,
            records = List(6, ::record),
            currentUsageMinutes = 78,
        )

        assertTrue(report.insufficientData)
    }

    @Test
    fun reportIsAvailableAtSevenRecords() = runBlocking {
        val report = gateway.generateDailyReport(
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
        stateId = if (index % 2 == 0) "procrastinating" else "tired",
        stateLabel = if (index % 2 == 0) "Bir şeyi erteliyorum" else "Biraz yoruldum",
    )
}
