package com.hooshmasnooi.app.data.local

import android.content.Context
import com.hooshmasnooi.app.domain.model.ChatMessage
import com.hooshmasnooi.app.core.AtlasText
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StoredConversation(
    val id: String,
    val title: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val messages: List<ChatMessage>
) {
    val messageCount: Int get() = messages.size
}

/** Private, on-device persistence for independent conversations. */
object ChatStorage {
    private const val PREFS = "atlas_private_chat"
    private const val KEY_MESSAGES = "messages"
    private const val KEY_CONVERSATIONS = "conversations_v2"
    private const val KEY_CONVERSATIONS_BACKUP = "conversations_v2_backup"
    private const val KEY_ACTIVE_CONVERSATION = "active_conversation"
    private const val MAX_MESSAGES = 300
    private const val MAX_CONVERSATIONS = 100

    fun ensureActive(context: Context): StoredConversation {
        val conversations = loadAll(context)
        val preferences = preferences(context)
        val activeId = preferences.getString(KEY_ACTIVE_CONVERSATION, null)
        val active = conversations.firstOrNull { it.id == activeId }
        if (active != null) return active
        if (conversations.isNotEmpty()) {
            preferences.edit().putString(KEY_ACTIVE_CONVERSATION, conversations.first().id).apply()
            return conversations.first()
        }
        return createConversation(context)
    }

    fun load(context: Context): List<ChatMessage> = ensureActive(context).messages

    fun loadAll(context: Context): List<StoredConversation> {
        val preferences = preferences(context)
        val raw = preferences.getString(KEY_CONVERSATIONS, null)
        if (raw.isNullOrBlank()) {
            val backupRaw = preferences.getString(KEY_CONVERSATIONS_BACKUP, null)
            val backup = parseConversations(backupRaw)
            if (backup != null && !backupRaw.isNullOrBlank()) {
                preferences.edit().putString(KEY_CONVERSATIONS, backupRaw).commit()
                return backup
            }
            val legacyRaw = preferences.getString(KEY_MESSAGES, null)
            val legacyMessages = parseMessages(legacyRaw)
            if (legacyMessages.isEmpty()) return emptyList()
            val now = System.currentTimeMillis()
            val migrated = StoredConversation(
                id = "legacy-" + now,
                title = titleFor(legacyMessages),
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                messages = legacyMessages
            )
            saveAll(context, listOf(migrated))
            preferences.edit().putString(KEY_ACTIVE_CONVERSATION, migrated.id).apply()
            return listOf(migrated)
        }
        parseConversations(raw)?.let { return it }

        // Recover the last known-good snapshot instead of silently treating corrupt JSON as empty.
        val backupRaw = preferences.getString(KEY_CONVERSATIONS_BACKUP, null)
        val backup = parseConversations(backupRaw)
        if (backup != null && !backupRaw.isNullOrBlank()) {
            preferences.edit().putString(KEY_CONVERSATIONS, backupRaw).commit()
            return backup
        }
        return emptyList()
    }

    private fun parseConversations(raw: String?): List<StoredConversation>? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    add(
                        StoredConversation(
                            id = id,
                            title = item.optString("title", AtlasText.of("گفت‌وگوی جدید", "New conversation")),
                            createdAtEpochMillis = item.optLong("createdAt", 0L),
                            updatedAtEpochMillis = item.optLong("updatedAt", 0L),
                            messages = parseMessages(item.optJSONArray("messages"))
                        )
                    )
                }
            }.sortedByDescending { it.updatedAtEpochMillis }.take(MAX_CONVERSATIONS)
        }.getOrNull()
    }

    fun createConversation(context: Context): StoredConversation {
        val now = System.currentTimeMillis()
        val conversation = StoredConversation(
            id = UUID.randomUUID().toString(),
            title = AtlasText.of("گفت‌وگوی جدید", "New conversation"),
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            messages = emptyList()
        )
        val all = loadAll(context).toMutableList()
        all.add(0, conversation)
        saveAll(context, all)
        preferences(context).edit().putString(KEY_ACTIVE_CONVERSATION, conversation.id).apply()
        return conversation
    }

    fun setActiveConversation(context: Context, conversationId: String) {
        if (loadAll(context).any { it.id == conversationId }) {
            preferences(context).edit().putString(KEY_ACTIVE_CONVERSATION, conversationId).apply()
        }
    }

    fun loadConversation(context: Context, conversationId: String): List<ChatMessage> =
        loadAll(context).firstOrNull { it.id == conversationId }?.messages ?: emptyList()

    fun save(context: Context, messages: List<ChatMessage>, conversationId: String? = null) {
        val preferences = preferences(context)
        val id = conversationId
            ?: preferences.getString(KEY_ACTIVE_CONVERSATION, null)
            ?: createConversation(context).id
        val all = loadAll(context).toMutableList()
        val existing = all.firstOrNull { it.id == id }
        val now = System.currentTimeMillis()
        val updated = StoredConversation(
            id = id,
            title = existing?.title
                ?.takeIf { it.isNotBlank() && it != AtlasText.of("گفت‌وگوی جدید", "New conversation") }
                ?: if (messages.isEmpty()) {
                    existing?.title ?: AtlasText.of("گفت‌وگوی جدید", "New conversation")
                } else {
                    titleFor(messages)
                },
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
            messages = messages.takeLast(MAX_MESSAGES)
        )
        all.removeAll { it.id == id }
        all.add(0, updated)
        saveAll(context, all)
        preferences.edit().putString(KEY_ACTIVE_CONVERSATION, id).apply()
    }

    fun renameConversation(context: Context, conversationId: String, newTitle: String) {
        val cleanTitle = newTitle.trim().take(60)
        if (cleanTitle.isBlank()) return
        val updated = loadAll(context).map { conversation ->
            if (conversation.id == conversationId) conversation.copy(title = cleanTitle) else conversation
        }
        saveAll(context, updated)
    }

    fun deleteConversation(context: Context, conversationId: String) {
        val all = loadAll(context).filterNot { it.id == conversationId }
        saveAll(context, all, keepPreviousSnapshot = false)
        val activeId = preferences(context).getString(KEY_ACTIVE_CONVERSATION, null)
        if (activeId == conversationId) {
            if (all.isNotEmpty()) {
                preferences(context).edit().putString(KEY_ACTIVE_CONVERSATION, all.first().id).apply()
            } else {
                createConversation(context)
            }
        }
    }

    fun clear(context: Context) {
        val activeId = preferences(context).getString(KEY_ACTIVE_CONVERSATION, null) ?: return
        val now = System.currentTimeMillis()
        val cleared = loadAll(context).map { conversation ->
            if (conversation.id == activeId) {
                conversation.copy(messages = emptyList(), updatedAtEpochMillis = now)
            } else {
                conversation
            }
        }
        saveAll(context, cleared, keepPreviousSnapshot = false)
    }

    private fun titleFor(messages: List<ChatMessage>): String {
        val firstUserMessage = messages.firstOrNull { it.fromUser }?.text?.trim().orEmpty()
        if (firstUserMessage.isBlank()) return AtlasText.of("گفت‌وگوی جدید", "New conversation")
        return if (firstUserMessage.length > 44) firstUserMessage.take(44).trimEnd() + "…" else firstUserMessage
    }

    private fun parseMessages(raw: String?): List<ChatMessage> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { parseMessages(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    private fun parseMessages(array: JSONArray?): List<ChatMessage> {
        if (array == null) return emptyList()
        val firstRetainedIndex = (array.length() - MAX_MESSAGES).coerceAtLeast(0)
        return buildList {
            for (index in firstRetainedIndex until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optLong("id", index.toLong() + 1L)
                val text = item.optString("text", "")
                if (text.isNotBlank()) {
                    add(ChatMessage(id = id, text = text, fromUser = item.optBoolean("fromUser", true)))
                }
            }
        }
    }

    private fun saveAll(
        context: Context,
        conversations: List<StoredConversation>,
        keepPreviousSnapshot: Boolean = true
    ) {
        val array = JSONArray()
        conversations.take(MAX_CONVERSATIONS).forEach { conversation ->
            val messages = JSONArray()
            conversation.messages.takeLast(MAX_MESSAGES).forEach { message ->
                messages.put(
                    JSONObject()
                        .put("id", message.id)
                        .put("text", message.text)
                        .put("fromUser", message.fromUser)
                )
            }
            array.put(
                JSONObject()
                    .put("id", conversation.id)
                    .put("title", conversation.title)
                    .put("createdAt", conversation.createdAtEpochMillis)
                    .put("updatedAt", conversation.updatedAtEpochMillis)
                    .put("messages", messages)
            )
        }
        val prefs = preferences(context)
        val previousSnapshot = prefs.getString(KEY_CONVERSATIONS, null)
        val nextSnapshot = array.toString()
        val editor = prefs.edit()
        if (keepPreviousSnapshot) {
            if (!previousSnapshot.isNullOrBlank() && previousSnapshot != nextSnapshot) {
                // Keep one previous snapshot so a partial/corrupt write can be recovered.
                editor.putString(KEY_CONVERSATIONS_BACKUP, previousSnapshot)
            }
        } else {
            // Explicit deletion/clear must not leave recoverable copies of deleted messages.
            editor.remove(KEY_CONVERSATIONS_BACKUP)
        }
        editor.putString(KEY_CONVERSATIONS, nextSnapshot).commit()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
