package com.hooshmasnooi.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hooshmasnooi.app.domain.model.ChatMessage
import com.hooshmasnooi.app.domain.model.Conversation
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JsonChatHistoryRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: JsonChatHistoryRepository
    private lateinit var historyFile: File
    private lateinit var corruptBackup: File
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        historyFile = File(context.filesDir, "atlas_chat_history.json")
        corruptBackup = File(context.filesDir, "atlas_chat_history.json.corrupt")
        tempFile = File(context.filesDir, "atlas_chat_history.json.tmp")
        historyFile.delete()
        corruptBackup.delete()
        tempFile.delete()
        repository = JsonChatHistoryRepository(context)
    }

    @After
    fun tearDown() {
        historyFile.delete()
        corruptBackup.delete()
        tempFile.delete()
    }

    @Test
    fun savesAndRestoresConversationAndMessages() = runBlocking {
        val conversation = conversation("conversation-1")
        repository.saveConversation(conversation)
        repository.saveMessage(conversation.id, ChatMessage(1L, "سلام اطلس", true))
        repository.saveMessage(conversation.id, ChatMessage(2L, "پاسخ آزمایشی", false))

        assertEquals(listOf(conversation.id), repository.getConversations().map { it.id })
        assertEquals(
            listOf("سلام اطلس", "پاسخ آزمایشی"),
            repository.getMessages(conversation.id).map { it.text }
        )
        assertEquals(2, repository.getConversations().single().messageCount)
    }

    @Test
    fun updatingConversationMetadataPreservesExistingMessages() = runBlocking {
        val original = conversation("conversation-1")
        repository.saveConversation(original)
        repository.saveMessage(original.id, ChatMessage(1L, "پیام اول", true))

        repository.saveConversation(original.copy(title = "عنوان تازه", updatedAtEpochMillis = 20L))

        assertEquals("عنوان تازه", repository.getConversations().single().title)
        assertEquals(listOf("پیام اول"), repository.getMessages(original.id).map { it.text })
    }

    @Test
    fun ignoresDuplicateMessageIds() = runBlocking {
        val item = conversation("conversation-1")
        repository.saveConversation(item)
        repository.saveMessage(item.id, ChatMessage(7L, "نسخه اول", true))
        repository.saveMessage(item.id, ChatMessage(7L, "نسخه تکراری", true))

        assertEquals(listOf("نسخه اول"), repository.getMessages(item.id).map { it.text })
    }

    @Test
    fun deletesOneConversationWithoutRemovingOthers() = runBlocking {
        repository.saveConversation(conversation("one"))
        repository.saveConversation(conversation("two"))

        repository.deleteConversation("one")

        assertEquals(listOf("two"), repository.getConversations().map { it.id })
    }

    @Test
    fun boundsConversationAndMessageRetention() = runBlocking {
        repeat(45) { index ->
            val item = conversation("conversation-$index", updatedAt = index.toLong())
            repository.saveConversation(item)
        }
        val latest = conversation("latest")
        repository.saveConversation(latest)
        repeat(305) { index ->
            repository.saveConversationAndMessage(
                latest.copy(updatedAtEpochMillis = index.toLong()),
                ChatMessage(index.toLong(), "message-$index", true)
            )
        }

        assertEquals(40, repository.getConversations().size)
        val messages = repository.getMessages(latest.id)
        assertEquals(300, messages.size)
        assertEquals("message-5", messages.first().text)
        assertEquals("message-304", messages.last().text)
    }

    @Test
    fun preservesMalformedFileForRecoveryAndClearAllRemovesIt() = runBlocking {
        historyFile.writeText("{broken-json")

        assertTrue(repository.getConversations().isEmpty())
        assertTrue(corruptBackup.exists())

        repository.deleteAllConversations()

        assertTrue(repository.getConversations().isEmpty())
        assertFalse(corruptBackup.exists())
        assertTrue(historyFile.exists())
    }

    @Test
    fun clearAllRemovesEveryConversation() = runBlocking {
        repository.saveConversation(conversation("one"))
        repository.saveConversation(conversation("two"))
        repository.saveMessage("one", ChatMessage(1L, "private text", true))

        repository.deleteAllConversations()

        assertTrue(repository.getConversations().isEmpty())
        assertTrue(repository.getMessages("one").isEmpty())
    }

    @Test
    fun ignoresMessagesForUnknownConversationIds() = runBlocking {
        repository.saveMessage("missing-conversation", ChatMessage(1L, "orphan", true))

        assertTrue(repository.getConversations().isEmpty())
        assertTrue(repository.getMessages("missing-conversation").isEmpty())
    }

    @Test
    fun truncatesOversizedTitlesAndMessageBodies() = runBlocking {
        val item = conversation("long-title").copy(title = "T".repeat(120))
        repository.saveConversation(item)
        repository.saveMessage(item.id, ChatMessage(1L, "M".repeat(13_000), true))

        assertEquals(80, repository.getConversations().single().title.length)
        assertEquals(12_000, repository.getMessages(item.id).single().text.length)
    }

    @Test
    fun serializesConcurrentMessageWritesWithoutLosingEntries() = runBlocking {
        val item = conversation("concurrent")
        repository.saveConversation(item)

        kotlinx.coroutines.coroutineScope {
            (1L..20L).map { id ->
                async(kotlinx.coroutines.Dispatchers.Default) {
                    repository.saveConversationAndMessage(
                        item.copy(updatedAtEpochMillis = id),
                        ChatMessage(id, "message-$id", true)
                    )
                }
            }.forEach { it.await() }
        }

        val saved = repository.getMessages(item.id)
        assertEquals(20, saved.size)
        assertEquals((1L..20L).toSet(), saved.map { it.id }.toSet())
    }

    @Test
    fun concurrentOverflowKeepsNewestMessagesInChronologicalOrder() = runBlocking {
        val item = conversation("concurrent-overflow")
        repository.saveConversation(item)

        kotlinx.coroutines.coroutineScope {
            (0L until 350L).map { id ->
                async(kotlinx.coroutines.Dispatchers.Default) {
                    repository.saveConversationAndMessage(
                        item.copy(updatedAtEpochMillis = id),
                        ChatMessage(id, "message-$id", true)
                    )
                }.also { }
            }.forEach { it.await() }
        }

        val saved = repository.getMessages(item.id)
        assertEquals(300, saved.size)
        assertEquals(50L, saved.first().id)
        assertEquals(349L, saved.last().id)
        assertEquals((50L until 350L).toList(), saved.map { it.id })
    }

    private fun conversation(id: String, updatedAt: Long = 10L) = Conversation(
        id = id,
        title = id,
        createdAtEpochMillis = updatedAt,
        updatedAtEpochMillis = updatedAt
    )
}
