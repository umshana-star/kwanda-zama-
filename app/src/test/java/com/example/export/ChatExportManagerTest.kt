package com.example.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatMessage
import com.example.data.local.WhatsAppInteractionEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatExportManagerTest {

    @Test
    fun testExportToJson_createsValidJsonFileAndIntent() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val sampleMessages = listOf(
            ChatMessage(
                messageId = "msg_001",
                content = "Can I book Knotless Braids this Saturday?",
                isFromUser = true,
                senderRole = "USER",
                timestamp = "14:02",
                statusTicks = "✓✓",
                isVoiceNote = false
            ),
            ChatMessage(
                messageId = "msg_002",
                content = "Hi! We have 2:00 PM available for Saturday. Shall I reserve it?",
                isFromUser = false,
                senderRole = "AI",
                timestamp = "14:02",
                statusTicks = "✓✓",
                aiTrace = "Intent: BookingInquiry, Hold: BK-829",
                intentTag = "BOOKING_INQUIRY"
            )
        )

        val sampleInteractions = listOf(
            WhatsAppInteractionEntity(
                interactionId = "int_001",
                threadId = "+27821234567",
                customerPhone = "+27821234567",
                customerName = "Nomvula",
                userMessage = "Can I book a slot?",
                agentReply = "Hi! We have 2:00 PM available.",
                detectedIntent = "Booking & Appointments",
                confidenceScore = 0.98f,
                latencyMs = 120L,
                aiReasoningTrace = "Booked slot hold",
                requiresHumanHandoff = false,
                interactionStatus = "SENT"
            )
        )

        val (file, intent) = ChatExportManager.exportToJson(context, sampleMessages, sampleInteractions)

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertEquals("application/json", intent.type)

        // Read and parse JSON content
        val jsonString = file.readText(Charsets.UTF_8)
        val root = JSONObject(jsonString)

        assertEquals(2, root.getInt("totalMessages"))
        assertEquals(1, root.getInt("totalInteractions"))

        val messagesArr = root.getJSONArray("messages")
        assertEquals(2, messagesArr.length())
        assertEquals("Can I book Knotless Braids this Saturday?", messagesArr.getJSONObject(0).getString("content"))
        assertEquals("BOOKING_INQUIRY", messagesArr.getJSONObject(1).getString("intentTag"))

        val interactionsArr = root.getJSONArray("interactions")
        assertEquals(1, interactionsArr.length())
        assertEquals("+27821234567", interactionsArr.getJSONObject(0).getString("senderPhoneNumber"))
        assertEquals("Booking & Appointments", interactionsArr.getJSONObject(0).getString("resolvedIntent"))
    }
}
