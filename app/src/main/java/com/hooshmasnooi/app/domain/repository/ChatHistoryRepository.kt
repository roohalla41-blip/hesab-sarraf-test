package com.hooshmasnooi.app.domain.repository

import com.hooshmasnooi.app.domain.model.ChatMessage
import com.hooshmasnooi.app.domain.model.Conversation

/** Local history contract, intentionally independent from cloud storage. */
interface ChatHistoryRepository {
    suspend fun getConversations(): List<Conversation>
    suspend fun getMessages(conversationId: String): List<ChatMessage>
    suspend fun saveConversation(conversation: Conversation)
    suspend fun saveMessage(conversationId: String, message: ChatMessage)
    /** Atomically updates conversation metadata and appends its message. */
    suspend fun saveConversationAndMessage(conversation: Conversation, message: ChatMessage)
    suspend fun deleteConversation(conversationId: String)
    suspend fun deleteAllConversations()
}
