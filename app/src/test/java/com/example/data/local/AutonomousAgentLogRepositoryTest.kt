package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutonomousAgentLogRepositoryTest {

    private lateinit var database: ZamaDatabase
    private lateinit var logDao: AutonomousAgentLogDao
    private lateinit var repository: AutonomousAgentLogRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ZamaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        logDao = database.autonomousAgentLogDao()
        repository = AutonomousAgentLogRepository(logDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRecordAndRetrieveLog() = runBlocking {
        val entity = repository.logAgentInteraction(
            threadId = "+27821112233",
            customerPhone = "+27821112233",
            customerName = "Kwandiswa",
            messageText = "Hi, do you do loc extensions?",
            agentReply = "Hello Kwandiswa! Yes, our master stylists specialize in loc extensions and maintenance.",
            detectedIntent = "SERVICE_INQUIRY",
            confidenceScore = 0.98f,
            reasoningTrace = "Loc catalog checked; slot availability verified.",
            latencyMs = 450L,
            deliveryStatus = "DELIVERED",
            agentState = "AUTONOMOUS",
            isEscalatedToHuman = false,
            quickActions = listOf("View Pricing", "Book Consultation")
        )

        assertNotNull(entity.logId)
        val allLogs = repository.allLogs.first()
        assertEquals(1, allLogs.size)
        assertEquals("+27821112233", allLogs[0].customerPhone)
        assertEquals("AUTONOMOUS", allLogs[0].agentState)
        assertEquals("SERVICE_INQUIRY", allLogs[0].detectedIntent)
        assertEquals(1, repository.logCount.first())
    }

    @Test
    fun testFilterByAgentStateAndEscalations() = runBlocking {
        // Record regular autonomous log
        repository.logAgentInteraction(
            threadId = "+27820000001",
            customerPhone = "+27820000001",
            customerName = "Sarah",
            messageText = "What time do you open?",
            agentReply = "We open at 08:00 AM Monday through Saturday.",
            detectedIntent = "HOURS_INQUIRY",
            agentState = "AUTONOMOUS"
        )

        // Record away mode log
        repository.logAgentInteraction(
            threadId = "+27820000002",
            customerPhone = "+27820000002",
            customerName = "Mandy",
            messageText = "Are you available now?",
            agentReply = "Hi Mandy! Our team is currently away from the desk. We will attend to your inquiry shortly.",
            detectedIntent = "Away Auto-Reply",
            agentState = "AWAY"
        )

        // Record escalated log
        repository.logAgentInteraction(
            threadId = "+27820000003",
            customerPhone = "+27820000003",
            customerName = "Thabo",
            messageText = "I want a full refund immediately!",
            agentReply = "I have flagged this for the salon manager to call you promptly.",
            detectedIntent = "REFUND_DISPUTE",
            agentState = "ESCALATED",
            isEscalatedToHuman = true,
            escalationReason = "Customer requested urgent refund handoff"
        )

        val awayLogs = repository.getLogsByState("AWAY").first()
        assertEquals(1, awayLogs.size)
        assertEquals("+27820000002", awayLogs[0].customerPhone)

        val escalatedLogs = repository.escalatedLogs.first()
        assertEquals(1, escalatedLogs.size)
        assertTrue(escalatedLogs[0].isEscalatedToHuman)
        assertEquals("Thabo", escalatedLogs[0].customerName)
    }

    @Test
    fun testUpdateDeliveryStatusAndAgentState() = runBlocking {
        val entity = repository.logAgentInteraction(
            threadId = "+27845556677",
            customerPhone = "+27845556677",
            customerName = "Lerato",
            messageText = "Booking inquiry",
            agentReply = "Tentative slot held",
            detectedIntent = "BOOKING_INQUIRY",
            deliveryStatus = "SENT",
            agentState = "AUTONOMOUS"
        )

        // Update delivery status to READ
        repository.updateDeliveryStatus(entity.logId, "READ")
        // Update state to AWAY
        repository.updateAgentState(entity.logId, "AWAY")

        val updated = repository.getLogsByThread("+27845556677").first().first()
        assertEquals("READ", updated.deliveryStatus)
        assertEquals("AWAY", updated.agentState)
    }

    @Test
    fun testSearchLogs() = runBlocking {
        repository.logAgentInteraction(
            threadId = "+27823334455",
            customerPhone = "+27823334455",
            customerName = "Zonke",
            messageText = "Need Bohemian Knotless Braids",
            agentReply = "Bohemian Knotless Braids starting at R1200",
            detectedIntent = "PRICING_REQUEST"
        )

        val searchResult = repository.searchLogs("Bohemian").first()
        assertEquals(1, searchResult.size)
        assertEquals("Zonke", searchResult[0].customerName)

        val noMatch = repository.searchLogs("NonExistentTreatment").first()
        assertEquals(0, noMatch.size)
    }
}
