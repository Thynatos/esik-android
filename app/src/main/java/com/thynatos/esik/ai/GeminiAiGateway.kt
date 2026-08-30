package com.thynatos.esik.ai

import android.os.SystemClock
import android.util.Log
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
    private val profileModel: String = BuildConfig.GEMINI_PROFILE_MODEL,
    private val cardModel: String = BuildConfig.GEMINI_CARD_MODEL,
    private val reportModel: String = BuildConfig.GEMINI_REPORT_MODEL,
    private val fallback: AiGateway = MockAiGateway(),
    private val client: GeminiMessageClient = GeminiMessageClient(apiKey),
) : AiGateway {
    override suspend fun generateProfile(intake: ProfileIntake): PersonalizationProfile {
        val startedAt = SystemClock.elapsedRealtime()
        if (!client.isConfigured) {
            debugResult(TASK_PROFILE, profileModel, startedAt, SOURCE_FALLBACK, "not_configured")
            return fallback.generateProfile(intake)
        }
        if (containsCrisisSignal(intake.externalText())) {
            debugResult(
                TASK_PROFILE,
                profileModel,
                startedAt,
                SOURCE_FALLBACK,
                "crisis_short_circuit",
            )
            return fallback.generateProfile(intake)
        }

        return try {
            val fallbackProfile = fallback.generateProfile(intake)
            val completion = completeJsonWithSchemaFallback(
                model = profileModel,
                systemPrompt = AiPrompts.PROFILE_SYSTEM_PROMPT,
                userPrompt = profileInputJson(intake).toString(2),
                maxTokens = 900,
                temperature = 0.15,
                responseSchema = PROFILE_RESPONSE_SCHEMA,
            )
            val profile = ProfileGroundingSanitizer.sanitize(
                intake = intake,
                generated = parseProfile(completion.text),
                fallback = fallbackProfile,
            ).withFallbackDefaults(fallbackProfile)
            debugResult(
                TASK_PROFILE,
                profileModel,
                startedAt,
                SOURCE_LIVE,
                if (completion.schemaFallbackUsed) "ok_without_schema" else "ok",
            )
            profile
        } catch (error: Exception) {
            debugResult(
                TASK_PROFILE,
                profileModel,
                startedAt,
                SOURCE_FALLBACK,
                failureCategory(error),
            )
            fallback.generateProfile(intake)
        }
    }

    override suspend fun generateCard(
        profile: UserProfile,
        currentUsageMinutes: Int,
        input: InterventionInput,
    ): AiCard {
        val startedAt = SystemClock.elapsedRealtime()
        val externalContext = listOf(
            input.text,
            profile.reason,
            profile.personalization.goals.joinToString(" "),
            profile.personalization.preferredActivities.joinToString(" "),
            profile.personalization.lowEnergyActivities.joinToString(" "),
        ).joinToString(" ")
        if (!client.isConfigured) {
            debugResult(TASK_CARD, cardModel, startedAt, SOURCE_FALLBACK, "not_configured")
            return fallback.generateCard(profile, currentUsageMinutes, input)
        }
        if (containsCrisisSignal(externalContext)) {
            debugResult(
                TASK_CARD,
                cardModel,
                startedAt,
                SOURCE_FALLBACK,
                "crisis_short_circuit",
            )
            return fallback.generateCard(profile, currentUsageMinutes, input)
        }

        val policy = InterventionContextBuilder.build(profile, input)
        return try {
            val completion = completeJsonWithSchemaFallback(
                model = cardModel,
                systemPrompt = AiPrompts.CARD_SYSTEM_PROMPT,
                userPrompt = cardInputJson(profile, currentUsageMinutes, input, policy).toString(2),
                maxTokens = 480,
                temperature = 0.15,
                responseSchema = CARD_RESPONSE_SCHEMA,
            )
            val firstCard = runCatching { parseStructuredCard(completion.text) }.getOrNull()
            val firstValidation = firstCard?.let { AiCardSemanticValidator.validate(it, policy) }
                ?: CardValidationResult(listOf("structured_card_parse_failed"))

            if (firstCard != null && firstValidation.isValid) {
                debugResult(
                    TASK_CARD,
                    cardModel,
                    startedAt,
                    SOURCE_LIVE,
                    if (completion.schemaFallbackUsed) "ok_without_schema" else "ok",
                )
                firstCard.toVisibleCard()
            } else {
                val repaired = repairCard(
                    invalidResponse = completion.text,
                    policy = policy,
                    validationErrors = firstValidation.errors,
                )
                if (repaired != null) {
                    debugResult(TASK_CARD, cardModel, startedAt, SOURCE_REPAIRED, "ok")
                    repaired.toVisibleCard()
                } else {
                    debugResult(
                        TASK_CARD,
                        cardModel,
                        startedAt,
                        SOURCE_FALLBACK,
                        firstValidation.errors.firstOrNull() ?: "invalid_live_output",
                    )
                    fallback.generateCard(profile, currentUsageMinutes, input)
                }
            }
        } catch (error: Exception) {
            debugResult(
                TASK_CARD,
                cardModel,
                startedAt,
                SOURCE_FALLBACK,
                failureCategory(error),
            )
            fallback.generateCard(profile, currentUsageMinutes, input)
        }
    }

    override suspend fun generateDailyReport(
        profile: UserProfile,
        records: List<InterventionRecord>,
        currentUsageMinutes: Int,
    ): DailyReport {
        val startedAt = SystemClock.elapsedRealtime()
        val externalContext = buildString {
            append(profile.reason)
            append(' ')
            append(profile.personalization.goals.joinToString(" "))
            records.forEach { record ->
                append(' ')
                append(record.text)
            }
        }
        if (records.size < REPORT_MINIMUM_RECORDS) {
            debugResult(TASK_REPORT, reportModel, startedAt, SOURCE_FALLBACK, "insufficient_data")
            return fallback.generateDailyReport(profile, records, currentUsageMinutes)
        }
        if (!client.isConfigured) {
            debugResult(TASK_REPORT, reportModel, startedAt, SOURCE_FALLBACK, "not_configured")
            return fallback.generateDailyReport(profile, records, currentUsageMinutes)
        }
        if (containsCrisisSignal(externalContext)) {
            debugResult(
                TASK_REPORT,
                reportModel,
                startedAt,
                SOURCE_FALLBACK,
                "crisis_short_circuit",
            )
            return fallback.generateDailyReport(profile, records, currentUsageMinutes)
        }

        val evidence = DailyReportEvidenceBuilder.build(records)
        return try {
            val completion = completeJsonWithSchemaFallback(
                model = reportModel,
                systemPrompt = AiPrompts.REPORT_SYSTEM_PROMPT,
                userPrompt = reportInputJson(
                    profile = profile,
                    records = records,
                    currentUsageMinutes = currentUsageMinutes,
                    evidence = evidence,
                ).toString(2),
                maxTokens = 520,
                temperature = 0.1,
                responseSchema = REPORT_RESPONSE_SCHEMA,
            )
            val reflection = parseDailyReflection(completion.text)
            val validation = DailyReportSemanticValidator.validate(reflection, evidence)
            if (!validation.isValid) {
                debugResult(
                    TASK_REPORT,
                    reportModel,
                    startedAt,
                    SOURCE_FALLBACK,
                    validation.errors.firstOrNull() ?: "invalid_report_output",
                )
                fallback.generateDailyReport(profile, records, currentUsageMinutes)
            } else {
                val continued = records.count { it.choice == UserChoice.CONTINUE }
                val stopped = records.count { it.choice == UserChoice.STOPPED }
                debugResult(
                    TASK_REPORT,
                    reportModel,
                    startedAt,
                    SOURCE_LIVE,
                    if (completion.schemaFallbackUsed) "ok_without_schema" else "ok",
                )
                DailyReport(
                    totalUsageMinutes = currentUsageMinutes,
                    limitMinutes = profile.dailyLimitMinutes,
                    interventionCount = records.size,
                    continuedCount = continued,
                    stoppedCount = stopped,
                    observationQuestion = reflection.observationQuestion,
                    microStep = reflection.microStep,
                )
            }
        } catch (error: Exception) {
            debugResult(
                TASK_REPORT,
                reportModel,
                startedAt,
                SOURCE_FALLBACK,
                failureCategory(error),
            )
            fallback.generateDailyReport(profile, records, currentUsageMinutes)
        }
    }

    private suspend fun repairCard(
        invalidResponse: String,
        policy: InterventionPolicy,
        validationErrors: List<String>,
    ): StructuredAiCard? = runCatching {
        val completion = completeJsonWithSchemaFallback(
            model = cardModel,
            systemPrompt = AiPrompts.CARD_REPAIR_SYSTEM_PROMPT,
            userPrompt = repairInputJson(
                invalidResponse = invalidResponse,
                policy = policy,
                validationErrors = validationErrors,
            ).toString(2),
            maxTokens = 420,
            temperature = 0.0,
            responseSchema = CARD_RESPONSE_SCHEMA,
        )
        val repaired = parseStructuredCard(completion.text)
        repaired.takeIf { AiCardSemanticValidator.validate(it, policy).isValid }
    }.getOrNull()

    private suspend fun completeJsonWithSchemaFallback(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Double,
        responseSchema: JSONObject,
    ): JsonCompletion = try {
        JsonCompletion(
            text = client.complete(
                model = model,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = maxTokens,
                temperature = temperature,
                responseSchema = responseSchema,
            ),
            schemaFallbackUsed = false,
        )
    } catch (error: GeminiApiException) {
        if (!error.mayBeSchemaCompatibilityFailure) throw error
        JsonCompletion(
            text = client.complete(
                model = model,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = maxTokens,
                temperature = temperature,
                responseSchema = null,
            ),
            schemaFallbackUsed = true,
        )
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

    private fun parseStructuredCard(raw: String): StructuredAiCard {
        val json = extractJson(raw)
        val need = InterventionNeed.fromWire(json.optString("need"))
            ?: throw GeminiApiException("Card need is invalid")
        val strategy = InterventionStrategy.fromWire(json.optString("strategy"))
            ?: throw GeminiApiException("Card strategy is invalid")
        val question = json.optString("question").trim()
        val alternative = json.optString("alternative").trim()
        val durationMinutes = json.optInt("duration_minutes", -1)
        val personalizationAnchor = json.optString("personalization_anchor").trim()
        if (question.isBlank() || alternative.isBlank() || durationMinutes < 1) {
            throw GeminiApiException("Card JSON is incomplete")
        }
        return StructuredAiCard(
            need = need,
            strategy = strategy,
            question = question,
            alternative = alternative,
            durationMinutes = durationMinutes,
            personalizationAnchor = personalizationAnchor,
        )
    }

    private fun parseDailyReflection(raw: String): StructuredDailyReflection {
        val json = extractJson(raw)
        val observation = json.optString("observation_question").trim()
        val microStep = json.optString("micro_step").trim()
        if (observation.isBlank() || microStep.isBlank()) {
            throw GeminiApiException("Report JSON is incomplete")
        }
        return StructuredDailyReflection(
            evidenceStateId = json.optString("evidence_state_id").trim(),
            observationQuestion = observation,
            microStep = microStep,
        )
    }

    private fun StructuredAiCard.toVisibleCard(): AiCard =
        AiCard(question = question, alternative = alternative)

    private fun extractJson(raw: String): JSONObject {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw GeminiApiException(
                message = "No JSON object in response",
                kind = GeminiFailureKind.INVALID_OUTPUT,
            )
        }
        return runCatching { JSONObject(raw.substring(start, end + 1)) }
            .getOrElse {
                throw GeminiApiException(
                    message = "Invalid output JSON",
                    kind = GeminiFailureKind.INVALID_OUTPUT,
                )
            }
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
        policy: InterventionPolicy,
    ): JSONObject = JSONObject()
        .put("local_time", Instant.now().atZone(ZoneId.systemDefault()).toLocalTime().toString())
        .put("target_app", profile.targetAppLabel)
        .put("usage_minutes", currentUsageMinutes)
        .put("user_defined_limit_minutes", profile.dailyLimitMinutes)
        .put("selected_state_id", input.stateId)
        .put("selected_state_label", input.stateLabel)
        .put("user_text", input.text.take(MAX_USER_TEXT_CHARS))
        .put("input_method", input.method.storageValue)
        .put("compiled_policy", policy.toJson())

    private fun repairInputJson(
        invalidResponse: String,
        policy: InterventionPolicy,
        validationErrors: List<String>,
    ): JSONObject = JSONObject()
        .put("compiled_policy", policy.toJson())
        .put("invalid_response", invalidResponse.take(MAX_INVALID_RESPONSE_CHARS))
        .put("validation_errors", JSONArray(validationErrors.take(MAX_VALIDATION_ERRORS)))

    private fun InterventionPolicy.toJson(): JSONObject = JSONObject()
        .put("resolved_state_id", resolvedStateId)
        .put("need", need.wireValue)
        .put("energy", energy.wireValue)
        .put("objective", objective.wireValue)
        .put(
            "allowed_strategies",
            JSONArray(allowedStrategies.map(InterventionStrategy::wireValue).sorted()),
        )
        .put("max_duration_minutes", maxDurationMinutes)
        .put(
            "anchors",
            JSONObject()
                .put("goals", JSONArray(anchors.goals))
                .put("activities", JSONArray(anchors.activities))
                .put("low_energy_activities", JSONArray(anchors.lowEnergyActivities)),
        )
        .put("forbidden_patterns", JSONArray(forbiddenPatterns))
        .put("evidence_summary", evidenceSummary)

    private fun reportInputJson(
        profile: UserProfile,
        records: List<InterventionRecord>,
        currentUsageMinutes: Int,
        evidence: DailyReportEvidence,
    ): JSONObject = JSONObject()
        .put("target_app", profile.targetAppLabel)
        .put("usage_minutes", currentUsageMinutes)
        .put("user_defined_limit_minutes", profile.dailyLimitMinutes)
        .put("intervention_count", records.size)
        .put("continued_count", records.count { it.choice == UserChoice.CONTINUE })
        .put("stopped_count", records.count { it.choice == UserChoice.STOPPED })
        .put("goals", JSONArray(profile.personalization.goals))
        .put("evidence_summary", evidence.toJson())
        .put(
            "records",
            JSONArray().apply {
                records.takeLast(MAX_REPORT_RECORDS).forEach { record ->
                    put(
                        JSONObject()
                            .put("time", record.localTime())
                            .put("state_id", record.stateId)
                            .put("state", record.stateLabel)
                            .put("text", record.text.take(MAX_REPORT_TEXT_CHARS))
                            .put("choice", record.choice.storageValue)
                            .put("alternative", record.aiAlternative.take(MAX_REPORT_TEXT_CHARS)),
                    )
                }
            },
        )

    private fun DailyReportEvidence.toJson(): JSONObject = JSONObject()
        .put("candidate_state_ids", JSONArray(candidateStateIds.toList().sorted()))
        .put("dominant_state_id", dominantStateId.orEmpty())
        .put("higher_continue_state_id", higherContinueStateId.orEmpty())
        .put(
            "states",
            JSONArray().apply {
                states.forEach { state ->
                    put(
                        JSONObject()
                            .put("state_id", state.stateId)
                            .put("state_label", state.stateLabel)
                            .put("count", state.count)
                            .put("continued_count", state.continuedCount)
                            .put("stopped_count", state.stoppedCount),
                    )
                }
            },
        )
        .put(
            "time_bucket_counts",
            JSONObject().apply {
                timeBucketCounts.forEach { (bucket, count) -> put(bucket, count) }
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

    private fun failureCategory(error: Exception): String = when (error) {
        is GeminiApiException -> error.kind.name.lowercase()
        else -> error::class.java.simpleName.ifBlank { "unexpected_error" }
    }

    private fun debugResult(
        task: String,
        model: String,
        startedAt: Long,
        source: String,
        outcome: String,
    ) {
        if (!BuildConfig.DEBUG) return
        val elapsed = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
        Log.d(
            LOG_TAG,
            "task=$task model=$model source=$source outcome=$outcome elapsed_ms=$elapsed",
        )
    }

    private data class JsonCompletion(
        val text: String,
        val schemaFallbackUsed: Boolean,
    )

    private companion object {
        const val LOG_TAG = "EsikAi"
        const val TASK_PROFILE = "profile"
        const val TASK_CARD = "card"
        const val TASK_REPORT = "report"
        const val SOURCE_LIVE = "live"
        const val SOURCE_REPAIRED = "repaired"
        const val SOURCE_FALLBACK = "local_fallback"

        val NON_ID_CHARS = Regex("[^a-z0-9]+")
        const val REPORT_MINIMUM_RECORDS = 7
        const val MAX_USER_TEXT_CHARS = 2_000
        const val MAX_PROFILE_ITEM_CHARS = 120
        const val MAX_REPORT_RECORDS = 30
        const val MAX_REPORT_TEXT_CHARS = 240
        const val MAX_INVALID_RESPONSE_CHARS = 2_000
        const val MAX_VALIDATION_ERRORS = 8

        val CARD_RESPONSE_SCHEMA: JSONObject = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put(
                "properties",
                JSONObject()
                    .put(
                        "need",
                        JSONObject()
                            .put("type", "string")
                            .put(
                                "enum",
                                JSONArray(InterventionNeed.entries.map(InterventionNeed::wireValue)),
                            ),
                    )
                    .put(
                        "strategy",
                        JSONObject()
                            .put("type", "string")
                            .put(
                                "enum",
                                JSONArray(
                                    InterventionStrategy.entries.map(InterventionStrategy::wireValue),
                                ),
                            ),
                    )
                    .put("question", JSONObject().put("type", "string"))
                    .put("alternative", JSONObject().put("type", "string"))
                    .put(
                        "duration_minutes",
                        JSONObject()
                            .put("type", "integer")
                            .put("minimum", 1)
                            .put("maximum", 10),
                    )
                    .put("personalization_anchor", JSONObject().put("type", "string")),
            )
            .put(
                "required",
                JSONArray(
                    listOf(
                        "need",
                        "strategy",
                        "question",
                        "alternative",
                        "duration_minutes",
                        "personalization_anchor",
                    ),
                ),
            )

        val REPORT_RESPONSE_SCHEMA: JSONObject = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put(
                "properties",
                JSONObject()
                    .put("evidence_state_id", JSONObject().put("type", "string"))
                    .put("observation_question", JSONObject().put("type", "string"))
                    .put("micro_step", JSONObject().put("type", "string")),
            )
            .put(
                "required",
                JSONArray(
                    listOf(
                        "evidence_state_id",
                        "observation_question",
                        "micro_step",
                    ),
                ),
            )

        val PROFILE_RESPONSE_SCHEMA: JSONObject = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put(
                "properties",
                JSONObject()
                    .put(
                        "goals",
                        JSONObject()
                            .put("type", "array")
                            .put("items", JSONObject().put("type", "string")),
                    )
                    .put(
                        "recurring_contexts",
                        JSONObject()
                            .put("type", "array")
                            .put("items", JSONObject().put("type", "string")),
                    )
                    .put(
                        "preferred_activities",
                        JSONObject()
                            .put("type", "array")
                            .put("items", JSONObject().put("type", "string")),
                    )
                    .put(
                        "low_energy_activities",
                        JSONObject()
                            .put("type", "array")
                            .put("items", JSONObject().put("type", "string")),
                    )
                    .put(
                        "tone",
                        JSONObject()
                            .put("type", "string")
                            .put(
                                "enum",
                                JSONArray(listOf("supportive_direct", "gentle", "practical")),
                            ),
                    )
                    .put(
                        "quick_states",
                        JSONObject()
                            .put("type", "array")
                            .put(
                                "items",
                                JSONObject()
                                    .put("type", "object")
                                    .put("additionalProperties", false)
                                    .put(
                                        "properties",
                                        JSONObject()
                                            .put("id", JSONObject().put("type", "string"))
                                            .put("label", JSONObject().put("type", "string"))
                                            .put("emoji", JSONObject().put("type", "string"))
                                            .put("category", JSONObject().put("type", "string")),
                                    )
                                    .put(
                                        "required",
                                        JSONArray(listOf("id", "label", "emoji", "category")),
                                    ),
                            ),
                    ),
            )
            .put(
                "required",
                JSONArray(
                    listOf(
                        "goals",
                        "recurring_contexts",
                        "preferred_activities",
                        "low_energy_activities",
                        "tone",
                        "quick_states",
                    ),
                ),
            )
    }
}
