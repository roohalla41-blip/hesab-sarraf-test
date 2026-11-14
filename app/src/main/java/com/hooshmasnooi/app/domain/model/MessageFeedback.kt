package com.hooshmasnooi.app.domain.model

data class MessageFeedback(
    val messageId: String,
    val kind: FeedbackKind,
    val reason: String? = null,
    val createdAtEpochMillis: Long
)

enum class FeedbackKind {
    HELPFUL,
    NOT_HELPFUL,
    REPORT_SAFETY,
    REPORT_INACCURACY
}
