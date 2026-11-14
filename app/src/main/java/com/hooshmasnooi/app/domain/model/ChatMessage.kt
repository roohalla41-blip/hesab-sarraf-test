package com.hooshmasnooi.app.domain.model

/**
 * A single message displayed in an ATLAS AI conversation.
 *
 * Persistence and remote response metadata will be added in later stages.
 */
data class ChatMessage(
    val id: Long,
    val text: String,
    val fromUser: Boolean
)
