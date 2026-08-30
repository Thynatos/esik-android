package com.thynatos.esik.ai

import com.thynatos.esik.BuildConfig
import com.thynatos.esik.data.AiCard
import com.thynatos.esik.data.DailyReport
import com.thynatos.esik.data.InterventionInput
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.PersonalizationDefaults
import com.thynatos.esik.data.PersonalizationProfile
import com.thynatos.esik.data.ProfileIntake
import com.thynatos.esik.data.ProfileTone
import com.thynatos.esik.data.QuickStateOption
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/**
 * Demo-only direct Gemini integration.
 *
 * The key is read from local BuildConfig values and is never committed. A production build must
 * replace this client with a backend proxy, server-held credentials, abuse controls, and an
 * explicit logging/retention policy.
 */
class GeminiAiGateway(
    apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val fastModel: String = BuildConfig.GEMINI_FAST_MODEL,
    private val reportModel: String = BuildConfig.GEMINI_REPORT_MODEL,
    private val fallback: AiGateway = MockAiGateway(),
    private val client: GeminiMessageClient = GeminiMessageClient(apiKey),
) : AiGateway {
    override suspend fun generateProfile(intake: ProfileIntake): PersonalizationProfile {
        if (!client.isConfigured || containsCrisisSignal(intake.externalText())) {
            return fallback.generateProfile(intake)
        }

        return try {
            val raw = client.complete(
                model = fastModel,
                systemPrompt = AiPrompts.PROFILE_SYSTEM_PROMPT,
                userPrompt = profileInputJson(intake).toString(2),
                maxTokens = 900,
            )
            parseProfile(raw).withFallbackDefaults(fallback.generateProfile(intake))
        } catch (_: Exception) {
            fallback.generateProfile(intake)
        }
    }

    override suspend fun generateCard(
        profile: UserProfile,
        currentUsageMinutes: Int,
        input: InterventionInput,
    ): AiCard {
        val externalContext = listOf(
            input.text,
            profile.reason,
            profile.personalization.goals.joinToString(" "),
            profile.personalization.preferredActivities.joinToString(" "),
            profile.personalization.lowEnergyActivities.joinToString(" "),
        ).joinToString(" ")
        if (!client.isConfigured || containsCrisisSignal(externalContext)) {
            return fallback.generateCard(profile, currentUsageMinutes, input)
        }

        return try {
            val raw = client.complete(
                model = fastModel,
                systemPrompt = AiPrompts.CARD_SYSTEM_PROMPT,
                userPrompt = cardInputJson(profile, currentUsageMinutes, input).toString(2),
                maxTokens = 420,
            )
            val parsed = parseCard(raw)
            if (SafetyLanguageValidator.isDisplaySafe(parsed.question, parsed.alternative)) {
                parsed
            } else {
                fallback.generateCard(profile, currentUsageMinutes, input)
            }
        } catch (_: Exception) {
            fallback.generateCard(profile, currentUsageMinutes, input)
        }
    }

    override suspend fun generateDailyReport(
        profile: UserProfile,
        records: List<InterventionRecord>,
        currentUsageMinutes: Int,
    ): DailyReport {
        val externalContext = buildString {
            append(profile.reason)
            append(' ')
            append(profile.personalization.goals.joinToString(" "))
            records.forEach { record ->
                append(' ')
                append(record.text)
            }
        }
        if (
            records.size < REPORT_MINIMUM_RECORDS ||
            !client.isConfigured ||
            containsCrisisSignal(externalContext)
        ) {
            return fallback.generateDailyReport(profile, records, currentUsageMinutes)
        }

        return try {
            val raw = client.complete(
                model = reportModel,
                systemPrompt = AiPrompts.REPORT_SYSTEM_PROMPT,
                userPrompt = reportInputJson(profile, records, currentUsageMinutes).toString(2),
                maxTokens = 2_048,
            )
            val content = extractJson(raw)
            val observation = content.optString("observation_question").trim().take(220)
            val microStep = content.optString("micro_step").trim().take(220)
            if (
                observation.isBlank() ||
                microStep.isBlank() ||
                !SafetyLanguageValidator.isDisplaySafe(observation, microStep)
            ) {
                fallback.generateDailyReport(profile, records, currentUsageMinutes)
            } else {
                val continued = records.count { it.choice == UserChoice.CONTINUE }
                val stopped = records.count { it.choice == UserChoice.STOPPED }
                DailyReport(
                    totalUsageMinutes = currentUsageMinutes,
                    limitMinutes = profile.dailyLimitMinutes,
                    interventionCount = records.size,
                    continuedCount = continued,
                    stoppedCount = stopped,
                    observationQuestion = observation,
                    microStep = microStep,
                )
            }
        } catch (_: Exception) {
            fallback.generateDailyReport(profile, records, currentUsageMinutes)
        }
    }

    private fun parseProfile(raw: String): PersonalizationProfile {
        val json = extractJson(raw)
        val quickStates = json.optJSONArray("quick_states").toQuickStates()
        val completedStates = (quickStates + PersonalizationDefaults.quickStates)
            .filter { it.id.isNotBlank() && it.label.isNotBlank() }
            .distinctBy(QuickStateOption::id)
            .take(6)

        return PersonalizationProfile(
            goals = json.optJSONArray("goals").toSafeStringList(maxItems = 3),
            recurringContexts = json.optJSONArray("recurring_contexts")
                .toSafeStringList(maxItems = 4),
            preferredActivities = json.optJSONArray("preferred_activities")
                .toSafeStringList(maxItems = 5),
            lowEnergyActivities = json.optJSONArray("low_energy_activities")
                .toSafeStringList(maxItems = 3),
            quickStates = completedStates,
            tone = ProfileTone.fromStorage(json.optString("tone")),
        )
    }

    private fun PersonalizationProfile.withFallbackDefaults(
        fallbackProfile: PersonalizationProfile,
    ): PersonalizationProfile = copy(
        goals = goals.ifEmpty { fallbackProfile.goals },
        recurringContexts = recurringContexts.ifEmpty { fallbackProfile.recurringContexts },
        preferredActivities = preferredActivities.ifEmpty { fallbackProfile.preferredActivities },
        lowEnergyActivities = lowEnergyActivities.ifEmpty { fallbackProfile.lowEnergyActivities },
        quickStates = quickStates.ifEmpty { fallbackProfile.quickStates },
    )

    private fun parseCard(raw: String): AiCard {
        val json = extractJson(raw)
        val question = json.optString("question").trim().take(180)
        val alternative = json.optString("alternative").trim().take(240)
        if (question.isBlank() || alternative.isBlank()) {
            throw GeminiApiException("Card JSON is incomplete")
        }
        return AiCard(question = question, alternative = alternative)
    }

    private fun extractJson(raw: String): JSONObject {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) throw GeminiApiException("No JSON object in response")
        return JSONObject(raw.substring(start, end + 1))
    }

    private fun profileInputJson(intake: ProfileIntake): JSONObject =
        JSONObject()
            .put("name", intake.name)
            .put("department", intake.department)
            .put("biography", intake.biography.take(MAX_USER_TEXT_CHARS))
            .put("explicit_hobbies", JSONArray(intake.hobbies))
            .put("explicit_improvement_area", intake.improvementArea)
            .put("explicit_reason", intake.reason)

    private fun cardInputJson(
        profile: UserProfile,
        currentUsageMinutes: Int,
        input: InterventionInput,
    ): JSONObject = JSONObject()
        .put("local_time", Instant.now().atZone(ZoneId.systemDefault()).toLocalTime().toString())
        .put("target_app", profile.targetAppLabel)
        .put("usage_minutes", currentUsageMinutes)
        .put("user_limit_minutes", profile.dailyLimitMinutes)
        .put("user_reason", profile.reason)
        .put("goals", JSONArray(profile.personalization.goals))
        .put("preferred_activities", JSONArray(profile.personalization.preferredActivities))
        .put("low_energy_activities", JSONArray(profile.personalization.lowEnergyActivities))
        .put("selected_state_id", input.stateId)
        .put("selected_state_label", input.stateLabel)
        .put("user_text", input.text.take(MAX_USER_TEXT_CHARS))
        .put("input_method", input.method.storageValue)

    private fun reportInputJson(
        profile: UserProfile,
        records: List<InterventionRecord>,
        currentUsageMinutes: Int,
    ): JSONObject = JSONObject()
        .put("target_app", profile.targetAppLabel)
        .put("usage_minutes", currentUsageMinutes)
        .put("user_limit_minutes", profile.dailyLimitMinutes)
        .put("intervention_count", records.size)
        .put("continued_count", records.count { it.choice == UserChoice.CONTINUE })
        .put("stopped_count", records.count { it.choice == UserChoice.STOPPED })
        .put("goals", JSONArray(profile.personalization.goals))
        .put(
            "records",
            JSONArray().apply {
                records.takeLast(MAX_REPORT_RECORDS).forEach { record ->
                    put(
                        JSONObject()
                            .put("time", record.localTime())
                            .put("state", record.stateLabel)
                            .put("text", record.text.take(MAX_REPORT_TEXT_CHARS))
                            .put("choice", record.choice.storageValue)
                            .put("alternative", record.aiAlternative.take(MAX_REPORT_TEXT_CHARS)),
                    )
                }
            },
        )

    private fun JSONArray?.toSafeStringList(maxItems: Int): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val value = optString(index).trim().take(MAX_PROFILE_ITEM_CHARS)
                if (value.isNotBlank() && SafetyLanguageValidator.isDisplaySafe(value)) {
                    add(value)
                }
                if (size >= maxItems) break
            }
        }.distinct()
    }

    private fun JSONArray?.toQuickStates(): List<QuickStateOption> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id").trim().lowercase().replace(NON_ID_CHARS, "_")
                    .trim('_')
                    .take(40)
                val label = item.optString("label").trim().take(70)
                if (
                    id.isBlank() ||
                    label.isBlank() ||
                    !SafetyLanguageValidator.isDisplaySafe(label)
                ) {
                    continue
                }
                add(
                    QuickStateOption(
                        id = id,
                        label = label,
                        emoji = item.optString("emoji").trim().take(4),
                        category = item.optString("category", id).trim().take(40),
                    ),
                )
                if (size >= 6) break
            }
        }
    }

    private fun ProfileIntake.externalText(): String = listOf(
        department,
        biography,
        hobbies.joinToString(" "),
        improvementArea,
        reason,
    ).joinToString(" ")

    private fun containsCrisisSignal(text: String): Boolean =
        CrisisFilter.check(text).isCrisisSignal

    private companion object {
        val NON_ID_CHARS = Regex("[^a-z0-9]+")
        const val REPORT_MINIMUM_RECORDS = 7
        const val MAX_USER_TEXT_CHARS = 2_000
        const val MAX_PROFILE_ITEM_CHARS = 120
        const val MAX_REPORT_RECORDS = 30
        const val MAX_REPORT_TEXT_CHARS = 240
    }
}
