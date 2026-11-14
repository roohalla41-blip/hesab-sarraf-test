package com.hooshmasnooi.app.domain.repository

import com.hooshmasnooi.app.domain.model.AccountSession
import com.hooshmasnooi.app.domain.model.PrivacyPreferences

/** Authentication implementation is intentionally not connected in the scaffold. */
interface AccountRepository {
    suspend fun currentSession(): AccountSession?
    suspend fun signOut()
    suspend fun getPrivacyPreferences(): PrivacyPreferences
    suspend fun savePrivacyPreferences(preferences: PrivacyPreferences)
}
