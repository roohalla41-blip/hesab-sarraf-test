package com.hooshmasnooi.app.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val messageCount: Int = 0,
    val isPinned: Boolean = false
)
