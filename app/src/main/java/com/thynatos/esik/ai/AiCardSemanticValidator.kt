package com.thynatos.esik.ai

internal object AiCardSemanticValidator {
    fun validate(
        card: StructuredAiCard,
        policy: InterventionPolicy,
    ): CardValidationResult {
        val errors = buildList {
            if (card.need != policy.need) {
                add("need_must_match_compiled_policy")
            }
            if (card.strategy !in policy.allowedStrategies) {
                add("strategy_not_allowed_for_context")
            }
            if (card.durationMinutes !in 1..policy.maxDurationMinutes) {
                add("duration_outside_allowed_range")
            }

            val question = card.question.trim()
            val alternative = card.alternative.trim()
            if (question.isBlank()) add("question_blank")
            if (question.length > MAX_QUESTION_CHARS) add("question_too_long")
            if (question.isNotBlank() && !question.endsWith('?')) add("question_must_end_with_question_mark")
            if (alternative.isBlank()) add("alternative_blank")
            if (alternative.length > MAX_ALTERNATIVE_CHARS) add("alternative_too_long")
            if (!SafetyLanguageValidator.isDisplaySafe(question, alternative)) {
                add("unsafe_or_judgmental_language")
            }

            val normalizedAlternative = alternative.normalizeForSemanticMatching()
            if (GENERIC_ADVICE.any(normalizedAlternative::contains)) {
                add("alternative_not_concrete")
            }
            if (
                policy.energy == EnergyExpectation.LOW &&
                HIGH_EFFORT_ACTIONS.any(normalizedAlternative::contains)
            ) {
                add("high_effort_action_for_low_energy_context")
            }
            if (INVENTED_LIVE_CONTENT.any(normalizedAlternative::contains)) {
                add("invented_or_unverified_live_content")
            }
            if (
                policy.need == InterventionNeed.INTENTIONAL_BREAK &&
                FORCE_STOP_LANGUAGE.any(normalizedAlternative::contains)
            ) {
                add("intentional_break_must_preserve_autonomy")
            }

            val anchor = card.personalizationAnchor.trim()
            if (anchor.isNotEmpty()) {
                val allowedAnchors = policy.anchors.all
                    .map(String::normalizeForSemanticMatching)
                    .toSet()
                if (anchor.normalizeForSemanticMatching() !in allowedAnchors) {
                    add("personalization_anchor_not_supplied_by_user")
                }
            }
        }

        return CardValidationResult(errors.distinct())
    }

    private fun String.normalizeForSemanticMatching(): String =
        lowercase()
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ı', 'i')
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private const val MAX_QUESTION_CHARS = 140
    private const val MAX_ALTERNATIVE_CHARS = 180

    private val GENERIC_ADVICE = setOf(
        "kendine iyi bak",
        "daha iyi hissetmeye calis",
        "daha saglikli bir secim yap",
        "baska bir sey yap",
        "uretken olmaya calis",
        "motivasyonunu bul",
        "do something else",
        "make a healthier choice",
        "try to be productive",
    )
    private val HIGH_EFFORT_ACTIONS = setOf(
        "antrenman yap",
        "spora git",
        "kosuya cik",
        "egzersiz yap",
        "work out",
        "go to the gym",
        "go for a run",
        "exercise now",
    )
    private val INVENTED_LIVE_CONTENT = setOf(
        "yeni bolumu cikti",
        "yeni bolumunu dinle",
        "en sevdigin podcastin yeni bolumu",
        "new episode is out",
        "latest episode",
        "new release",
        "bugun yayinlandi",
        "just released",
    )
    private val FORCE_STOP_LANGUAGE = setOf(
        "hemen kapat",
        "uygulamadan cik",
        "telefonu birakmak zorundasin",
        "stop immediately",
        "you must close",
        "leave the app now",
    )
}
