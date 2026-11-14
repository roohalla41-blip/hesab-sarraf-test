package com.hooshmasnooi.app.domain.model

enum class MessageDeliveryState {
    DRAFT,
    QUEUED,
    SENDING,
    SENT,
    STREAMING,
    COMPLETED,
    FAILED,
    CANCELLED
}
