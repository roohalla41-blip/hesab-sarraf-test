package com.hooshmasnooi.app.data.repository

import com.hooshmasnooi.app.data.remote.AiFailure
import com.hooshmasnooi.app.data.remote.AiGateway
import com.hooshmasnooi.app.data.remote.AiGatewayResult
import com.hooshmasnooi.app.domain.model.ChatMessage
import com.hooshmasnooi.app.domain.repository.ChatRepository

/**
 * Production repository adapter for the authenticated ATLAS AI gateway.
 *
 * Authentication is supplied by the AccessTokenProvider passed to the gateway.
 * It deliberately does not fall back to a fake/local answer when the service is
 * unavailable, so the UI can present the real failure state to the user.
 */
class RemoteChatRepository(
    private val gateway: AiGateway,
    private val conversationIdProvider: () -> String? = { null }
) : ChatRepository {

    override suspend fun sendMessage(conversation: List<ChatMessage>): Result<String> {
        if (conversation.isEmpty() || conversation.lastOrNull()?.fromUser != true) {
            return Result.failure(AiRequestException(AiFailure.INVALID_RESPONSE))
        }

        return when (
            val result = gateway.complete(
                messages = conversation,
                conversationId = conversationIdProvider()
            )
        ) {
            is AiGatewayResult.Success -> Result.success(result.answer)
            is AiGatewayResult.Failure -> Result.failure(AiRequestException(result.reason))
        }
    }
}

class AiRequestException(
    val failure: AiFailure
) : Exception(failure.name)
