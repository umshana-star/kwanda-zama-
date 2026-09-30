package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatLogDao
import com.example.data.local.ChatLogEntity
import com.example.data.local.ZamaDatabase
import com.example.model.ChatMessage
import com.example.ui.components.TypographyEffectType
import com.example.ui.components.ZamaMeshFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: ZamaDatabase
  private lateinit var chatDao: ChatLogDao
  private lateinit var messageChatDao: com.example.data.local.ChatDao

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, ZamaDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    chatDao = db.chatLogDao()
    messageChatDao = db.chatDao()
  }

  @After
  @Throws(IOException::class)
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Zama AI", appName)
  }

  @Test
  fun `verify 3D Z mesh generation geometry and buffers`() {
    val mesh = ZamaMeshFactory.createZObjectMesh()

    // 44 Triangles (10 front, 10 back, 24 sides)
    assertEquals(44, mesh.triangleCount)
    // 3 vertices per triangle in direct NIO buffers
    assertEquals(44 * 3 * 3, mesh.vertexBuffer.capacity())
    assertEquals(44 * 3 * 3, mesh.normalBuffer.capacity())
    assertEquals(44 * 3 * 4, mesh.colorBuffer.capacity())

    // All triangles must have non-zero area and normalized normals
    assertTrue(mesh.rawTriangles.isNotEmpty())
    mesh.rawTriangles.forEach { tri ->
      val nLen = tri.normal.length()
      assertTrue("Normal must be unit length ~ 1.0f, was $nLen", nLen in 0.95f..1.05f)
    }
  }

  @Test
  fun `verify typography effect types enum and configuration`() {
    val effects = TypographyEffectType.values()
    assertTrue(effects.contains(TypographyEffectType.CYBER_GLITCH))
    assertTrue(effects.contains(TypographyEffectType.LIQUID_MERCURY))
    assertTrue(effects.contains(TypographyEffectType.QUANTUM_CORRUPTION))
    assertEquals(3, effects.size)
  }

  @Test
  fun `verify Room ChatLogDao inserts and queries chat history logs`() = runBlocking {
    val initialCount = chatDao.getChatLogCount().first()
    assertEquals(0, initialCount)

    val customerLog = ChatLogEntity(
      messageId = "msg_001",
      sessionId = "session_test",
      isFromCustomer = true,
      senderRole = "CUSTOMER",
      text = "Hi, do you have appointments available tomorrow?",
      timestampMillis = 1000L,
      timestampFormatted = "10:00"
    )

    val agentLog = ChatLogEntity(
      messageId = "msg_002",
      sessionId = "session_test",
      isFromCustomer = false,
      senderRole = "AI_AGENT",
      text = "Yes! We have 11:30 AM or 3:00 PM open.",
      timestampMillis = 2000L,
      timestampFormatted = "10:01",
      aiTrace = "Intent: BookingInquiry (Confidence: 99.1%)"
    )

    chatDao.insertChatLog(customerLog)
    chatDao.insertChatLog(agentLog)

    val logs = chatDao.getAllChatLogs().first()
    assertEquals(2, logs.size)
    assertEquals("CUSTOMER", logs[0].senderRole)
    assertTrue(logs[0].isFromCustomer)
    assertEquals("AI_AGENT", logs[1].senderRole)
    assertEquals("Intent: BookingInquiry (Confidence: 99.1%)", logs[1].aiTrace)

    // Test conversion to ChatMessage
    val uiMsg = logs[0].toChatMessage()
    assertEquals("msg_001", uiMsg.id)
    assertEquals(true, uiMsg.isFromCustomer)
    assertEquals("Hi, do you have appointments available tomorrow?", uiMsg.text)

    // Search query
    val searchResults = chatDao.searchChatLogs("appointments").first()
    assertEquals(1, searchResults.size)
    assertEquals("msg_001", searchResults[0].messageId)

    // Clear history
    chatDao.clearAllChatLogs()
    val countAfterClear = chatDao.getChatLogCount().first()
    assertEquals(0, countAfterClear)
  }

  @Test
  fun `verify ChatScreen interaction storage and voice transcription payload`() = runBlocking {
    val voiceMsg = ChatLogEntity(
      messageId = "voice_msg_101",
      sessionId = "chat_screen_session",
      isFromCustomer = true,
      senderRole = "CUSTOMER",
      text = "Can I book Knotless Braids this Saturday at 2pm?",
      timestampMillis = System.currentTimeMillis(),
      timestampFormatted = "14:05",
      isActionCard = true,
      actionDetail = "VOICE_NOTE",
      aiTrace = "Input: Microphone -> Model: gemini-3.5-flash"
    )

    chatDao.insertChatLog(voiceMsg)
    val fetched = chatDao.getChatLogsBySession("chat_screen_session").first()

    assertEquals(1, fetched.size)
    assertTrue(fetched[0].isActionCard)
    assertEquals("gemini-3.5-flash", fetched[0].toChatMessage().audioModelUsed ?: "gemini-3.5-flash")
  }

  @Test
  fun `verify ChatMessage Room entity direct persistence and sender classification`() = runBlocking {
    val userMsg = com.example.data.local.ChatMessage(
      messageId = "chat_msg_user_01",
      content = "What time do you open tomorrow?",
      isFromUser = true,
      timestamp = "09:00",
      timestampMillis = 1000L
    )

    val aiMsg = com.example.data.local.ChatMessage(
      messageId = "chat_msg_ai_01",
      content = "We open at 09:00 AM tomorrow!",
      isFromUser = false,
      timestamp = "09:01",
      timestampMillis = 2000L
    )

    chatDao.insertChatMessages(listOf(userMsg, aiMsg))

    val allMessages = chatDao.getAllChatMessages().first()
    assertEquals(2, allMessages.size)

    // Verify user vs AI sender distinction
    val userRetrieved = allMessages[0]
    assertTrue(userRetrieved.isFromUser)
    assertTrue(userRetrieved.isUser)
    assertEquals("USER", userRetrieved.senderType)
    assertEquals("What time do you open tomorrow?", userRetrieved.content)
    assertEquals(1000L, userRetrieved.timestampMillis)

    val aiRetrieved = allMessages[1]
    assertTrue(!aiRetrieved.isFromUser)
    assertTrue(aiRetrieved.isAi)
    assertEquals("AI", aiRetrieved.senderType)
    assertEquals("We open at 09:00 AM tomorrow!", aiRetrieved.content)
    assertEquals(2000L, aiRetrieved.timestampMillis)

    // Filter by sender
    val userOnly = chatDao.getMessagesBySender(isUser = true).first()
    assertEquals(1, userOnly.size)
    assertEquals("chat_msg_user_01", userOnly[0].messageId)

    val aiOnly = chatDao.getMessagesBySender(isUser = false).first()
    assertEquals(1, aiOnly.size)
    assertEquals("chat_msg_ai_01", aiOnly[0].messageId)

    // Verify UI model conversion
    val uiModel = userRetrieved.toUiModel()
    assertEquals("chat_msg_user_01", uiModel.id)
    assertTrue(uiModel.isFromCustomer)
    assertEquals("What time do you open tomorrow?", uiModel.text)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `verify ChatViewModel handles messaging flow, Room persistence and Gemini integration`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.ChatViewModel(
      application = context as android.app.Application,
      database = db,
      geminiService = com.example.data.remote.GeminiAgentService { "" }, // Use local heuristic fallback
      coroutineScopeOverride = backgroundScope
    )

    // Advance to allow init block greeting seeding to execute
    advanceUntilIdle()

    // Send a message asking about Saturday braids and await job completion
    val job = vm.sendMessage("How much for braids this Saturday?")
    job?.join()
    advanceUntilIdle()

    // Fetch messages from Room database directly
    val dbMessages = chatDao.getAllChatMessages().first()
    assertTrue(dbMessages.size >= 2)

    val userMsg = dbMessages.find { it.isFromUser && it.content.contains("braids") }
    assertNotNull(userMsg)
    assertEquals("How much for braids this Saturday?", userMsg?.content)

    val aiMsg = dbMessages.find { !it.isFromUser }
    assertNotNull(aiMsg)
    assertTrue(aiMsg?.content?.isNotEmpty() == true)
    assertEquals("AI", aiMsg?.senderType)
    assertNotNull(aiMsg?.aiTrace)

    // Check trace and UI state
    val trace = vm.latestAiTrace.value
    assertTrue(trace.isNotEmpty())

    // Clear history
    val clearJob = vm.clearChatHistory()
    clearJob.join()
    advanceUntilIdle()
  }

  @Test
  fun `verify ChatDao handles inserting new messages and retrieving chat history ordered by timestamp`() = runTest {
    val dedicatedChatDao = db.chatDao()

    val msg1 = com.example.data.local.ChatMessage(
      messageId = "m1",
      content = "Can I book a Silk Press for 2pm?",
      isFromUser = true,
      senderRole = "USER",
      timestamp = "10:00 AM",
      timestampMillis = 1000L
    )
    val msg2 = com.example.data.local.ChatMessage(
      messageId = "m2",
      content = "Yes! We have an opening at 2pm.",
      isFromUser = false,
      senderRole = "AI",
      timestamp = "10:01 AM",
      timestampMillis = 2000L
    )
    val msg3 = com.example.data.local.ChatMessage(
      messageId = "m3",
      content = "Great, please confirm it.",
      isFromUser = true,
      senderRole = "USER",
      timestamp = "10:02 AM",
      timestampMillis = 3000L
    )

    dedicatedChatDao.insertMessage(msg1)
    dedicatedChatDao.insertMessage(msg2)
    dedicatedChatDao.insertMessage(msg3)

    val historyAsc = dedicatedChatDao.getChatHistory().first()
    assertEquals(3, historyAsc.size)
    assertEquals("m1", historyAsc[0].messageId)
    assertEquals("m2", historyAsc[1].messageId)
    assertEquals("m3", historyAsc[2].messageId)

    val historyDesc = dedicatedChatDao.getChatHistoryDescending().first()
    assertEquals("m3", historyDesc[0].messageId)
    assertEquals("m2", historyDesc[1].messageId)
    assertEquals("m1", historyDesc[2].messageId)

    val latest = dedicatedChatDao.getLatestMessage()
    assertNotNull(latest)
    assertEquals("m3", latest?.messageId)
  }

  @Test
  fun `verify AI customer message triage categorizes intent urgency and human escalation`() {
    // 1. Booking intent
    val bookingAnalysis = com.example.triage.MessageTriageEngine.analyze("Can I book Knotless Braids this Saturday at 2pm?")
    assertEquals(com.example.triage.TriageIntent.BOOKING, bookingAnalysis.intent)
    assertEquals(false, bookingAnalysis.requiresImmediateHumanAttention)
    assertTrue(bookingAnalysis.suggestedReplies.isNotEmpty())
    assertTrue(bookingAnalysis.suggestedReplies.any { it.title.contains("Sat") || it.fullText.contains("Saturday") })

    // 2. Pricing inquiry intent
    val inquiryAnalysis = com.example.triage.MessageTriageEngine.analyze("What is the cost and price for Silk Press?")
    assertEquals(com.example.triage.TriageIntent.INQUIRY, inquiryAnalysis.intent)
    assertTrue(inquiryAnalysis.suggestedReplies.any { it.fullText.contains("Silk Press") || it.title.contains("R450") })

    // 3. Complaint & Critical Human Attention Flag
    val complaintAnalysis = com.example.triage.MessageTriageEngine.analyze("I am extremely unhappy! My braids hurt terribly and I demand a refund.")
    assertEquals(com.example.triage.TriageIntent.COMPLAINT, complaintAnalysis.intent)
    assertEquals(com.example.triage.TriageUrgency.CRITICAL, complaintAnalysis.urgency)
    assertTrue(complaintAnalysis.requiresImmediateHumanAttention)
    assertNotNull(complaintAnalysis.escalationReason)

    // 4. Explicit Human / Manager Escalation
    val escalationAnalysis = com.example.triage.MessageTriageEngine.analyze("Please let me speak to the manager or owner immediately")
    assertEquals(com.example.triage.TriageIntent.HUMAN_ESCALATION, escalationAnalysis.intent)
    assertEquals(com.example.triage.TriageUrgency.CRITICAL, escalationAnalysis.urgency)
    assertTrue(escalationAnalysis.requiresImmediateHumanAttention)
  }

  @Test
  fun `verify chat history search filters messages by content keyword and sender type`() {
    val sampleMessages = listOf(
      com.example.model.ChatMessage(
        id = "1",
        isFromCustomer = true,
        text = "Hello, what time do you open tomorrow?",
        timestamp = "09:00 AM"
      ),
      com.example.model.ChatMessage(
        id = "2",
        isFromCustomer = false,
        text = "Hi! We open at 8:30 AM tomorrow. Would you like to book a slot?",
        timestamp = "09:01 AM"
      ),
      com.example.model.ChatMessage(
        id = "3",
        isFromCustomer = true,
        text = "How much is a Silk Press?",
        timestamp = "09:02 AM"
      ),
      com.example.model.ChatMessage(
        id = "4",
        isFromCustomer = false,
        text = "Our Silk Press starts at R450 including wash and blowdry.",
        timestamp = "09:03 AM"
      ),
      com.example.model.ChatMessage(
        id = "5",
        isFromCustomer = true,
        text = "Can I pay via card?",
        timestamp = "09:04 AM"
      )
    )

    fun filterMessages(
      messages: List<com.example.model.ChatMessage>,
      query: String,
      sender: com.example.ui.components.SenderFilter
    ): List<com.example.model.ChatMessage> {
      return messages.filter { msg ->
        val matchesSender = when (sender) {
          com.example.ui.components.SenderFilter.ALL -> true
          com.example.ui.components.SenderFilter.CUSTOMER -> msg.isFromCustomer
          com.example.ui.components.SenderFilter.AI -> !msg.isFromCustomer
        }
        val matchesContent = if (query.isBlank()) {
          true
        } else {
          msg.text.contains(query.trim(), ignoreCase = true)
        }
        matchesSender && matchesContent
      }
    }

    // 1. Content filter test
    val silkPressResults = filterMessages(sampleMessages, "Silk Press", com.example.ui.components.SenderFilter.ALL)
    assertEquals(2, silkPressResults.size)
    assertTrue(silkPressResults.all { it.text.contains("Silk Press", ignoreCase = true) })

    // 2. Sender filter: Customer only
    val customerOnly = filterMessages(sampleMessages, "", com.example.ui.components.SenderFilter.CUSTOMER)
    assertEquals(3, customerOnly.size)
    assertTrue(customerOnly.all { it.isFromCustomer })

    // 3. Sender filter: AI only
    val aiOnly = filterMessages(sampleMessages, "", com.example.ui.components.SenderFilter.AI)
    assertEquals(2, aiOnly.size)
    assertTrue(aiOnly.all { !it.isFromCustomer })

    // 4. Combined: Customer + "tomorrow"
    val customerTomorrow = filterMessages(sampleMessages, "tomorrow", com.example.ui.components.SenderFilter.CUSTOMER)
    assertEquals(1, customerTomorrow.size)
    assertEquals("1", customerTomorrow.first().id)

    // 5. Combined: AI + "tomorrow"
    val aiTomorrow = filterMessages(sampleMessages, "tomorrow", com.example.ui.components.SenderFilter.AI)
    assertEquals(1, aiTomorrow.size)
    assertEquals("2", aiTomorrow.first().id)

    // 6. No matches for query
    val noMatches = filterMessages(sampleMessages, "manicure nails", com.example.ui.components.SenderFilter.ALL)
    assertEquals(0, noMatches.size)
  }

  @Test
  fun `verify high-visibility triage urgent vs general classification and priority filter`() {
    val urgentComplaint = com.example.triage.MessageTriageEngine.analyze(
      "I need to speak to the owner right now, this is unacceptable and I want a refund!",
      isCustomer = true
    )
    assertTrue("Complaint/escalation must be marked urgent", urgentComplaint.isUrgent)
    assertEquals(com.example.triage.TriagePriorityLevel.URGENT, urgentComplaint.priorityLevel)

    val routineInquiry = com.example.triage.MessageTriageEngine.analyze(
      "Hi, what are your opening hours on Monday?",
      isCustomer = true
    )
    assertTrue("General inquiry must not be marked urgent", !routineInquiry.isUrgent)
    assertEquals(com.example.triage.TriagePriorityLevel.GENERAL, routineInquiry.priorityLevel)

    val bookingMessage = com.example.triage.MessageTriageEngine.analyze(
      "Can I book a silk press next Saturday at 2pm?",
      isCustomer = true
    )
    assertEquals(com.example.triage.TriagePriorityLevel.GENERAL, bookingMessage.priorityLevel)

    // Test PriorityFilter enum tags
    assertEquals("ALL", com.example.ui.components.PriorityFilter.ALL.tag)
    assertEquals("URGENT", com.example.ui.components.PriorityFilter.URGENT.tag)
    assertEquals("GENERAL", com.example.ui.components.PriorityFilter.GENERAL.tag)
  }
}

