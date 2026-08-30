package com.thynatos.esik.data

interface EsikRepository {
    fun loadProfile(): UserProfile?
    fun saveProfile(profile: UserProfile)
    fun loadRecords(): List<InterventionRecord>
    fun appendRecord(record: InterventionRecord)
    fun replaceRecords(records: List<InterventionRecord>)

    /**
     * Records what the user reported about one earlier moment, identified by its local timestamp.
     *
     * Returns true when a matching record was still awaiting an answer. An outcome is written once
     * and never overwritten, so a late duplicate answer cannot rewrite history.
     */
    fun updateRecordOutcome(
        timestampEpochMillis: Long,
        outcome: InterventionOutcome,
        reportedAtMillis: Long,
    ): Boolean

    fun clearAll()
}
