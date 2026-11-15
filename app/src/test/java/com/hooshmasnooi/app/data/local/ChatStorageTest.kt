package com.hooshmasnooi.app.data.local

import android.app.Application
import android.content.Context
import com.hooshmasnooi.app.domain.model.ChatMessage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChatStorageTest {
    private val context: Application
        get() = RuntimeEnvironment.getApplication()

    @Before
    fun clearPreferences() {
        context.getSharedPreferences("atlas_private_chat", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun savingMoreThanLimitKeepsNewestThreeHundredMessages() {
        val conversation = ChatStorage.createConversation(context)
        val messages = (1L..350L).map { id ->
            ChatMessage(id, "message-$id", fromUser = true)
        }

        ChatStorage.save(context, messages, conversation.id)

        val saved = ChatStorage.loadConversation(context, conversation.id)
        assertEquals(300, saved.size)
        assertEquals(51L, saved.first().id)
        assertEquals(350L, saved.last().id)
        assertEquals((51L..350L).toList(), saved.map { it.id })
    }

    @Test
    fun conversationTitleDoesNotChangeWhenOldestMessageIsTrimmed() {
        val conversation = ChatStorage.createConversation(context)
        val firstBatch = (1L..350L).map { id ->
            ChatMessage(id, "message-$id", fromUser = true)
        }
        ChatStorage.save(context, firstBatch, conversation.id)
        val originalTitle = ChatStorage.loadAll(context).single().title

        val nextBatch = (51L..351L).map { id ->
            ChatMessage(id, "message-$id", fromUser = true)
        }
        ChatStorage.save(context, nextBatch, conversation.id)

        assertEquals(originalTitle, ChatStorage.loadAll(context).single().title)
        assertEquals(300, ChatStorage.loadConversation(context, conversation.id).size)
    }

    @Test
    fun loadingOlderStoredConversationTrimsOldestMessages() {
        val messages = JSONArray()
        (1L..350L).forEach { id ->
            messages.put(
                JSONObject()
                    .put("id", id)
                    .put("text", "message-$id")
                    .put("fromUser", true)
            )
        }
        val conversation = JSONObject()
            .put("id", "older-conversation")
            .put("title", "Older conversation")
            .put("createdAt", 1L)
            .put("updatedAt", 2L)
            .put("messages", messages)
        context.getSharedPreferences("atlas_private_chat", Context.MODE_PRIVATE)
            .edit()
            .putString("conversations_v2", JSONArray().put(conversation).toString())
            .commit()

        val saved = ChatStorage.loadConversation(context, "older-conversation")
        assertEquals(300, saved.size)
        assertEquals(51L, saved.first().id)
        assertEquals(350L, saved.last().id)
    }

    @Test
    fun corruptedCurrentSnapshotRecoversPreviousConversationSnapshot() {
        val conversation = ChatStorage.createConversation(context)
        ChatStorage.save(
            context,
            listOf(ChatMessage(1L, "first saved message", true)),
            conversation.id
        )
        ChatStorage.save(
            context,
            listOf(
                ChatMessage(1L, "first saved message", true),
                ChatMessage(2L, "newer message", true)
            ),
            conversation.id
        )

        context.getSharedPreferences("atlas_private_chat", Context.MODE_PRIVATE)
            .edit()
            .putString("conversations_v2", "{broken-json")
            .commit()

        val recovered = ChatStorage.loadConversation(context, conversation.id)
        assertEquals(1, recovered.size)
        assertEquals("first saved message", recovered.single().text)
        val restoredJson = context.getSharedPreferences("atlas_private_chat", Context.MODE_PRIVATE)
            .getString("conversations_v2", null)
        assertEquals(true, restoredJson?.startsWith("[") == true)
    }

    @Test
    fun missingCurrentSnapshotRecoversPreviousConversationSnapshot() {
        val conversation = ChatStorage.createConversation(context)
        ChatStorage.save(context, listOf(ChatMessage(1L, "recover me", true)), conversation.id)
        ChatStorage.save(
            context,
            listOf(ChatMessage(1L, "recover me", true), ChatMessage(2L, "latest", true)),
            conversation.id
        )

        context.getSharedPreferences("atlas_private_chat", Context.MODE_PRIVATE)
            .edit()
            .putString("conversations_v2", "")
            .commit()

        val recovered = ChatStorage.loadConversation(context, conversation.id)
        assertEquals(listOf("recover me"), recovered.map { it.text })
    }

    @Test
    fun migratingLegacyHistoryKeepsNewestThreeHundredMessages() {
        val messages = JSONArray()
        (1L..350L).forEach { id ->
            messages.put(
                JSONObject()
                    .put("id", id)
                    .put("text", "message-$id")
                    .put("fromUser", true)
            )
        }
        context.getSharedPreferences("atlas_private_chat", Context.MODE_PRIVATE)
            .edit()
            .putString("messages", messages.toString())
            .commit()

        val migrated = ChatStorage.loadAll(context).single()
        assertEquals(300, migrated.messages.size)
        assertEquals(51L, migrated.messages.first().id)
        assertEquals(350L, migrated.messages.last().id)
    }
}
