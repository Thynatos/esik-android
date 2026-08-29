package com.thynatos.esik.ai

data class CrisisCheck(
    val isCrisisSignal: Boolean,
    val matchedTerm: String? = null,
)

object CrisisFilter {
    private val normalizedTerms = listOf(
        "intihar",
        "kendimi oldur",
        "kendime zarar",
        "yasamak istemiyorum",
        "olmek istiyorum",
        "suicide",
        "kill myself",
        "hurt myself",
        "self harm",
        "do not want to live",
    )

    fun check(text: String): CrisisCheck {
        val normalized = text.normalizeForMatching()
        val match = normalizedTerms.firstOrNull(normalized::contains)
        return CrisisCheck(isCrisisSignal = match != null, matchedTerm = match)
    }

    private fun String.normalizeForMatching(): String =
        lowercase()
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ı', 'i')
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
            .replace(Regex("\\s+"), " ")
            .trim()
}
