package com.hooshmasnooi.app.domain.model

/** Contains no access token or credential; those belong in a secure auth layer. */
data class AccountSession(
    val userId: String,
    val displayName: String? = null,
    val isAuthenticated: Boolean,
    val expiresAtEpochMillis: Long? = null
)
