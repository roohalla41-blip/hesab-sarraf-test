package com.hooshmasnooi.app.data.remote

import com.hooshmasnooi.app.domain.model.ChatMessage

/** Server-side AI boundary. Provider credentials must never ship in the Android app. */
interface AiGateway {
    suspend fun complete(
        messages: List<ChatMessage>,
        conversationId: String?
    ): AiGatewayResult
}

sealed interface AiGatewayResult {
    data class Success(val answer: String) : AiGatewayResult
    data class Failure(val reason: AiFailure) : AiGatewayResult
}

enum class AiFailure {
    NOT_CONFIGURED,
    UNAUTHORIZED,
    RATE_LIMITED,
    NETWORK,
    SERVER,
    INVALID_RESPONSE,
    UNKNOWN
}
