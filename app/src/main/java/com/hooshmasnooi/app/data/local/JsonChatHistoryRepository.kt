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

/**
 * JSON-backed conversation history stored in the app's private internal files directory.
 *
 * This is device-local storage only: it does not sync to Supabase or any cloud service.
 * Android app sandboxing protects the file from ordinary other apps, but this first
 * version does not provide application-level encryption. Do not store passwords,
 * API keys, or other secrets in conversation messages.
 */
class JsonChatHistoryRepository(context: Context) : ChatHistoryRepository {
    private val directory = context.applicationContext.filesDir
    private val file = File(directory, HISTORY_FILE)
    private val corruptBackup = File(directory, CORRUPT_BACKUP_FILE)
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
                findConversation(readRoot(), conversationId)
                    ?.optJSONArray(KEY_MESSAGES)
                    .toMessageList()
            }
        }

    override suspend fun saveConversation(conversation: Conversation) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val root = readRoot()
                val items = root.optJSONArray(KEY_CONVERSATIONS) ?: JSONArray()
                val existing = findIndex(items, conversation.id)
                val encoded = conversation.toJson()
                if (existing >= 0) {
                    val old = items.getJSONObject(existing)
                    encoded.put(KEY_MESSAGES, old.optJSONArray(KEY_MESSAGES) ?: JSONArray())
                    items.put(existing, encoded)
                } else {
                    encoded.put(KEY_MESSAGES, JSONArray())
                    items.put(encoded)
                }
                root.put(KEY_CONVERSATIONS, trimConversations(items))
                writeRoot(root)
            }
        }

    override suspend fun saveConversationAndMessage(
        conversation: Conversation,
        message: ChatMessage
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val root = readRoot()
            val items = root.optJSONArray(KEY_CONVERSATIONS) ?: JSONArray()
            val index = findIndex(items, conversation.id)
            val stored = if (index >= 0) {
                items.getJSONObject(index).also { existing ->
                    existing.put("title", conversation.title.take(MAX_TITLE_CHARS))
                    existing.put("createdAtEpochMillis", conversation.createdAtEpochMillis)
                    existing.put("isPinned", conversation.isPinned)
                }
            } else {
                conversation.toJson().put(KEY_MESSAGES, JSONArray())
            }
            val messages = stored.optJSONArray(KEY_MESSAGES) ?: JSONArray()
            val duplicate = (0 until messages.length()).any {
                messages.optJSONObject(it)?.optLong("id") == message.id
            }
            if (!duplicate) messages.put(message.toJson())
            stored.put(KEY_MESSAGES, trimMessages(messages))
            stored.put("messageCount", messages.length())
            stored.put(
                "updatedAtEpochMillis",
                maxOf(conversation.updatedAtEpochMillis, System.currentTimeMillis())
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
                if ((0 until messages.length()).any {
                        messages.optJSONObject(it)?.optLong("id") == message.id
                    }) return@withLock

                messages.put(message.toJson())
                conversation.put(KEY_MESSAGES, trimMessages(messages))
                conversation.put("messageCount", messages.length())
                conversation.put("updatedAtEpochMillis", System.currentTimeMillis())
                items.put(index, conversation)
                root.put(KEY_CONVERSATIONS, items)
                writeRoot(root)
            }
        }

    override suspend fun deleteConversation(conversationId: String) =
        withContext(Dispatchers.IO) {
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
            // Persist the empty history first. If this fails, keep the recovery copy.
            writeRoot(JSONObject().put(KEY_CONVERSATIONS, JSONArray()))
            if (corruptBackup.exists() && !corruptBackup.delete()) {
                throw IllegalStateException("Could not remove the damaged-history recovery copy")
            }
            tempFile.delete()
        }
    }

    private val tempFile: File
        get() = File(directory, TEMP_FILE_NAME)

    private fun readRoot(): JSONObject {
        if (!file.exists()) return JSONObject().put(KEY_CONVERSATIONS, JSONArray())
        return try {
            JSONObject(file.readText(Charsets.UTF_8)).also {
                if (it.optJSONArray(KEY_CONVERSATIONS) == null) {
                    it.put(KEY_CONVERSATIONS, JSONArray())
                }
            }
        } catch (_: Exception) {
            // Preserve the damaged file for recovery; clear-all explicitly removes this copy.
            if (!corruptBackup.exists()) file.copyTo(corruptBackup, overwrite = false)
            JSONObject().put(KEY_CONVERSATIONS, JSONArray())
        }
    }

    private fun writeRoot(root: JSONObject) {
        val bytes = root.toString().toByteArray(Charsets.UTF_8)
        FileOutputStream(tempFile).use { output ->
            output.write(bytes)
            output.fd.sync()
        }

        try {
            try {
                Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                // Same-directory replacement avoids the old unsafe truncate-and-rewrite fallback.
                Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (failure: Exception) {
            // Keep the previous history intact and surface the failure to the UI.
            throw failure
        }
    }

    private fun trimMessages(source: JSONArray): JSONArray {
        val retained = (0 until source.length())
            .mapNotNull { source.optJSONObject(it) }
            .sortedBy { it.optLong("id") }
            .takeLast(MAX_MESSAGES_PER_CONVERSATION)
        return JSONArray(retained)
    }

    private fun trimConversations(source: JSONArray): JSONArray {
        val values = (0 until source.length())
            .mapNotNull { source.optJSONObject(it) }
            .sortedByDescending { it.optLong("updatedAtEpochMillis") }
            .take(MAX_CONVERSATIONS)
        return JSONArray(values)
    }

    private fun findConversation(root: JSONObject, id: String): JSONObject? {
        val items = root.optJSONArray(KEY_CONVERSATIONS) ?: return null
        val index = findIndex(items, id)
        return if (index >= 0) items.optJSONObject(index) else null
    }

    private fun findIndex(items: JSONArray, id: String): Int =
        (0 until items.length()).firstOrNull {
            items.optJSONObject(it)?.optString("id") == id
        } ?: -1

    private fun JSONArray?.toConversationList(): List<Conversation> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let { json ->
                Conversation(
                    id = json.optString("id"),
                    title = json.optString("title", "گفت‌وگوی تازه"),
                    createdAtEpochMillis = json.optLong("createdAtEpochMillis"),
                    updatedAtEpochMillis = json.optLong("updatedAtEpochMillis"),
                    messageCount = json.optInt("messageCount"),
                    isPinned = json.optBoolean("isPinned")
                )
            }
        }
    }

    private fun JSONArray?.toMessageList(): List<ChatMessage> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let { json ->
                ChatMessage(
                    id = json.optLong("id"),
                    text = json.optString("text"),
                    fromUser = json.optBoolean("fromUser")
                )
            }
        }
    }

    private fun Conversation.toJson() = JSONObject()
        .put("id", id)
        .put("title", title.take(MAX_TITLE_CHARS))
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .put("updatedAtEpochMillis", updatedAtEpochMillis)
        .put("messageCount", messageCount)
        .put("isPinned", isPinned)

    private fun ChatMessage.toJson() = JSONObject()
        .put("id", id)
        .put("text", text.take(MAX_MESSAGE_CHARS))
        .put("fromUser", fromUser)

    private companion object {
        const val HISTORY_FILE = "atlas_chat_history.json"
        const val TEMP_FILE_NAME = "atlas_chat_history.json.tmp"
        const val CORRUPT_BACKUP_FILE = "atlas_chat_history.json.corrupt"
        const val KEY_CONVERSATIONS = "conversations"
        const val KEY_MESSAGES = "messages"
        const val MAX_CONVERSATIONS = 40
        const val MAX_MESSAGES_PER_CONVERSATION = 300
        const val MAX_MESSAGE_CHARS = 12_000
        const val MAX_TITLE_CHARS = 80
    }
}
