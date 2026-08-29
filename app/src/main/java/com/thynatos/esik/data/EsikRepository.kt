package com.thynatos.esik.data

interface EsikRepository {
    fun loadProfile(): UserProfile?
    fun saveProfile(profile: UserProfile)
    fun loadRecords(): List<InterventionRecord>
    fun appendRecord(record: InterventionRecord)
    fun replaceRecords(records: List<InterventionRecord>)
    fun clearAll()
}
