package com.thynatos.esik.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCardSemanticValidatorTest {
    private val policy = InterventionPolicy(
        resolvedStateId = "tired",
        need = InterventionNeed.REST,
        energy = EnergyExpectation.LOW,
        objective = InterventionObjective.PAUSE_AND_RECOVER,
        allowedStrategies = setOf(
            InterventionStrategy.LOW_ENERGY_RESET,
            InterventionStrategy.SENSORY_BREAK,
        ),
        maxDurationMinutes = 5,
        anchors = PersonalizationAnchors(
            activities = listOf("müzik"),
            lowEnergyActivities = listOf("bir şarkı dinlemek"),
        ),
        forbiddenPatterns = listOf("high_effort_action"),
        evidenceSummary = "source=quick_reply; resolved_state=tired",
    )

    @Test
    fun acceptsGroundedLowEnergyCard() {
        val result = AiCardSemanticValidator.validate(
            StructuredAiCard(
                need = InterventionNeed.REST,
                strategy = InterventionStrategy.LOW_ENERGY_RESET,
                question = "Şu an kısa bir dinlenme sana daha iyi gelebilir mi?",
                alternative = "Bir şarkı boyunca telefonu bırakıp yalnızca müzik dinlemeyi deneyebilirsin.",
                durationMinutes = 4,
                personalizationAnchor = "müzik",
            ),
            policy,
        )

        assertTrue(result.errors.joinToString(), result.isValid)
    }

    @Test
    fun rejectsStrategyOutsideCompiledPolicy() {
        val result = AiCardSemanticValidator.validate(
            validCard().copy(strategy = InterventionStrategy.MICRO_START),
            policy,
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("strategy_not_allowed_for_context"))
    }

    @Test
    fun rejectsInventedAnchor() {
        val result = AiCardSemanticValidator.validate(
            validCard().copy(personalizationAnchor = "podcast"),
            policy,
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("personalization_anchor_not_supplied_by_user"))
    }

    @Test
    fun rejectsHighEffortActionForTiredUser() {
        val result = AiCardSemanticValidator.validate(
            validCard().copy(alternative = "Şimdi spora gitmeyi deneyebilirsin."),
            policy,
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("high_effort_action_for_low_energy_context"))
    }

    @Test
    fun rejectsQuestionWithoutQuestionMark() {
        val result = AiCardSemanticValidator.validate(
            validCard().copy(question = "Şu an kısa bir dinlenmeye ihtiyacın olabilir"),
            policy,
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("question_must_end_with_question_mark"))
    }

    @Test
    fun rejectsDurationAboveLocalMaximum() {
        val result = AiCardSemanticValidator.validate(
            validCard().copy(durationMinutes = 10),
            policy,
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("duration_outside_allowed_range"))
    }

    @Test
    fun rejectsInventedLiveContent() {
        val result = AiCardSemanticValidator.validate(
            validCard().copy(
                alternative = "En sevdiğin podcastin yeni bölümünü dinlemeyi deneyebilirsin.",
            ),
            policy,
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("invented_or_unverified_live_content"))
    }

    private fun validCard(): StructuredAiCard = StructuredAiCard(
        need = InterventionNeed.REST,
        strategy = InterventionStrategy.SENSORY_BREAK,
        question = "Şu an kısa bir ekran molası iyi gelebilir mi?",
        alternative = "İki dakika boyunca telefonu masaya bırakıp bulunduğun ortamı dinleyebilirsin.",
        durationMinutes = 2,
        personalizationAnchor = "",
    )
}
