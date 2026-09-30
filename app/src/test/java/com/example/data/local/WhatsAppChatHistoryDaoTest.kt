package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WhatsAppChatHistoryDaoTest {

    private lateinit var database: ZamaDatabase
    private lateinit var chatHistoryDao: WhatsAppChatHistoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ZamaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        chatHistoryDao = database.whatsAppChatHistoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun appendMessageToThread_persistsUserAndAgentTurnsAndUpdatesThreadMetadata() = runBlocking {
        val threadId = "thread_thandiwe_naledi_01"
        val userTurn = WhatsAppChatMessageEntity(
            messageId = "wamid.user.001",
            threadId = threadId,
            agentId = "agent_thandiwe_01",
            agentName = "Thandiwe",
            customerPhone = "+27 82 419 8820",
            customerName = "Naledi Khumalo",
            isFromUser = true,
            senderRole = "USER",
            content = "Hi Thandiwe, do you have a Saturday 2pm slot for Knotless Braids?",
            timestampMillis = 1700000001000L,
            formattedTime = "14:00",
            detectedIntent = "BOOKING_TRIAGE"
        )

        chatHistoryDao.appendMessageToThread(
            message = userTurn,
            topicSummary = "Knotless Braids Booking"
        )

        // Verify thread state after user message (unread_count = 1, message_count = 1)
        val afterUserThread = chatHistoryDao.getThreadById(threadId)
        assertNotNull(afterUserThread)
        assertEquals(1, afterUserThread?.messageCount)
        assertEquals(1, afterUserThread?.unreadCount)
        assertEquals("USER", afterUserThread?.lastMessageSender)

        // Autonomous WhatsApp Agent responds
        val agentTurn = WhatsAppChatMessageEntity(
            messageId = "wamid.agent.002",
            threadId = threadId,
            agentId = "agent_thandiwe_01",
            agentName = "Thandiwe",
            customerPhone = "+27 82 419 8820",
            customerName = "Naledi Khumalo",
            isFromUser = false,
            senderRole = "WHATSAPP_AGENT",
            content = "Warm greetings Naledi! Yes, Saturday at 14:00 is available for Knotless Braids (R650). Slot #BK-749 is held for you.",
            timestampMillis = 1700000002000L,
            formattedTime = "14:00",
            detectedIntent = "BOOKING_TRIAGE",
            confidenceScore = 0.98f,
            latencyMs = 640L,
            aiReasoningTrace = "Catalog: Knotless Braids • Calendar: #BK-749 Free",
            reservedSlotId = "BK-749"
        )

        chatHistoryDao.appendMessageToThread(message = agentTurn)

        // Verify relational WhatsAppThreadWithMessages
        val threadWithMessages = chatHistoryDao.getThreadWithMessages(threadId)
        assertNotNull(threadWithMessages)
        assertEquals(2, threadWithMessages?.thread?.messageCount)
        assertEquals(0, threadWithMessages?.thread?.unreadCount)
        assertEquals("WHATSAPP_AGENT", threadWithMessages?.thread?.lastMessageSender)
        assertEquals("BK-749", threadWithMessages?.thread?.reservedSlotId)

        val ordered = threadWithMessages!!.chronologicalMessages
        assertEquals(2, ordered.size)
        assertTrue(ordered[0].isFromUser)
        assertEquals("wamid.user.001", ordered[0].messageId)
        assertEquals(false, ordered[1].isFromUser)
        assertEquals("wamid.agent.002", ordered[1].messageId)
        assertEquals("BK-749", ordered[1].reservedSlotId)
    }

    @Test
    fun multipleThreads_filterByAgentSearchMessagesAndDeleteThreadAtomically() = runBlocking {
        chatHistoryDao.appendMessageToThread(
            message = WhatsAppChatMessageEntity(
                messageId = "wamid.t1.1",
                threadId = "thread_1",
                agentId = "agent_thandiwe_01",
                agentName = "Thandiwe",
                customerPhone = "+27 82 111 2222",
                customerName = "Zanele M.",
                isFromUser = true,
                content = "How much is a luxury silk press treatment?",
                timestampMillis = 1700000010000L,
                formattedTime = "10:15"
            ),
            topicSummary = "Silk Press Pricing"
        )

        chatHistoryDao.appendMessageToThread(
            message = WhatsAppChatMessageEntity(
                messageId = "wamid.t2.1",
                threadId = "thread_2",
                agentId = "agent_sipho_02",
                agentName = "Sipho",
                customerPhone = "+27 83 333 4444",
                customerName = "Kabelo D.",
                isFromUser = false,
                content = "Your Fade & Beard trim is confirmed for Friday at 11:30.",
                timestampMillis = 1700000020000L,
                formattedTime = "10:20",
                reservedSlotId = "BK-812"
            ),
            topicSummary = "Fade & Beard Confirmation"
        )

        val allThreads = chatHistoryDao.getAllThreads().first()
        assertEquals(2, allThreads.size)
        // Most recent thread first
        assertEquals("thread_2", allThreads[0].threadId)
        assertEquals("thread_1", allThreads[1].threadId)

        // Filter by agent
        val siphoThreads = chatHistoryDao.getThreadsByAgent("agent_sipho_02").first()
        assertEquals(1, siphoThreads.size)
        assertEquals("Kabelo D.", siphoThreads[0].customerName)

        // Search messages across threads
        val silkSearch = chatHistoryDao.searchMessages("silk press").first()
        assertEquals(1, silkSearch.size)
        assertEquals("thread_1", silkSearch[0].threadId)

        // Delete thread_1 atomically
        chatHistoryDao.deleteThreadAndMessages("thread_1")
        assertNull(chatHistoryDao.getThreadById("thread_1"))
        assertEquals(0, chatHistoryDao.getMessagesForThreadSync("thread_1").size)
        assertEquals(1, chatHistoryDao.getThreadCountSync())
    }
}
