package com.hooshmasnooi.app.data.local

import android.content.Context
import com.hooshmasnooi.app.domain.model.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Private, on-device persistence for the active conversation.
 * No message content is sent to a cloud database by this store.
 */
object ChatStorage {
    private const val PREFS = "atlas_private_chat"
    private const val KEY_MESSAGES = "messages"
    private const val MAX_MESSAGES = 2000

    fun load(context: Context): List<ChatMessage> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MESSAGES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until minOf(array.length(), MAX_MESSAGES)) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optLong("id", index.toLong() + 1L)
                    val text = item.optString("text", "")
                    if (text.isNotBlank()) {
                        add(ChatMessage(id = id, text = text, fromUser = item.optBoolean("fromUser", true)))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.takeLast(MAX_MESSAGES).forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("text", message.text)
                    .put("fromUser", message.fromUser)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MESSAGES, array.toString())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_MESSAGES)
            .apply()
    }
}
