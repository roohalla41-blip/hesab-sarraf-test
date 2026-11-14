package com.hooshmasnooi.app.domain.repository

import com.hooshmasnooi.app.domain.model.SubscriptionPlan
import com.hooshmasnooi.app.domain.model.UsageQuota

/** Server-authoritative usage and plan contract; no local billing authority. */
interface UsageRepository {
    suspend fun getCurrentQuota(): UsageQuota
    suspend fun getCurrentPlan(): SubscriptionPlan
}
