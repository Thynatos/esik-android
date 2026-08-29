package com.thynatos.esik.ai

object SafetyLanguageValidator {
    private val blockedPhrases = setOf(
        "basarisiz oldun",
        "iraden zayif",
        "too much",
        "a lot",
        "you are addicted",
        "you failed",
        "lack of willpower",
    )
    private val blockedWords = setOf(
        "cok",
        "fazla",
        "asiri",
        "bagimlisin",
        "bagimlilik",
        "depresyon",
        "anksiyete",
        "tani",
        "teshis",
        "iradesiz",
        "excessive",
        "addicted",
        "addiction",
        "depression",
        "anxiety",
        "diagnosis",
    )

    fun isDisplaySafe(vararg fields: String): Boolean {
        val normalized = fields.joinToString(" ").normalizeForMatching()
        if (blockedPhrases.any(normalized::contains)) return false
        val words = normalized.split(Regex("[^a-z0-9]+"))
        return blockedWords.none(words::contains)
    }

    private fun String.normalizeForMatching(): String =
        lowercase()
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ı', 'i')
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
}
