package com.hooshmasnooi.app.domain.repository

import com.hooshmasnooi.app.domain.model.ChatMessage

/**
 * Contract for sending a conversation to an assistant provider.
 *
 * No implementation is connected yet. A future implementation will call
 * the independent ATLAS AI backend; it must never call the Sarraf projects.
 */
interface ChatRepository {
    suspend fun sendMessage(conversation: List<ChatMessage>): Result<String>
}
