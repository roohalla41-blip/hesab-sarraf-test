package com.hooshmasnooi.app.domain.model

data class SubscriptionPlan(
    val id: String,
    val displayName: String,
    val tier: SubscriptionTier,
    val isActive: Boolean,
    val renewsAtEpochMillis: Long? = null
)

enum class SubscriptionTier {
    FREE,
    PLUS,
    PRO,
    TEAM
}
