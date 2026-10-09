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
class ConversationEntityTest {

    private lateinit var database: ZamaDatabase
    private lateinit var conversationDao: ConversationDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ZamaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        conversationDao = database.conversationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertConversationAndMessages_andQueryRelational() = runBlocking {
        val conversation = Conversation(
            conversationId = "conv_whatsapp_001",
            customerPhoneNumber = "+27821112233",
            customerName = "Lerato Moloi",
            lastMessageText = "Can I book Knotless Braids?",
            lastMessageTimestamp = 1700000000000L,
            unreadCount = 1,
            isArchived = false,
            isPinned = true,
            activeIntent = "BOOKING_INQUIRY"
        )

        conversationDao.insertConversation(conversation)

        val message1 = Message(
            messageId = "msg_001",
            conversationId = conversation.conversationId,
            content = "Hi Zama! Can I book Knotless Braids this Saturday?",
            sender = "USER",
            isFromUser = true,
            timestampMillis = 1700000000000L,
            timestampFormatted = "10:00",
            status = "READ",
            isVoiceNote = false
        )

        val message2 = Message(
            messageId = "msg_002",
            conversationId = conversation.conversationId,
            content = "Hello Lerato! We have Saturday 2:00 PM available. Shall I reserve it for you?",
            sender = "AI",
            isFromUser = false,
            timestampMillis = 1700000001000L,
            timestampFormatted = "10:00",
            status = "DELIVERED",
            aiReasoningTrace = "Intent: BookingInquiry, confidence: 99%",
            intentTag = "BOOKING_INQUIRY"
        )

        conversationDao.insertMessages(listOf(message1, message2))

        // Verify conversation querying
        val conversations = conversationDao.getAllConversations().first()
        assertEquals(1, conversations.size)
        assertEquals("Lerato Moloi", conversations[0].customerName)
        assertTrue(conversations[0].isPinned)

        // Verify messages querying
        val messages = conversationDao.getMessagesForConversation(conversation.conversationId).first()
        assertEquals(2, messages.size)
        assertEquals("Hi Zama! Can I book Knotless Braids this Saturday?", messages[0].content)
        assertEquals("AI", messages[1].sender)

        // Verify 1-to-N relation query (ConversationWithMessages)
        val convWithMsgs = conversationDao.getConversationWithMessages(conversation.conversationId).first()
        assertNotNull(convWithMsgs)
        assertEquals(conversation.conversationId, convWithMsgs?.conversation?.conversationId)
        assertEquals(2, convWithMsgs?.messages?.size)

        // Verify update last message
        conversationDao.updateLastMessage(
            id = conversation.conversationId,
            lastMessage = "Yes please, book Saturday 2pm!",
            timestamp = 1700000002000L,
            activeIntent = "BOOKING_INQUIRY"
        )
        val updatedConv = conversationDao.getConversationById(conversation.conversationId).first()
        assertEquals("Yes please, book Saturday 2pm!", updatedConv?.lastMessageText)
    }

    @Test
    fun testCascadeDeleteConversation_removesMessages() = runBlocking {
        val conversation = Conversation(
            conversationId = "conv_whatsapp_002",
            customerPhoneNumber = "+27834445566",
            customerName = "Kagiso Dube",
            lastMessageText = "Hello",
            lastMessageTimestamp = 1700000000000L
        )
        conversationDao.insertConversation(conversation)

        val message = Message(
            messageId = "msg_003",
            conversationId = conversation.conversationId,
            content = "Hello Zama",
            sender = "USER",
            isFromUser = true,
            timestampMillis = 1700000000000L
        )
        conversationDao.insertMessage(message)

        // Verify existence
        val beforeDelete = conversationDao.getMessagesForConversation(conversation.conversationId).first()
        assertEquals(1, beforeDelete.size)

        // Delete conversation
        conversationDao.deleteConversation(conversation)

        // Verify cascade deletion of child messages
        val afterDelete = conversationDao.getMessagesForConversation(conversation.conversationId).first()
        assertEquals(0, afterDelete.size)

        val convAfter = conversationDao.getConversationById(conversation.conversationId).first()
        assertNull(convAfter)
    }

    @Test
    fun testDateConverter_mapsDateToTimestampAndBack() {
        val converter = DateConverter()
        val now = java.util.Date(1700000000000L)

        val timestamp = converter.dateToTimestamp(now)
        assertEquals(1700000000000L, timestamp)

        val reconstructedDate = converter.fromTimestamp(timestamp)
        assertEquals(now, reconstructedDate)

        assertNull(converter.dateToTimestamp(null))
        assertNull(converter.fromTimestamp(null))
    }

    @Test
    fun testConversationDatabase_persistsDateFieldsAccurately() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dedicatedDb = Room.inMemoryDatabaseBuilder(context, ConversationDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = dedicatedDb.conversationDao()

        val expectedDate = java.util.Date(1705000000000L)
        val conversation = Conversation(
            conversationId = "conv_date_001",
            customerPhoneNumber = "+27829990011",
            customerName = "Thandiwe Khumalo",
            lastMessageText = "See you tomorrow!",
            lastMessageTimestamp = expectedDate.time,
            lastMessageDate = expectedDate
        )
        dao.insertConversation(conversation)

        val message = Message(
            messageId = "msg_date_001",
            conversationId = conversation.conversationId,
            content = "Confirmed for 10am",
            sender = "AI",
            isFromUser = false,
            timestampMillis = expectedDate.time,
            timestampDate = expectedDate
        )
        dao.insertMessage(message)

        val retrievedConv = dao.getConversationById("conv_date_001").first()
        assertNotNull(retrievedConv)
        assertEquals(expectedDate, retrievedConv?.lastMessageDate)
        assertEquals(expectedDate.time, retrievedConv?.lastMessageTimestamp)

        val retrievedMsg = dao.getMessageById("msg_date_001")
        assertNotNull(retrievedMsg)
        assertEquals(expectedDate, retrievedMsg?.timestampDate)
        assertEquals(expectedDate.time, retrievedMsg?.timestampMillis)

        dedicatedDb.close()
    }
}
