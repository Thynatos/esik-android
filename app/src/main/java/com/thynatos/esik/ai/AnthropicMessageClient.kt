package com.thynatos.esik.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AnthropicMessageClient(
    private val apiKey: String,
    private val endpoint: String = DEFAULT_ENDPOINT,
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
        check(isConfigured) { "Anthropic API key is not configured" }
        check(model.isNotBlank()) { "Anthropic model is not configured" }

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            setRequestProperty("content-type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
        }

        try {
            val request = JSONObject()
                .put("model", model)
                .put("max_tokens", maxTokens.coerceIn(64, 2_048))
                .put("system", systemPrompt)
                .put(
                    "messages",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", userPrompt),
                    ),
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
                throw AnthropicApiException("Anthropic HTTP $status")
            }

            val response = JSONObject(responseBody)
            if (response.optString("stop_reason") == "refusal") {
                throw AnthropicApiException("Anthropic response was refused")
            }

            val content = response.optJSONArray("content")
                ?: throw AnthropicApiException("Anthropic response has no content")
            val text = buildString {
                for (index in 0 until content.length()) {
                    val block = content.optJSONObject(index) ?: continue
                    if (block.optString("type") == "text") {
                        append(block.optString("text"))
                    }
                }
            }.trim()

            if (text.isBlank()) {
                throw AnthropicApiException("Anthropic response contains no text")
            }
            text
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val DEFAULT_ENDPOINT = "https://api.anthropic.com/v1/messages"
        const val ANTHROPIC_VERSION = "2023-06-01"
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 6_000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 12_000
    }
}

class AnthropicApiException(message: String) : IllegalStateException(message)
