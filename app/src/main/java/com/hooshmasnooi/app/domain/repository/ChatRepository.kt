package com.hooshmasnooi.app.domain.repository

import com.hooshmasnooi.app.domain.model.ChatMessage

/**
 * Application contract for sending a conversation to ATLAS AI.
 *
 * The remote implementation lives in data/repository/RemoteChatRepository.kt
 * and delegates to the authenticated server gateway. The Android UI must not
 * substitute a canned answer when the backend is unavailable.
 */
interface ChatRepository {
    suspend fun sendMessage(conversation: List<ChatMessage>): Result<String>
}
