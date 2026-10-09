package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ZamaDatabaseTest {

    private lateinit var context: Context
    private lateinit var db: ZamaDatabase
    private lateinit var chatDao: ChatDao
    private lateinit var interactionDao: WhatsAppInteractionDao
    private lateinit var logDao: AutonomousAgentLogDao
    private lateinit var conversationDao: ConversationDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ZamaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        chatDao = db.chatDao()
        interactionDao = db.whatsAppInteractionDao()
        logDao = db.autonomousAgentLogDao()
        conversationDao = db.conversationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testChatDao_insertAndRetrieveOrderedMessages() {
        runBlocking {
            val msg1 = ChatMessage(
                messageId = "test_msg_001",
                content = "Hello Zama",
                isFromUser = true,
                timestamp = "10:00",
                timestampMillis = 1000L
            )
            val msg2 = ChatMessage(
                messageId = "test_msg_002",
                content = "Hello! How can I assist you with salon bookings?",
                isFromUser = false,
                timestamp = "10:01",
                timestampMillis = 2000L
            )

            chatDao.insertMessage(msg1)
            chatDao.insertMessage(msg2)

            val history = chatDao.getChatHistory().first()
            assertEquals(2, history.size)
            assertEquals("test_msg_001", history[0].messageId)
            assertEquals("test_msg_002", history[1].messageId)

            val count = chatDao.getMessageCount()
            assertEquals(2, count)

            assertTrue(chatDao.countByMessageId("test_msg_001") == 1)
        }
    }

    @Test
    fun testWhatsAppInteractionDao_insertAndQuery() {
        runBlocking {
            val interaction = WhatsAppInteractionEntity(
                interactionId = "wamid_test_99",
                customerPhone = "+27821112233",
                customerName = "Nomsa Khumalo",
                userMessage = "What are the bridal package rates?",
                agentReply = "Our Bridal Luxury Updo & Makeup starts at R1,400.",
                aiReasoningTrace = "Query matches Bridal Luxury category",
                interactionStatus = "DELIVERED",
                timestampMillis = System.currentTimeMillis()
            )

            val rowId = interactionDao.insertInteraction(interaction)
            assertTrue(rowId > 0)

            val fetched = interactionDao.getInteractionByStringId("wamid_test_99")
            assertNotNull(fetched)
            assertEquals("Nomsa Khumalo", fetched?.customerName)
            assertEquals("DELIVERED", fetched?.interactionStatus)
        }
    }

    @Test
    fun testAutonomousAgentLogDao_persistence() {
        runBlocking {
            val log = AutonomousAgentLogEntity(
                logId = "log_test_01",
                threadId = "thread_01",
                customerPhone = "+27821112233",
                customerName = "Nomsa Khumalo",
                messageText = "Knotless braids pricing",
                agentReply = "Starting price is R650.",
                detectedIntent = "SALON_PRICING",
                confidenceScore = 0.98f,
                latencyMs = 42L,
                timestampMillis = System.currentTimeMillis()
            )

            logDao.insertLog(log)

            val count = logDao.getLogCountSync()
            assertEquals(1, count)
        }
    }

    @Test
    fun testDateConverter_typeMapping() {
        val converter = DateConverter()
        val now = Date()
        val timestamp = converter.dateToTimestamp(now)
        assertNotNull(timestamp)

        val reconstructed = converter.fromTimestamp(timestamp)
        assertEquals(now.time, reconstructed?.time)
    }

    @Test
    fun testDatabase_clearAllTablesPurgesData() {
        runBlocking {
            chatDao.insertMessage(
                ChatMessage(
                    messageId = "purge_test_01",
                    content = "Ephemeral",
                    isFromUser = true,
                    timestamp = "12:00"
                )
            )
            assertEquals(1, chatDao.getMessageCount())

            db.clearAllTables()

            assertEquals(0, chatDao.getMessageCount())
        }
    }

    @Test
    fun testFilePersistence_simulatingAppRestart() {
        runBlocking {
            val dbFile = context.getDatabasePath("test_restart_db.db")
            dbFile.delete()

            // 1. First app session - create database and insert message
            var sessionDb = Room.databaseBuilder(context, ZamaDatabase::class.java, "test_restart_db.db")
                .allowMainThreadQueries()
                .build()
            sessionDb.chatDao().insertMessage(
                ChatMessage(
                    messageId = "restart_persisted_msg",
                    content = "Preserved after process restart",
                    isFromUser = false,
                    timestamp = "14:00"
                )
            )
            sessionDb.close()

            // 2. Second app session - reopen database and verify data persisted
            sessionDb = Room.databaseBuilder(context, ZamaDatabase::class.java, "test_restart_db.db")
                .allowMainThreadQueries()
                .build()
            val restoredMessages = sessionDb.chatDao().getChatHistory().first()
            assertEquals(1, restoredMessages.size)
            assertEquals("restart_persisted_msg", restoredMessages[0].messageId)
            assertEquals("Preserved after process restart", restoredMessages[0].content)
            sessionDb.close()

            dbFile.delete()
        }
    }
}
