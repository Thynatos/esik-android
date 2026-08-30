package com.thynatos.esik.ai

import com.thynatos.esik.data.PersonalizationProfile
import com.thynatos.esik.data.ProfileIntake
import com.thynatos.esik.data.QuickStateOption
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileGroundingSanitizerTest {
    private val fallback = PersonalizationProfile(
        goals = listOf("telefonu daha bilinçli kullanmak"),
        recurringContexts = listOf("alışkanlıkla açma"),
        preferredActivities = emptyList(),
        lowEnergyActivities = listOf("bir bardak su içip ekrandan uzaklaşmak"),
        quickStates = listOf(
            QuickStateOption("habit", "Alışkanlıkla açtım"),
            QuickStateOption("tired", "Biraz yoruldum"),
            QuickStateOption("relaxing", "Sadece kafa dağıtıyorum"),
            QuickStateOption("bored", "Biraz sıkıldım"),
            QuickStateOption("waiting", "Bir şeyi bekliyorum"),
            QuickStateOption("procrastinating", "Bir şeyi erteliyorum"),
        ),
    )

    @Test
    fun keepsActivitiesGroundedInNarrative() {
        val result = ProfileGroundingSanitizer.sanitize(
            intake = ProfileIntake(
                name = "Ayşe",
                biography = "Müzik dinlemeyi ve gitar çalmayı seviyorum.",
                hobbies = listOf("gitar", "müzik"),
            ),
            generated = PersonalizationProfile(
                preferredActivities = listOf("gitar çalmak", "müzik dinlemek"),
            ),
            fallback = fallback,
        )

        assertTrue(result.preferredActivities.contains("gitar çalmak"))
        assertTrue(result.preferredActivities.contains("müzik dinlemek"))
    }

    @Test
    fun removesInventedPodcastAndRunningPreferences() {
        val result = ProfileGroundingSanitizer.sanitize(
            intake = ProfileIntake(
                name = "Ayşe",
                biography = "Kitap okumayı seviyorum.",
                hobbies = listOf("kitap"),
            ),
            generated = PersonalizationProfile(
                preferredActivities = listOf("kitap okumak", "podcast dinlemek", "koşu"),
            ),
            fallback = fallback,
        )

        assertTrue(result.preferredActivities.contains("kitap okumak"))
        assertFalse(result.preferredActivities.contains("podcast dinlemek"))
        assertFalse(result.preferredActivities.contains("koşu"))
    }

    @Test
    fun permitsNeutralLowEnergyActionsWithoutInventingPreference() {
        val result = ProfileGroundingSanitizer.sanitize(
            intake = ProfileIntake(
                name = "Ayşe",
                biography = "Derslere başlamakta zorlanıyorum.",
            ),
            generated = PersonalizationProfile(
                lowEnergyActivities = listOf(
                    "bir bardak su içmek",
                    "en sevdiği podcasti dinlemek",
                ),
            ),
            fallback = fallback,
        )

        assertTrue(result.lowEnergyActivities.contains("bir bardak su içmek"))
        assertFalse(result.lowEnergyActivities.contains("en sevdiği podcasti dinlemek"))
    }

    @Test
    fun sparseEvidenceUsesFallbackInsteadOfFabricatedSpecificity() {
        val result = ProfileGroundingSanitizer.sanitize(
            intake = ProfileIntake(name = "Ayşe", biography = "Telefonumu azaltmak istiyorum."),
            generated = PersonalizationProfile(
                goals = listOf("maraton koşmak"),
                preferredActivities = listOf("meditasyon"),
            ),
            fallback = fallback,
        )

        assertTrue(result.goals.contains("telefonu daha bilinçli kullanmak"))
        assertTrue(result.preferredActivities.isEmpty())
    }
}
