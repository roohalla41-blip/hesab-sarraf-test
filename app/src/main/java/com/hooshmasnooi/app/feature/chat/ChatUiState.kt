package com.hooshmasnooi.app.feature.chat

import com.hooshmasnooi.app.domain.model.ChatMessage

data class ChatUiState(
    val conversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val canSend: Boolean = false,
    val errorMessage: String? = null
)
