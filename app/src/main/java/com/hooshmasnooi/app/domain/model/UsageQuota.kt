package com.hooshmasnooi.app.domain.model

data class UsageQuota(
    val periodStartsAtEpochMillis: Long,
    val periodEndsAtEpochMillis: Long,
    val limit: Long?,
    val used: Long,
    val unit: UsageUnit
) {
    val remaining: Long?
        get() = limit?.let { (it - used).coerceAtLeast(0) }

    val isLimitReached: Boolean
        get() = limit != null && used >= limit

    init {
        require(periodEndsAtEpochMillis >= periodStartsAtEpochMillis)
        require(limit == null || limit >= 0)
        require(used >= 0)
    }
}

enum class UsageUnit {
    MESSAGES,
    TOKENS,
    CREDITS
}
