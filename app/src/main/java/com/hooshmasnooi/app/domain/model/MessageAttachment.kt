package com.hooshmasnooi.app.domain.model

/** Metadata only; file bytes are handled by a storage implementation. */
data class MessageAttachment(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localUri: String? = null,
    val remoteReference: String? = null
) {
    init {
        require(id.isNotBlank())
        require(displayName.isNotBlank())
        require(sizeBytes >= 0)
    }
}
