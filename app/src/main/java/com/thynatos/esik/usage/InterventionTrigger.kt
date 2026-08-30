package com.thynatos.esik.usage

/**
 * Why an intervention record was shown. The current product creates only [THRESHOLD] interventions.
 * Legacy pattern values remain parseable so experimental records do not break local data loading.
 */
enum class InterventionTrigger(val storageValue: String) {
    THRESHOLD("threshold"),
    IMMEDIATE_REOPEN("immediate_reopen"),
    RAPID_REOPEN_LOOP("rapid_reopen_loop"),
    SESSION_DRIFT("session_drift");

    companion object {
        fun fromStorage(value: String): InterventionTrigger? =
            entries.firstOrNull { it.storageValue == value.trim().lowercase() }
    }
}
