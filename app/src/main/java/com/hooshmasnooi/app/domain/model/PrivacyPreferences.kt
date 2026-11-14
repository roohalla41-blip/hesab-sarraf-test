package com.hooshmasnooi.app.domain.model

data class PrivacyPreferences(
    val saveChatHistory: Boolean = true,
    val allowOptionalProductAnalytics: Boolean = false,
    val allowModelImprovementUse: Boolean = false
)
