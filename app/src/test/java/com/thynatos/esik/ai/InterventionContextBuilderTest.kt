package com.thynatos.esik.ai

import com.thynatos.esik.data.InterventionInput
import com.thynatos.esik.data.InterventionInputMethod
import com.thynatos.esik.data.InterventionOutcome
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.PersonalizationProfile
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterventionContextBuilderTest {
    @Test
    fun tiredStateUsesLowEnergyPolicyAndFiltersHighEffortAnchors() {
        val profile = profile(
            activities = listOf("müzik", "koşu", "spor"),
            lowEnergyActivities = listOf("bir şarkı dinlemek"),
        )

        val policy = InterventionContextBuilder.build(
            profile = profile,
            input = quickInput("tired", "Biraz yoruldum"),
        )

        assertEquals(InterventionNeed.REST, policy.need)
        assertEquals(EnergyExpectation.LOW, policy.energy)
        assertEquals(InterventionObjective.PAUSE_AND_RECOVER, policy.objective)
        assertTrue(policy.allowedStrategies.contains(InterventionStrategy.LOW_ENERGY_RESET))
        assertEquals(5, policy.maxDurationMinutes)
        assertTrue(policy.anchors.activities.contains("müzik"))
        assertFalse(policy.anchors.activities.contains("koşu"))
        assertFalse(policy.anchors.activities.contains("spor"))
    }

    @Test
    fun procrastinatingStateUsesMicroStartAndGoalAnchors() {
        val policy = InterventionContextBuilder.build(
            profile = profile(goals = listOf("istatistik ödevine başlamak")),
            input = quickInput("procrastinating", "Bir şeyi erteliyorum"),
        )

        assertEquals(InterventionNeed.ACTIVATION, policy.need)
        assertEquals(InterventionObjective.MICRO_START, policy.objective)
        assertTrue(policy.allowedStrategies.contains(InterventionStrategy.MICRO_START))
        assertTrue(policy.anchors.goals.contains("istatistik ödevine başlamak"))
    }

    @Test
    fun tiredProcrastinationKeepsMicroStartButReducesDuration() {
        val policy = InterventionContextBuilder.build(
            profile = profile(goals = listOf("rapora başlamak")),
            input = InterventionInput(
                text = "Çok yoruldum ama rapora başlamayı erteliyorum",
                method = InterventionInputMethod.TEXT,
            ),
        )

        assertEquals("procrastinating", policy.resolvedStateId)
        assertEquals(InterventionNeed.ACTIVATION, policy.need)
        assertEquals(EnergyExpectation.LOW, policy.energy)
        assertEquals(3, policy.maxDurationMinutes)
        assertTrue(policy.allowedStrategies.contains(InterventionStrategy.MICRO_START))
        assertTrue(policy.allowedStrategies.contains(InterventionStrategy.SENSORY_BREAK))
    }

    @Test
    fun customTextOverridesGenericSelectedState() {
        val policy = InterventionContextBuilder.build(
            profile = profile(goals = listOf("ders çalışmak")),
            input = InterventionInput(
                text = "Ders çalışmam lazım ama başlamayı erteliyorum",
                stateId = "habit",
                stateLabel = "Alışkanlıkla açtım",
                method = InterventionInputMethod.VOICE,
            ),
        )

        assertEquals("procrastinating", policy.resolvedStateId)
        assertEquals(InterventionObjective.MICRO_START, policy.objective)
    }

    @Test
    fun englishFatigueInputMapsToRestPolicy() {
        val policy = InterventionContextBuilder.build(
            profile = profile(activities = listOf("music")),
            input = InterventionInput(
                text = "I am exhausted and I am only scrolling to switch off",
                method = InterventionInputMethod.TEXT,
            ),
        )

        assertEquals("tired", policy.resolvedStateId)
        assertEquals(InterventionNeed.REST, policy.need)
        assertEquals(EnergyExpectation.LOW, policy.energy)
    }

    @Test
    fun intentionalRestPreservesAutonomyWithTimedUseStrategy() {
        val policy = InterventionContextBuilder.build(
            profile = profile(),
            input = quickInput("relaxing", "Sadece kafa dağıtıyorum"),
        )

        assertEquals(InterventionNeed.INTENTIONAL_BREAK, policy.need)
        assertEquals(InterventionObjective.MAKE_BREAK_INTENTIONAL, policy.objective)
        assertTrue(
            policy.allowedStrategies.contains(InterventionStrategy.TIMED_INTENTIONAL_USE),
        )
        assertTrue(policy.forbiddenPatterns.contains("automatic_stop_command"))
        assertEquals(10, policy.maxDurationMinutes)
    }

    @Test
    fun sparseProfileDoesNotInventAnchors() {
        val policy = InterventionContextBuilder.build(
            profile = profile(),
            input = quickInput("habit", "Alışkanlıkla açtım"),
        )

        assertTrue(policy.anchors.all.isEmpty())
        assertEquals(InterventionNeed.HABIT, policy.need)
    }

    @Test
    fun sameProfileProducesDifferentPolicyAcrossStates() {
        val profile = profile(
            goals = listOf("daha düzenli çalışmak"),
            activities = listOf("müzik"),
        )
        val tired = InterventionContextBuilder.build(
            profile,
            quickInput("tired", "Biraz yoruldum"),
        )
        val procrastinating = InterventionContextBuilder.build(
            profile,
            quickInput("procrastinating", "Bir şeyi erteliyorum"),
        )
        val relaxing = InterventionContextBuilder.build(
            profile,
            quickInput("relaxing", "Sadece kafa dağıtıyorum"),
        )

        assertNotEquals(tired.need, procrastinating.need)
        assertNotEquals(procrastinating.objective, relaxing.objective)
        assertNotEquals(tired.allowedStrategies, relaxing.allowedStrategies)
    }

    @Test
    fun strategyTheUserReportedAsHelpfulBecomesAHintWithoutLeavingTheAllowedSet() {
        val policy = InterventionContextBuilder.build(
            profile = profile(),
            input = quickInput("tired", "Biraz yoruldum"),
            history = answeredTired("sensory_break", InterventionOutcome.HELPED, times = 3),
        )

        assertEquals(InterventionStrategy.SENSORY_BREAK, policy.preferredStrategy)
        assertTrue(policy.allowedStrategies.contains(InterventionStrategy.SENSORY_BREAK))
        assertTrue(policy.evidenceSummary.contains("user_reported_helpful=sensory_break"))
    }

    @Test
    fun strategyTheUserRepeatedlyRejectedIsRemovedFromTheAllowedSet() {
        val policy = InterventionContextBuilder.build(
            profile = profile(),
            input = quickInput("tired", "Biraz yoruldum"),
            history = answeredTired(
                strategyId = "environment_change",
                outcome = InterventionOutcome.DID_NOT_HELP,
                times = 3,
            ),
        )

        assertFalse(policy.allowedStrategies.contains(InterventionStrategy.ENVIRONMENT_CHANGE))
        assertTrue(policy.allowedStrategies.isNotEmpty())
        assertTrue(policy.evidenceSummary.contains("strategies_narrowed_by_user_feedback=true"))
    }

    @Test
    fun feedbackNeverChangesTheNeedObjectiveOrDurationCeiling() {
        val input = quickInput("tired", "Biraz yoruldum")
        val withoutHistory = InterventionContextBuilder.build(profile(), input)
        val withHistory = InterventionContextBuilder.build(
            profile = profile(),
            input = input,
            history = answeredTired("sensory_break", InterventionOutcome.HELPED, times = 4),
        )

        assertEquals(withoutHistory.need, withHistory.need)
        assertEquals(withoutHistory.objective, withHistory.objective)
        assertEquals(withoutHistory.energy, withHistory.energy)
        assertEquals(withoutHistory.maxDurationMinutes, withHistory.maxDurationMinutes)
    }

    @Test
    fun unansweredHistoryLeavesThePolicyExactlyAsItWas() {
        val input = quickInput("tired", "Biraz yoruldum")
        val withoutHistory = InterventionContextBuilder.build(profile(), input)
        val withHistory = InterventionContextBuilder.build(
            profile = profile(),
            input = input,
            history = answeredTired("sensory_break", InterventionOutcome.UNKNOWN, times = 6),
        )

        assertEquals(withoutHistory, withHistory)
    }

    private fun answeredTired(
        strategyId: String,
        outcome: InterventionOutcome,
        times: Int,
    ): List<InterventionRecord> = (1..times).map { index ->
        InterventionRecord(
            timestampEpochMillis = 1_756_000_000_000L + index * 60_000L,
            usageMinutes = 78,
            text = "bugün yoruldum",
            choice = UserChoice.STOPPED,
            stateId = "tired",
            stateLabel = "Biraz yoruldum",
            aiAlternative = "iki dakikalık küçük bir adım",
            strategyId = strategyId,
            outcome = outcome,
        )
    }

    private fun quickInput(stateId: String, label: String): InterventionInput =
        InterventionInput(
            text = label,
            stateId = stateId,
            stateLabel = label,
            method = InterventionInputMethod.QUICK_REPLY,
        )

    private fun profile(
        goals: List<String> = emptyList(),
        activities: List<String> = emptyList(),
        lowEnergyActivities: List<String> = emptyList(),
    ): UserProfile = UserProfile(
        name = "Ayşe",
        department = "İstatistik",
        hobbies = emptyList(),
        improvementArea = "",
        reason = "",
        targetAppLabel = "Instagram",
        targetPackage = "com.instagram.android",
        dailyLimitMinutes = 60,
        personalization = PersonalizationProfile(
            goals = goals,
            preferredActivities = activities,
            lowEnergyActivities = lowEnergyActivities,
        ),
    )
}
