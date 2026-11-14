package com.hooshmasnooi.app.data.remote

import com.hooshmasnooi.app.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTP adapter for the ATLAS server boundary.
 *
 * This class contains no provider credentials. The endpoint, Supabase publishable
 * key, and optional app-session token are supplied by the application configuration.
 * Never pass a Supabase secret/service-role key or a Groq key here.
 *
 * The current live ai-assistant function still has the legacy حساب صراف prompt;
 * do not enable this adapter against production until the dedicated ATLAS endpoint
 * has been reviewed and deployed.
 */
class AtlasHttpGateway(
    private val functionUrl: String,
    private val publishableKey: String,
    private val appTokenProvider: suspend () -> String? = { null }
) : AiGateway {

    override suspend fun complete(
        messages: List<ChatMessage>,
        conversationId: String?
    ): AiGatewayResult {
        val latestUserMessage = messages.lastOrNull { it.fromUser }?.text?.trim().orEmpty()
        if (latestUserMessage.isBlank() || latestUserMessage.length > MAX_MESSAGE_CHARS) {
            return AiGatewayResult.Failure(AiFailure.INVALID_RESPONSE)
        }

        // Refuse cleartext, malformed, credential-bearing, or unexpectedly long URLs.
        val endpoint = try {
            URL(functionUrl).also {
                require(it.protocol.equals("https", ignoreCase = true))
                require(it.host.isNotBlank() && it.userInfo == null && it.ref == null)
                require(functionUrl.length <= MAX_URL_CHARS)
            }
        } catch (_: Exception) {
            return AiGatewayResult.Failure(AiFailure.NOT_CONFIGURED)
        }

        val appToken = try {
            appTokenProvider()?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            return AiGatewayResult.Failure(AiFailure.UNAUTHORIZED)
        }

        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val context = JSONArray()
                messages.takeLast(MAX_CONTEXT_MESSAGES).forEach { message ->
                    val item = JSONObject()
                        .put("role", if (message.fromUser) "user" else "assistant")
                        .put("text", message.text.take(MAX_CONTEXT_CHARS))
                    context.put(item)
                }

                val payload = JSONObject()
                    .put("message", latestUserMessage)
                    .put("context", context)

                connection = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    if (publishableKey.isNotBlank()) {
                        setRequestProperty("apikey", publishableKey)
                    }
                    if (appToken != null) {
                        setRequestProperty("x-app-token", appToken)
                    }
                }

                connection.outputStream.use { output ->
                    output.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val responseText = stream?.use { readBounded(it, MAX_RESPONSE_BYTES) }.orEmpty()
                val response = try {
                    JSONObject(responseText)
                } catch (_: Exception) {
                    return@withContext AiGatewayResult.Failure(AiFailure.INVALID_RESPONSE)
                }

                when {
                    statusCode == 401 || statusCode == 403 ->
                        AiGatewayResult.Failure(AiFailure.UNAUTHORIZED)
                    statusCode == 429 ->
                        AiGatewayResult.Failure(AiFailure.RATE_LIMITED)
                    statusCode >= 500 ->
                        AiGatewayResult.Failure(AiFailure.SERVER)
                    statusCode !in 200..299 ->
                        AiGatewayResult.Failure(AiFailure.UNKNOWN)
                    response.optString("status") == "not_configured" ->
                        AiGatewayResult.Failure(AiFailure.NOT_CONFIGURED)
                    response.optString("status") == "ok" &&
                        response.optString("reply").isNotBlank() ->
                        AiGatewayResult.Success(response.getString("reply").trim())
                    else -> AiGatewayResult.Failure(AiFailure.INVALID_RESPONSE)
                }
            } catch (_: java.net.SocketTimeoutException) {
                AiGatewayResult.Failure(AiFailure.NETWORK)
            } catch (_: IOException) {
                AiGatewayResult.Failure(AiFailure.NETWORK)
            } catch (_: Exception) {
                AiGatewayResult.Failure(AiFailure.UNKNOWN)
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun readBounded(input: java.io.InputStream, maxBytes: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > maxBytes) {
                throw IOException("Response exceeds configured limit")
            }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val MAX_MESSAGE_CHARS = 2000
        const val MAX_CONTEXT_MESSAGES = 8
        const val MAX_CONTEXT_CHARS = 600
        const val MAX_RESPONSE_BYTES = 64 * 1024
        const val MAX_URL_CHARS = 2048
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
