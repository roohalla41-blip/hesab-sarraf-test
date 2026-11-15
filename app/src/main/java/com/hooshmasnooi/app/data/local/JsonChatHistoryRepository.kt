package com.hooshmasnooi.app.data.local

import android.content.Context
import com.hooshmasnooi.app.domain.model.ChatMessage
import com.hooshmasnooi.app.domain.model.Conversation
import com.hooshmasnooi.app.domain.repository.ChatHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** AES-GCM encrypted, device-local conversation history with verified legacy migration. */
class JsonChatHistoryRepository internal constructor(
    context: Context,
    private val cipher: HistoryCipher = AndroidKeystoreHistoryCipher()
) : ChatHistoryRepository {
    private val directory = context.applicationContext.filesDir
    private val encryptedFile = File(directory, ENCRYPTED_HISTORY_FILE)
    private val legacyFile = File(directory, LEGACY_HISTORY_FILE)
    private val legacyCorruptBackup = File(directory, LEGACY_CORRUPT_FILE)
    private val encryptedCorruptBackup = File(directory, ENCRYPTED_CORRUPT_FILE)
    private val encryptedLegacyCorruptBackup = File(directory, ENCRYPTED_LEGACY_CORRUPT_FILE)
    private val mutex = Mutex()

    override suspend fun getConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readRoot().optJSONArray(KEY_CONVERSATIONS).toConversationList()
                .sortedByDescending { it.updatedAtEpochMillis }
        }
    }

    override suspend fun getMessages(conversationId: String): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                findConversation(readRoot(), conversationId)?.optJSONArray(KEY_MESSAGES).toMessageList()
            }
        }

    override suspend fun saveConversation(conversation: Conversation) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val root = readRoot()
            val items = root.optJSONArray(KEY_CONVERSATIONS) ?: JSONArray()
            val existing = findIndex(items, conversation.id)
            val encoded = conversation.toJson()
            if (existing >= 0) {
                val old = items.getJSONObject(existing)
                val retained = trimMessages(old.optJSONArray(KEY_MESSAGES) ?: JSONArray())
                encoded.put(KEY_MESSAGES, retained)
                encoded.put("messageCount", retained.length())
                encoded.put("updatedAtEpochMillis", maxOf(old.optLong("updatedAtEpochMillis"), conversation.updatedAtEpochMillis))
                items.put(existing, encoded)
            } else {
                encoded.put(KEY_MESSAGES, JSONArray())
                encoded.put("messageCount", 0)
                items.put(encoded)
            }
            root.put(KEY_CONVERSATIONS, trimConversations(items))
            writeRoot(root)
        }
    }

    override suspend fun saveConversationAndMessage(conversation: Conversation, message: ChatMessage) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val root = readRoot()
                val items = root.optJSONArray(KEY_CONVERSATIONS) ?: JSONArray()
                val index = findIndex(items, conversation.id)
                val stored = if (index >= 0) {
                    items.getJSONObject(index).also {
                        it.put("title", conversation.title.take(MAX_TITLE_CHARS))
                        it.put("createdAtEpochMillis", conversation.createdAtEpochMillis)
                        it.put("isPinned", conversation.isPinned)
                    }
                } else conversation.toJson().put(KEY_MESSAGES, JSONArray())

                val messages = stored.optJSONArray(KEY_MESSAGES) ?: JSONArray()
                val duplicate = (0 until messages.length()).any {
                    messages.optJSONObject(it)?.optLong("id") == message.id
                }
                if (!duplicate) messages.put(message.toJson())
                val retained = trimMessages(messages)
                stored.put(KEY_MESSAGES, retained)
                stored.put("messageCount", retained.length())
                stored.put(
                    "updatedAtEpochMillis",
                    maxOf(stored.optLong("updatedAtEpochMillis"), conversation.updatedAtEpochMillis, System.currentTimeMillis())
                )
                if (index >= 0) items.put(index, stored) else items.put(stored)
                root.put(KEY_CONVERSATIONS, trimConversations(items))
                writeRoot(root)
            }
        }

    override suspend fun saveMessage(conversationId: String, message: ChatMessage) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val root = readRoot()
                val items = root.optJSONArray(KEY_CONVERSATIONS) ?: JSONArray()
                val index = findIndex(items, conversationId)
                if (index < 0) return@withLock
                val conversation = items.getJSONObject(index)
                val messages = conversation.optJSONArray(KEY_MESSAGES) ?: JSONArray()
                if ((0 until messages.length()).any { messages.optJSONObject(it)?.optLong("id") == message.id }) {
                    return@withLock
                }
                messages.put(message.toJson())
                val retained = trimMessages(messages)
                conversation.put(KEY_MESSAGES, retained)
                conversation.put("messageCount", retained.length())
                conversation.put("updatedAtEpochMillis", maxOf(conversation.optLong("updatedAtEpochMillis"), System.currentTimeMillis()))
                items.put(index, conversation)
                root.put(KEY_CONVERSATIONS, items)
                writeRoot(root)
            }
        }

    override suspend fun deleteConversation(conversationId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val root = readRoot()
            val items = root.optJSONArray(KEY_CONVERSATIONS) ?: JSONArray()
            val index = findIndex(items, conversationId)
            if (index >= 0) items.remove(index)
            root.put(KEY_CONVERSATIONS, items)
            writeRoot(root)
        }
    }

    override suspend fun deleteAllConversations() = withContext(Dispatchers.IO) {
        mutex.withLock {
            writeRoot(JSONObject().put(KEY_CONVERSATIONS, JSONArray()))
            listOf(
                encryptedCorruptBackup, encryptedLegacyCorruptBackup, legacyFile, legacyCorruptBackup,
                File(directory, ENCRYPTED_TEMP_FILE),
                File(directory, ENCRYPTED_CORRUPT_TEMP_FILE),
                File(directory, ENCRYPTED_LEGACY_CORRUPT_TEMP_FILE)
            ).forEach {
                if (it.exists() && !it.delete()) {
                    throw IllegalStateException("Could not remove local history file: " + it.name)
                }
            }
        }
    }

    private fun readRoot(): JSONObject {
        migrateLegacyFilesIfNeeded()
        if (!encryptedFile.exists()) return JSONObject().put(KEY_CONVERSATIONS, JSONArray())
        val plain = cipher.decrypt(encryptedFile.readBytes()).toString(Charsets.UTF_8)
        return JSONObject(plain).also {
            if (it.optJSONArray(KEY_CONVERSATIONS) == null) it.put(KEY_CONVERSATIONS, JSONArray())
        }
    }

    /**
     * Never deletes a legacy plaintext copy until its encrypted replacement has been
     * written, decrypted, and compared byte-for-byte. Safe to retry after interruption.
     */
    private fun migrateLegacyFilesIfNeeded() {
        if (!encryptedFile.exists()) {
            val legacyBytes = if (legacyFile.exists()) legacyFile.readBytes() else null
            if (legacyBytes != null) {
                val valid = runCatching {
                    JSONObject(legacyBytes.toString(Charsets.UTF_8))
                    true
                }.getOrDefault(false)
                if (valid) {
                    writeEncryptedFile(encryptedFile, legacyBytes)
                } else {
                    writeEncryptedFile(encryptedCorruptBackup, legacyBytes)
                    writeEncryptedFile(encryptedFile, emptyHistoryBytes())
                }
                if (legacyFile.exists() && !legacyFile.delete()) {
                    throw IllegalStateException("Could not remove migrated plaintext history")
                }
            } else if (legacyCorruptBackup.exists()) {
                writeEncryptedFile(encryptedLegacyCorruptBackup, legacyCorruptBackup.readBytes())
                writeEncryptedFile(encryptedFile, emptyHistoryBytes())
            }
        }

        if (encryptedFile.exists()) {
            val current = cipher.decrypt(encryptedFile.readBytes())
            if (legacyFile.exists()) {
                val bytes = legacyFile.readBytes()
                val valid = runCatching {
                    JSONObject(bytes.toString(Charsets.UTF_8))
                    true
                }.getOrDefault(false)
                if (valid && current.contentEquals(bytes)) {
                    if (!legacyFile.delete()) throw IllegalStateException("Could not remove migrated plaintext history")
                } else if (!valid && encryptedCorruptBackup.exists()) {
                    verifyEncryptedFile(encryptedCorruptBackup, bytes)
                    if (!legacyFile.delete()) throw IllegalStateException("Could not remove migrated damaged history")
                }
            }
            if (legacyCorruptBackup.exists()) {
                val bytes = legacyCorruptBackup.readBytes()
                if (!encryptedLegacyCorruptBackup.exists()) writeEncryptedFile(encryptedLegacyCorruptBackup, bytes)
                else verifyEncryptedFile(encryptedLegacyCorruptBackup, bytes)
                if (!legacyCorruptBackup.delete()) throw IllegalStateException("Could not remove plaintext recovery copy")
            }
        }
    }

    private fun writeRoot(root: JSONObject) {
        writeEncryptedFile(encryptedFile, root.toString().toByteArray(Charsets.UTF_8))
    }

    private fun writeEncryptedFile(target: File, plain: ByteArray) {
        val temporary = File(directory, target.name + ".tmp")
        FileOutputStream(temporary).use {
            it.write(cipher.encrypt(plain))
            it.fd.sync()
        }
        verifyEncryptedFile(temporary, plain)
        try {
            try {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (failure: Exception) {
            temporary.delete()
            throw failure
        }
        verifyEncryptedFile(target, plain)
    }

    private fun verifyEncryptedFile(target: File, expected: ByteArray) {
        check(target.exists()) { "Encrypted history file was not created" }
        check(cipher.decrypt(target.readBytes()).contentEquals(expected)) { "Encrypted history verification failed" }
    }

    private fun emptyHistoryBytes() =
        JSONObject().put(KEY_CONVERSATIONS, JSONArray()).toString().toByteArray(Charsets.UTF_8)

    private fun trimMessages(source: JSONArray): JSONArray = JSONArray(
        (0 until source.length()).mapNotNull { source.optJSONObject(it) }
            .sortedBy { it.optLong("id") }.takeLast(MAX_MESSAGES_PER_CONVERSATION)
    )

    private fun trimConversations(source: JSONArray): JSONArray = JSONArray(
        (0 until source.length()).mapNotNull { source.optJSONObject(it) }
            .sortedByDescending { it.optLong("updatedAtEpochMillis") }.take(MAX_CONVERSATIONS)
    )

    private fun findConversation(root: JSONObject, id: String): JSONObject? {
        val items = root.optJSONArray(KEY_CONVERSATIONS) ?: return null
        val index = findIndex(items, id)
        return if (index >= 0) items.optJSONObject(index) else null
    }

    private fun findIndex(items: JSONArray, id: String): Int =
        (0 until items.length()).firstOrNull { items.optJSONObject(it)?.optString("id") == id } ?: -1

    private fun JSONArray?.toConversationList(): List<Conversation> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let {
                Conversation(
                    id = it.optString("id"),
                    title = it.optString("title", "گفت‌وگوی تازه"),
                    createdAtEpochMillis = it.optLong("createdAtEpochMillis"),
                    updatedAtEpochMillis = it.optLong("updatedAtEpochMillis"),
                    messageCount = it.optInt("messageCount"),
                    isPinned = it.optBoolean("isPinned")
                )
            }
        }
    }

    private fun JSONArray?.toMessageList(): List<ChatMessage> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let {
                ChatMessage(it.optLong("id"), it.optString("text"), it.optBoolean("fromUser"))
            }
        }
    }

    private fun Conversation.toJson() = JSONObject()
        .put("id", id).put("title", title.take(MAX_TITLE_CHARS))
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .put("updatedAtEpochMillis", updatedAtEpochMillis)
        .put("messageCount", messageCount).put("isPinned", isPinned)

    private fun ChatMessage.toJson() = JSONObject()
        .put("id", id).put("text", text.take(MAX_MESSAGE_CHARS)).put("fromUser", fromUser)

    private companion object {
        const val ENCRYPTED_HISTORY_FILE = "atlas_chat_history.enc"
        const val LEGACY_HISTORY_FILE = "atlas_chat_history.json"
        const val LEGACY_CORRUPT_FILE = "atlas_chat_history.json.corrupt"
        const val ENCRYPTED_CORRUPT_FILE = "atlas_chat_history.corrupt.enc"
        const val ENCRYPTED_LEGACY_CORRUPT_FILE = "atlas_chat_history.legacy-corrupt.enc"
        const val ENCRYPTED_TEMP_FILE = "atlas_chat_history.enc.tmp"
        const val ENCRYPTED_CORRUPT_TEMP_FILE = "atlas_chat_history.corrupt.enc.tmp"
        const val ENCRYPTED_LEGACY_CORRUPT_TEMP_FILE = "atlas_chat_history.legacy-corrupt.enc.tmp"
        const val KEY_CONVERSATIONS = "conversations"
        const val KEY_MESSAGES = "messages"
        const val MAX_CONVERSATIONS = 40
        const val MAX_MESSAGES_PER_CONVERSATION = 300
        const val MAX_MESSAGE_CHARS = 12_000
        const val MAX_TITLE_CHARS = 80
    }
}
