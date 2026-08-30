package com.thynatos.esik.ai

import com.thynatos.esik.data.PersonalizationProfile
import com.thynatos.esik.data.ProfileIntake

internal object ProfileGroundingSanitizer {
    fun sanitize(
        intake: ProfileIntake,
        generated: PersonalizationProfile,
        fallback: PersonalizationProfile,
    ): PersonalizationProfile {
        val allEvidence = listOf(
            intake.biography,
            intake.reason,
            intake.improvementArea,
            intake.hobbies.joinToString(" "),
        ).joinToString(" ")
        val activityEvidence = listOf(
            intake.biography,
            intake.hobbies.joinToString(" "),
        ).joinToString(" ")

        val goals = generated.goals
            .filter { value -> isGrounded(value, allEvidence) }
            .ifEmpty { fallback.goals }
            .distinct()
            .take(3)

        val contexts = generated.recurringContexts
            .filter { value ->
                isGrounded(value, allEvidence) ||
                    fallback.recurringContexts.any { fallbackValue ->
                        semanticallyRelated(value, fallbackValue)
                    }
            }
            .ifEmpty { fallback.recurringContexts }
            .distinct()
            .take(4)

        val activities = generated.preferredActivities
            .filter { value -> isGrounded(value, activityEvidence) }
            .ifEmpty { fallback.preferredActivities }
            .distinct()
            .take(5)

        val allowedActivityEvidence = (activityEvidence + " " + activities.joinToString(" ")).trim()
        val lowEnergy = generated.lowEnergyActivities
            .filter { value ->
                isGrounded(value, allowedActivityEvidence) ||
                    isNeutralLowEnergyAction(value)
            }
            .ifEmpty { fallback.lowEnergyActivities }
            .distinct()
            .take(3)

        val quickStates = generated.quickStates
            .filter { option ->
                option.id.isNotBlank() &&
                    option.label.isNotBlank() &&
                    option.label.length <= 70 &&
                    SafetyLanguageValidator.isDisplaySafe(option.label)
            }
            .distinctBy { it.id }
            .take(6)
            .let { values ->
                (values + fallback.quickStates)
                    .distinctBy { it.id }
                    .take(6)
            }

        return generated.copy(
            goals = goals,
            recurringContexts = contexts,
            preferredActivities = activities,
            lowEnergyActivities = lowEnergy,
            quickStates = quickStates,
        )
    }

    private fun isGrounded(value: String, evidence: String): Boolean {
        val valueTokens = value.meaningfulTokens()
        val evidenceTokens = evidence.meaningfulTokens()
        if (valueTokens.any(evidenceTokens::contains)) return true
        return SEMANTIC_GROUPS.any { group ->
            valueTokens.any(group::contains) && evidenceTokens.any(group::contains)
        }
    }

    private fun semanticallyRelated(first: String, second: String): Boolean {
        val firstTokens = first.meaningfulTokens()
        val secondTokens = second.meaningfulTokens()
        if (firstTokens.any(secondTokens::contains)) return true
        return SEMANTIC_GROUPS.any { group ->
            firstTokens.any(group::contains) && secondTokens.any(group::contains)
        }
    }

    private fun isNeutralLowEnergyAction(value: String): Boolean {
        val normalized = value.normalizeForGrounding()
        return NEUTRAL_LOW_ENERGY_CUES.any(normalized::contains)
    }

    private fun String.meaningfulTokens(): Set<String> =
        normalizeForGrounding()
            .split(' ')
            .asSequence()
            .filter { token -> token.length >= 4 }
            .filterNot(STOP_WORDS::contains)
            .toSet()

    private fun String.normalizeForGrounding(): String =
        lowercase()
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ı', 'i')
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private val STOP_WORDS = setOf(
        "daha",
        "icin",
        "gibi",
        "olan",
        "olarak",
        "biraz",
        "kendi",
        "with",
        "that",
        "this",
        "from",
        "more",
        "want",
    )
    private val SEMANTIC_GROUPS = listOf(
        setOf("muzik", "sarki", "music", "song"),
        setOf("kitap", "okumak", "okuma", "book", "read", "reading"),
        setOf("gitar", "guitar"),
        setOf("yuruyus", "yurumek", "walk", "walking"),
        setOf("kosu", "kosmak", "run", "running"),
        setOf("spor", "egzersiz", "exercise", "workout", "gym"),
        setOf("ders", "calismak", "odev", "study", "studying", "homework"),
        setOf("uyku", "uyumak", "gece", "sleep", "bedtime"),
        setOf("yorgunluk", "yorgun", "yorulmak", "tired", "fatigue", "exhausted"),
        setOf("erteleme", "ertelemek", "baslayamamak", "procrastination", "procrastinating", "avoidance"),
        setOf("sikilma", "sikilmak", "bored", "boredom"),
    )
    private val NEUTRAL_LOW_ENERGY_CUES = setOf(
        "su ic",
        "bardak su",
        "nefes",
        "gozlerini dinlendir",
        "ekrandan uzaklas",
        "telefonu birak",
        "kisa mola",
        "bulundugun ortami dinle",
        "drink water",
        "breath",
        "screen break",
        "put the phone down",
    )
}
