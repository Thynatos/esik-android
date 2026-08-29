package com.thynatos.esik.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

class JsonEsikRepository(context: Context) : EsikRepository {
    private val stateFile = File(context.filesDir, FILE_NAME)
    private val lock = Any()

    override fun loadProfile(): UserProfile? = synchronized(lock) {
        val profile = readRoot().optJSONObject(KEY_PROFILE) ?: return@synchronized null
        UserProfile(
            name = profile.optString("isim"),
            department = profile.optString("bolum"),
            hobbies = profile.optJSONArray("hobiler").toStringList(),
            improvementArea = profile.optString("gelisim_alani"),
            reason = profile.optString("neden"),
            targetAppLabel = profile.optString("hedef_app"),
            targetPackage = profile.optString("hedef_paket"),
            dailyLimitMinutes = profile.optInt("limit_dk", DEFAULT_LIMIT_MINUTES)
                .coerceAtLeast(1),
        )
    }

    override fun saveProfile(profile: UserProfile) = synchronized(lock) {
        val root = readRoot()
        root.put(
            KEY_PROFILE,
            JSONObject()
                .put("isim", profile.name)
                .put("bolum", profile.department)
                .put("hobiler", JSONArray(profile.hobbies))
                .put("gelisim_alani", profile.improvementArea)
                .put("neden", profile.reason)
                .put("hedef_app", profile.targetAppLabel)
                .put("hedef_paket", profile.targetPackage)
                .put("limit_dk", profile.dailyLimitMinutes),
        )
        ensureRecordsArray(root)
        writeRoot(root)
    }

    override fun loadRecords(): List<InterventionRecord> = synchronized(lock) {
        val records = readRoot().optJSONArray(KEY_RECORDS) ?: return@synchronized emptyList()
        buildList {
            for (index in 0 until records.length()) {
                val item = records.optJSONObject(index) ?: continue
                add(
                    InterventionRecord(
                        timestampEpochMillis = item.optLong("zaman_ms", System.currentTimeMillis()),
                        usageMinutes = item.optInt("kullanim_dk", 0).coerceAtLeast(0),
                        text = item.optString("metin"),
                        choice = UserChoice.fromStorage(item.optString("secim")),
                    ),
                )
            }
        }
    }

    override fun appendRecord(record: InterventionRecord) = synchronized(lock) {
        val root = readRoot()
        val records = ensureRecordsArray(root)
        records.put(record.toJson())
        writeRoot(root)
    }

    override fun replaceRecords(records: List<InterventionRecord>) = synchronized(lock) {
        val root = readRoot()
        root.put(KEY_RECORDS, JSONArray().apply { records.forEach { put(it.toJson()) } })
        writeRoot(root)
    }

    override fun clearAll() = synchronized(lock) {
        if (stateFile.exists() && !stateFile.delete()) {
            writeRoot(emptyRoot())
        }
    }

    private fun readRoot(): JSONObject {
        if (!stateFile.exists()) return emptyRoot()
        return runCatching {
            JSONObject(stateFile.readText(StandardCharsets.UTF_8))
        }.getOrElse { emptyRoot() }
    }

    private fun writeRoot(root: JSONObject) {
        stateFile.parentFile?.mkdirs()
        val temporary = File(stateFile.parentFile, "$FILE_NAME.tmp")
        temporary.writeText(root.toString(2), StandardCharsets.UTF_8)
        if (!temporary.renameTo(stateFile)) {
            stateFile.writeText(root.toString(2), StandardCharsets.UTF_8)
            temporary.delete()
        }
    }

    private fun emptyRoot(): JSONObject =
        JSONObject()
            .put(KEY_PROFILE, JSONObject.NULL)
            .put(KEY_RECORDS, JSONArray())

    private fun ensureRecordsArray(root: JSONObject): JSONArray {
        val existing = root.optJSONArray(KEY_RECORDS)
        if (existing != null) return existing
        return JSONArray().also { root.put(KEY_RECORDS, it) }
    }

    private fun InterventionRecord.toJson(): JSONObject =
        JSONObject()
            .put("zaman_ms", timestampEpochMillis)
            .put("saat", localTime())
            .put("kullanim_dk", usageMinutes)
            .put("metin", text)
            .put("secim", choice.storageValue)

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
            }
        }
    }

    private companion object {
        const val FILE_NAME = "esik_state.json"
        const val KEY_PROFILE = "profil"
        const val KEY_RECORDS = "kayitlar"
        const val DEFAULT_LIMIT_MINUTES = 60
    }
}
