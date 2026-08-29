package com.thynatos.esik.ai

import com.thynatos.esik.data.AiCard
import com.thynatos.esik.data.DailyReport
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile

class MockAiGateway : AiGateway {
    override fun generateCard(
        profile: UserProfile,
        currentUsageMinutes: Int,
        userText: String,
    ): AiCard {
        val hobby = profile.hobbies.firstOrNull().orEmpty()
        val alternative = when {
            hobby.isNotBlank() -> "$hobby için iki dakikalık küçük bir başlangıç yapabilirsin."
            profile.improvementArea.isNotBlank() ->
                "${profile.improvementArea} için iki dakikalık tek bir adım seçebilirsin."
            else -> "İki dakika boyunca telefonu bırakıp bulunduğun yerde kısa bir mola verebilirsin."
        }
        return AiCard(
            question = "Şu anda dinlenmeye mi, dikkatini başka yere vermeye mi ihtiyacın var?",
            alternative = alternative,
        )
    }

    override fun generateDailyReport(
        profile: UserProfile,
        records: List<InterventionRecord>,
        currentUsageMinutes: Int,
    ): DailyReport {
        if (records.size < REPORT_MINIMUM_RECORDS) {
            return DailyReport(
                totalUsageMinutes = currentUsageMinutes,
                limitMinutes = profile.dailyLimitMinutes,
                interventionCount = records.size,
                continuedCount = records.count { it.choice == UserChoice.CONTINUE },
                stoppedCount = records.count { it.choice == UserChoice.STOPPED },
                observationQuestion = "Yeterli veri yok.",
                microStep = "Yarın yalnızca bir kaydı tamamlamayı dene.",
                insufficientData = true,
            )
        }

        val continued = records.count { it.choice == UserChoice.CONTINUE }
        val stopped = records.count { it.choice == UserChoice.STOPPED }
        return DailyReport(
            totalUsageMinutes = currentUsageMinutes,
            limitMinutes = profile.dailyLimitMinutes,
            interventionCount = records.size,
            continuedCount = continued,
            stoppedCount = stopped,
            observationQuestion = "Akşam saatlerindeki girişler yorgunlukla bağlantılı olabilir mi?",
            microStep = "Yarın 22:30'da telefonu şarja bulunduğun yerden biraz uzağa bırak.",
        )
    }

    private companion object {
        const val REPORT_MINIMUM_RECORDS = 7
    }
}
