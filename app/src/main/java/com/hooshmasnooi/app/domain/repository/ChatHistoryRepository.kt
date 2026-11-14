package com.hooshmasnooi.app.domain.repository

import com.hooshmasnooi.app.domain.model.ChatMessage
import com.hooshmasnooi.app.domain.model.Conversation

/**
 * Local conversation storage contract.
 * The implementation will be added separately from the remote AI service.
 */
interface ChatHistoryRepository {
    suspend fun getConversations(): List<Conversation>
    suspend fun getMessages(conversationId: String): List<ChatMessage>
    suspend fun saveConversation(conversation: Conversation)
    suspend fun saveMessage(conversationId: String, message: ChatMessage)
    suspend fun deleteConversation(conversationId: String)
}
