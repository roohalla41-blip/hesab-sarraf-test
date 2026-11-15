package com.hooshmasnooi.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hooshmasnooi.app.domain.model.ChatMessage
import com.hooshmasnooi.app.domain.model.Conversation
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
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
    private lateinit var legacyFile: File
    private lateinit var encryptedFile: File
    private lateinit var legacyCorrupt: File
    private lateinit var encryptedCorrupt: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        legacyFile = File(context.filesDir, "atlas_chat_history.json")
        encryptedFile = File(context.filesDir, "atlas_chat_history.enc")
        legacyCorrupt = File(context.filesDir, "atlas_chat_history.json.corrupt")
        encryptedCorrupt = File(context.filesDir, "atlas_chat_history.corrupt.enc")
        clearFiles()
        repository = JsonChatHistoryRepository(context, TestHistoryCipher())
    }

    @After
    fun tearDown() = clearFiles()

    private fun clearFiles() {
        listOf(
            "atlas_chat_history.json",
            "atlas_chat_history.json.corrupt",
            "atlas_chat_history.enc",
            "atlas_chat_history.corrupt.enc",
            "atlas_chat_history.legacy-corrupt.enc",
            "atlas_chat_history.enc.tmp",
            "atlas_chat_history.corrupt.enc.tmp",
            "atlas_chat_history.legacy-corrupt.enc.tmp"
        ).forEach { File(context.filesDir, it).delete() }
    }

    @Test
    fun savesAndRestoresConversationAndMessagesInEncryptedFile() = runBlocking {
        val item = conversation("conversation-1")
        repository.saveConversation(item)
        repository.saveMessage(item.id, ChatMessage(1L, "سلام اطلس", true))
        repository.saveMessage(item.id, ChatMessage(2L, "پاسخ آزمایشی", false))

        assertEquals(listOf(item.id), repository.getConversations().map { it.id })
        assertEquals(listOf("سلام اطلس", "پاسخ آزمایشی"), repository.getMessages(item.id).map { it.text })
        assertEquals(2, repository.getConversations().single().messageCount)
        assertTrue(encryptedFile.exists())
        assertFalse(legacyFile.exists())
        assertFalse(encryptedFile.readBytes().toString(Charsets.UTF_8).contains("سلام اطلس"))
    }

    @Test
    fun migratesLegacyJsonAndDeletesPlaintextOnlyAfterVerification() = runBlocking {
        val legacyJson = """{"conversations":[{"id":"old-1","title":"گفت‌وگوی قدیمی","createdAtEpochMillis":1,"updatedAtEpochMillis":2,"messageCount":1,"isPinned":false,"messages":[{"id":9,"text":"پیام قدیمی محرمانه","fromUser":true}]}]}"""
        legacyFile.writeText(legacyJson)

        assertEquals(listOf("old-1"), repository.getConversations().map { it.id })
        assertEquals("پیام قدیمی محرمانه", repository.getMessages("old-1").single().text)
        assertTrue(encryptedFile.exists())
        assertFalse(legacyFile.exists())
        assertFalse(encryptedFile.readBytes().toString(Charsets.UTF_8).contains("پیام قدیمی محرمانه"))
    }

    @Test
    fun updatingConversationMetadataPreservesMessages() = runBlocking {
        val item = conversation("conversation-1")
        repository.saveConversation(item)
        repository.saveMessage(item.id, ChatMessage(1L, "پیام اول", true))
        repository.saveConversation(item.copy(title = "عنوان تازه", updatedAtEpochMillis = 5L))

        assertEquals("عنوان تازه", repository.getConversations().single().title)
        assertEquals(10L, repository.getConversations().single().updatedAtEpochMillis)
        assertEquals(1, repository.getConversations().single().messageCount)
        assertEquals(listOf("پیام اول"), repository.getMessages(item.id).map { it.text })
    }

    @Test
    fun ignoresDuplicateMessageIds() = runBlocking {
        val item = conversation("duplicate")
        repository.saveConversation(item)
        repository.saveMessage(item.id, ChatMessage(7L, "نسخه اول", true))
        repository.saveMessage(item.id, ChatMessage(7L, "نسخه تکراری", true))
        assertEquals(listOf("نسخه اول"), repository.getMessages(item.id).map { it.text })
    }

    @Test
    fun atomicAppendIgnoresDuplicateMessageIds() = runBlocking {
        val item = conversation("atomic-duplicate")
        repository.saveConversationAndMessage(item, ChatMessage(7L, "نسخه اول", true))
        repository.saveConversationAndMessage(item.copy(title = "عنوان تازه"), ChatMessage(7L, "تکراری", true))
        assertEquals(listOf("نسخه اول"), repository.getMessages(item.id).map { it.text })
        assertEquals(1, repository.getConversations().single().messageCount)
        assertEquals("عنوان تازه", repository.getConversations().single().title)
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
        repeat(45) { repository.saveConversation(conversation("conversation-$it", it.toLong())) }
        val latest = conversation("latest")
        repository.saveConversation(latest)
        repeat(305) { index ->
            repository.saveConversationAndMessage(latest.copy(updatedAtEpochMillis = index.toLong()),
                ChatMessage(index.toLong(), "message-$index", true))
        }
        assertEquals(40, repository.getConversations().size)
        val messages = repository.getMessages(latest.id)
        assertEquals(300, messages.size)
        assertEquals(300, repository.getConversations().single().messageCount)
        assertEquals("message-5", messages.first().text)
        assertEquals("message-304", messages.last().text)
    }

    @Test
    fun malformedLegacyFileIsPreservedAsEncryptedRecoveryCopy() = runBlocking {
        legacyFile.writeText("{broken-json")
        assertTrue(repository.getConversations().isEmpty())
        assertTrue(encryptedCorrupt.exists())
        assertFalse(legacyFile.exists())
        assertFalse(encryptedCorrupt.readBytes().toString(Charsets.UTF_8).contains("broken-json"))

        repository.deleteAllConversations()
        assertTrue(repository.getConversations().isEmpty())
        assertFalse(encryptedCorrupt.exists())
    }

    @Test
    fun clearAllRemovesEveryConversationAndEncryptedRecoveryCopy() = runBlocking {
        repository.saveConversation(conversation("one"))
        repository.saveConversation(conversation("two"))
        repository.saveMessage("one", ChatMessage(1L, "private text", true))
        repository.deleteAllConversations()
        assertTrue(repository.getConversations().isEmpty())
        assertTrue(repository.getMessages("one").isEmpty())
        assertFalse(encryptedCorrupt.exists())
    }

    @Test
    fun migrationFailureLeavesLegacyPlaintextUntouchedForRetry() = runBlocking {
        legacyFile.writeText("""{"conversations":[]}""")
        val failing = JsonChatHistoryRepository(context, FailingHistoryCipher())
        val result = runCatching { runBlocking { failing.getConversations() } }
        assertTrue(result.isFailure)
        assertTrue(legacyFile.exists())
        assertFalse(encryptedFile.exists())
    }

    @Test
    fun corruptedEncryptedHistoryFailsClosedInsteadOfSilentlyErasingData() = runBlocking {
        encryptedFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        val result = runCatching { repository.getConversations() }
        assertTrue(result.isFailure)
        assertTrue(encryptedFile.exists())
    }

    @Test
    fun ignoresMessagesForUnknownConversationIds() = runBlocking {
        repository.saveMessage("missing", ChatMessage(1L, "orphan", true))
        assertTrue(repository.getConversations().isEmpty())
        assertTrue(repository.getMessages("missing").isEmpty())
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
                    repository.saveConversationAndMessage(item.copy(updatedAtEpochMillis = id),
                        ChatMessage(id, "message-$id", true))
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
                    repository.saveConversationAndMessage(item.copy(updatedAtEpochMillis = id),
                        ChatMessage(id, "message-$id", true))
                }
            }.forEach { it.await() }
        }
        val saved = repository.getMessages(item.id)
        assertEquals(300, saved.size)
        assertEquals(50L, saved.first().id)
        assertEquals(349L, saved.last().id)
        assertEquals((50L until 350L).toList(), saved.map { it.id })
    }

    private fun conversation(id: String, updatedAt: Long = 10L) = Conversation(
        id = id, title = id, createdAtEpochMillis = updatedAt, updatedAtEpochMillis = updatedAt
    )

    private class TestHistoryCipher : HistoryCipher {
        override fun encrypt(plainText: ByteArray): ByteArray =
            byteArrayOf(0x54, 0x45, 0x53, 0x54) + plainText.map { (it.toInt() xor 0x5a).toByte() }.toByteArray()

        override fun decrypt(envelope: ByteArray): ByteArray {
            require(envelope.size >= 4 && envelope.copyOfRange(0, 4).contentEquals(byteArrayOf(0x54, 0x45, 0x53, 0x54)))
            return envelope.copyOfRange(4, envelope.size).map { (it.toInt() xor 0x5a).toByte() }.toByteArray()
        }
    }

    private class FailingHistoryCipher : HistoryCipher {
        override fun encrypt(plainText: ByteArray): ByteArray = error("simulated encryption failure")
        override fun decrypt(envelope: ByteArray): ByteArray = error("simulated decryption failure")
    }
}
