package com.thynatos.esik.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small REST client for the Gemini generateContent endpoint.
 *
 * This direct-to-provider client exists only for the hackathon prototype. A production mobile app
 * must call a backend proxy instead of embedding a long-lived provider credential in the APK.
 */
class GeminiMessageClient(
    private val apiKey: String,
    private val endpointBase: String = DEFAULT_ENDPOINT_BASE,
    private val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun complete(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
    ): String = withContext(Dispatchers.IO) {
        check(isConfigured) { "Gemini API key is not configured" }
        check(model.isNotBlank()) { "Gemini model is not configured" }

        val connection = (URL("$endpointBase/$model:generateContent").openConnection()
            as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        try {
            val request = JSONObject()
                .put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemPrompt)),
                    ),
                )
                .put(
                    "contents",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", userPrompt)),
                            ),
                    ),
                )
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("temperature", 0.2)
                        .put("maxOutputTokens", maxTokens.coerceIn(64, 2_048))
                        .put("responseMimeType", "application/json"),
                )

            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(request.toString())
            }

            val status = connection.responseCode
            val responseBody = (if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                throw GeminiApiException("Gemini HTTP $status")
            }

            val response = JSONObject(responseBody)
            val blockReason = response
                .optJSONObject("promptFeedback")
                ?.optString("blockReason")
                .orEmpty()
            if (blockReason.isNotBlank()) {
                throw GeminiApiException("Gemini prompt blocked: $blockReason")
            }

            val candidate = response.optJSONArray("candidates")?.optJSONObject(0)
                ?: throw GeminiApiException("Gemini response has no candidate")
            val finishReason = candidate.optString("finishReason")
            if (finishReason in BLOCKED_FINISH_REASONS) {
                throw GeminiApiException("Gemini generation stopped: $finishReason")
            }

            val parts = candidate
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?: throw GeminiApiException("Gemini response has no content")
            val text = buildString {
                for (index in 0 until parts.length()) {
                    val part = parts.optJSONObject(index) ?: continue
                    append(part.optString("text"))
                }
            }.trim()

            if (text.isBlank()) {
                throw GeminiApiException("Gemini response contains no text")
            }
            text
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val DEFAULT_ENDPOINT_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models"
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 6_000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 12_000

        val BLOCKED_FINISH_REASONS = setOf(
            "SAFETY",
            "RECITATION",
            "BLOCKLIST",
            "PROHIBITED_CONTENT",
            "SPII",
            "MAX_TOKENS",
            "MALFORMED_FUNCTION_CALL",
        )
    }
}

class GeminiApiException(message: String) : IllegalStateException(message)
