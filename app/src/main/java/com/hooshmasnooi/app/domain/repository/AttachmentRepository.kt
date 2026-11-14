package com.hooshmasnooi.app.domain.repository

import com.hooshmasnooi.app.domain.model.MessageAttachment

interface AttachmentRepository {
    suspend fun stageLocalAttachment(uri: String, displayName: String, mimeType: String): MessageAttachment
    suspend fun removeLocalAttachment(attachmentId: String)
}
