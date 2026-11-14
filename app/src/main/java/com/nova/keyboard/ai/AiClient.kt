package com.nova.keyboard.ai

import com.nova.keyboard.core.SecureStore
import com.nova.keyboard.core.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Failure(val message: String) : AiResult()
}

/**
 * OpenAI-compatible chat-completions client. The endpoint, model and key are
 * user supplied; no credential ships with the app and every call happens only
 * after an explicit user tap.
 */
class AiClient(
    private val secure: SecureStore,
    private val settings: SettingsRepository
) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // The whole request (including redirects and a slow TLS handshake) is
        // bounded by aiTimeoutSeconds; readTimeout alone could keep the panel in
        // "Thinking…" for far longer than the user configured.
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun hasKey(): Boolean = secure.hasSecret(KEY_ALIAS)

    fun setKey(key: String?) = secure.putSecret(KEY_ALIAS, key)

    fun isConfigured(): Boolean = settings.current.aiEnabled && hasKey()

    suspend fun complete(system: String, user: String, maxTokens: Int = 700): AiResult =
        withContext(Dispatchers.IO) {
            val s = settings.current
            if (!s.aiEnabled) return@withContext AiResult.Failure("AI is switched off in AI Studio.")
            val key = secure.getSecret(KEY_ALIAS)
                ?: return@withContext AiResult.Failure("No API key saved. Add one in AI Studio.")
            val base = s.aiBaseUrl.trim().trimEnd('/')
            // `startsWith` alone accepted "https://" with no host and lower-case
            // only; parse it properly so a typo fails here with a clear message
            // instead of a confusing network error.
            val parsed = runCatching { java.net.URI(base) }.getOrNull()
            if (parsed == null || !parsed.scheme.equals("https", ignoreCase = true) ||
                parsed.host.isNullOrBlank()
            ) {
                return@withContext AiResult.Failure(
                    "Enter a full https:// provider URL, for example https://api.openai.com/v1"
                )
            }
            if (user.isBlank()) return@withContext AiResult.Failure("There is no text to send.")

            val body = JSONObject().apply {
                put("model", s.aiModel)
                put("temperature", 0.4)
                put("max_tokens", maxTokens)
                put(
                    "messages", JSONArray().apply {
                        put(JSONObject().put("role", "system").put("content", system))
                        put(JSONObject().put("role", "user").put("content", user))
                    }
                )
            }.toString()

            val request = Request.Builder()
                .url(base + "/chat/completions")
                .addHeader("Authorization", "Bearer " + key)
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(mediaType))
                .build()

            val client = http.newBuilder()
                .callTimeout(s.aiTimeoutSeconds.coerceIn(10, 120).toLong(), TimeUnit.SECONDS)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val payload = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val detail = runCatching {
                            JSONObject(payload).getJSONObject("error").getString("message")
                        }.getOrNull() ?: ("HTTP " + response.code)
                        return@withContext AiResult.Failure("Provider error: " + detail)
                    }
                    val text = runCatching {
                        JSONObject(payload)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim()
                    }.getOrNull()
                    if (text.isNullOrBlank()) {
                        AiResult.Failure("The provider returned an empty response.")
                    } else {
                        AiResult.Success(text)
                    }
                }
            } catch (e: IOException) {
                // Never surface the URL or the key in an error string.
                AiResult.Failure("Network unavailable or request failed. The keyboard still works offline.")
            } catch (e: Exception) {
                AiResult.Failure("AI request failed: " + (e.message ?: e.javaClass.simpleName))
            }
        }

    private companion object {
        const val KEY_ALIAS = "ai_api_key"
    }
}
