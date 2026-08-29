package com.thynatos.esik.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyLanguageValidatorTest {
    @Test
    fun blocksStandaloneJudgmentWords() {
        assertFalse(SafetyLanguageValidator.isDisplaySafe("Bugün çok kullandın."))
        assertFalse(SafetyLanguageValidator.isDisplaySafe("Bu kullanım fazla olabilir."))
    }

    @Test
    fun doesNotBlockWordFragments() {
        assertTrue(
            SafetyLanguageValidator.isDisplaySafe(
                "İki dakikalık küçük bir başlangıç yapabilirsin.",
            ),
        )
    }

    @Test
    fun blocksDiagnosisAndFailureLanguage() {
        assertFalse(SafetyLanguageValidator.isDisplaySafe("Bağımlısın."))
        assertFalse(SafetyLanguageValidator.isDisplaySafe("Bu bağımlılık olabilir."))
        assertFalse(SafetyLanguageValidator.isDisplaySafe("Bu depresyon belirtisi."))
        assertFalse(SafetyLanguageValidator.isDisplaySafe("Başarısız oldun."))
    }
}
