package com.hooshmasnooi.app.domain.usecase

import com.hooshmasnooi.app.data.remote.AiGateway
import com.hooshmasnooi.app.data.remote.AiGatewayResult
import com.hooshmasnooi.app.domain.model.ChatMessage

class SendMessageUseCase(
    private val aiGateway: AiGateway
) {
    suspend operator fun invoke(
        messages: List<ChatMessage>,
        conversationId: String?
    ): AiGatewayResult = aiGateway.complete(messages, conversationId)
}
