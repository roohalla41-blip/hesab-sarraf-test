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
 * Calls the public ATLAS chat endpoint.
 * The publishable key is safe for client use; all AI provider credentials remain server-side.
 */
class SupabaseChatGateway(
    private val supabaseUrl: String,
    private val anonKey: String,
    private val accessTokenProvider: AccessTokenProvider = AccessTokenProvider { null },
) : AiGateway {

    override suspend fun complete(
        messages: List<ChatMessage>,
        conversationId: String?,
    ): AiGatewayResult = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            return@withContext AiGatewayResult.Failure(AiFailure.NOT_CONFIGURED)
        }

        val recentMessages = messages.takeLast(MAX_MESSAGES)
        val latest = recentMessages.lastOrNull()
        if (latest == null || !latest.fromUser ||
            recentMessages.any { it.text.isBlank() || it.text.length > MAX_MESSAGE_CHARS } ||
            recentMessages.sumOf { it.text.length } > MAX_TOTAL_CHARS
        ) {
            return@withContext AiGatewayResult.Failure(AiFailure.INVALID_RESPONSE)
        }

        val endpoint = runCatching {
            val base = URL(supabaseUrl.trimEnd('/') + "/")
            require(base.protocol == "https") { "HTTPS is required" }
            URL(base, "functions/v1/atlas-chat")
        }.getOrNull() ?: return@withContext AiGatewayResult.Failure(AiFailure.NOT_CONFIGURED)

        val accessToken = runCatching { accessTokenProvider.currentAccessToken() }.getOrNull()
        val history = recentMessages.dropLast(1).takeLast(MAX_HISTORY_MESSAGES)
        val payload = JSONObject()
            .put("message", latest.text)
            .put("mode", "عمومی")
            .put("history", JSONArray().apply {
                history.forEach { message ->
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
                if (!accessToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }
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
            if (status !in 200..299) {
                val errorBody = runCatching {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }.getOrDefault("")
                val errorMessage = runCatching {
                    JSONObject(errorBody).optString("error").lowercase()
                }.getOrDefault("")
                val failure = when {
                    status == 503 || errorMessage.contains("کلید") || errorMessage.contains("configured") ->
                        AiFailure.NOT_CONFIGURED
                    status >= 500 -> AiFailure.SERVER
                    else -> AiFailure.INVALID_RESPONSE
                }
                return@withContext AiGatewayResult.Failure(failure)
            }

            val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val answer = JSONObject(responseText).optString("reply").trim()
            if (answer.isBlank()) {
                AiGatewayResult.Failure(AiFailure.INVALID_RESPONSE)
            } else {
                AiGatewayResult.Success(answer)
            }
        } catch (_: java.net.SocketTimeoutException) {
            AiGatewayResult.Failure(AiFailure.NETWORK)
        } catch (_: java.io.IOException) {
            AiGatewayResult.Failure(AiFailure.NETWORK)
        } catch (_: Exception) {
            AiGatewayResult.Failure(AiFailure.INVALID_RESPONSE)
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val MAX_MESSAGES = 30
        const val MAX_HISTORY_MESSAGES = 8
        const val MAX_MESSAGE_CHARS = 3_000
        const val MAX_TOTAL_CHARS = 24_000
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 40_000
    }
}
