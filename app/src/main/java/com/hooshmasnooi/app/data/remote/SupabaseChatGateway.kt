package com.hooshmasnooi.app.data.remote

import com.hooshmasnooi.app.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

fun interface AccessTokenProvider {
    suspend fun currentAccessToken(): String?
}

/**
 * Authenticated client for the ATLAS Supabase Edge Function.
 * The anon key is a public project identifier; provider secrets stay server-side.
 * This adapter is intentionally not wired until the ATLAS project and sign-in flow are approved.
 */
class SupabaseChatGateway(
    private val supabaseUrl: String,
    private val anonKey: String,
    private val accessTokenProvider: AccessTokenProvider,
) : AiGateway {

    override suspend fun complete(
        messages: List<ChatMessage>,
        conversationId: String?,
    ): AiGatewayResult = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            return@withContext AiGatewayResult.Failure(AiFailure.NOT_CONFIGURED)
        }

        val accessToken = runCatching { accessTokenProvider.currentAccessToken() }.getOrNull()
        if (accessToken.isNullOrBlank()) {
            return@withContext AiGatewayResult.Failure(AiFailure.UNAUTHORIZED)
        }

        val endpoint = runCatching {
            val base = URL(supabaseUrl.trimEnd('/') + "/")
            require(base.protocol == "https") { "HTTPS is required" }
            URL(base, "functions/v1/chat")
        }.getOrNull() ?: return@withContext AiGatewayResult.Failure(AiFailure.NOT_CONFIGURED)

        val recentMessages = messages.takeLast(MAX_MESSAGES)
        if (recentMessages.isEmpty() || recentMessages.lastOrNull()?.fromUser != true ||
            recentMessages.any { it.text.isBlank() || it.text.length > MAX_MESSAGE_CHARS }
        ) {
            return@withContext AiGatewayResult.Failure(AiFailure.INVALID_RESPONSE)
        }

        val payload = JSONObject()
            .put("conversation_id", conversationId)
            .put("mode", "general")
            .put("messages", JSONArray().apply {
                recentMessages.forEach { message ->
                    put(
                        JSONObject()
                            .put("role", if (message.fromUser) "user" else "assistant")
                            .put("content", message.text)
                    )
                }
            })

        var connection: HttpURLConnection? = null
        try {
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("X-Request-ID", UUID.randomUUID().toString())
            }
            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            if (status == 401 || status == 403) {
                return@withContext AiGatewayResult.Failure(AiFailure.UNAUTHORIZED)
            }
            if (status == 429) {
                return@withContext AiGatewayResult.Failure(AiFailure.RATE_LIMITED)
            }
            if (status >= 500) {
                return@withContext AiGatewayResult.Failure(AiFailure.SERVER)
            }
            if (status !in 200..299) {
                return@withContext AiGatewayResult.Failure(AiFailure.INVALID_RESPONSE)
            }

            val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val response = JSONObject(responseText)
            val answer = response.optJSONObject("assistant_message")?.optString("content").orEmpty()
            if (answer.isBlank()) {
                AiGatewayResult.Failure(AiFailure.INVALID_RESPONSE)
            } else {
                AiGatewayResult.Success(answer)
            }
        } catch (_: Exception) {
            AiGatewayResult.Failure(AiFailure.NETWORK)
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val MAX_MESSAGES = 30
        const val MAX_MESSAGE_CHARS = 12_000
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 50_000
    }
}
