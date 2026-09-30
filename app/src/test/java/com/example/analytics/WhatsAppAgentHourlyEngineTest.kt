package com.example.analytics

import com.example.model.AgentOperationalStatus
import com.example.model.AgentSpecialization
import com.example.model.WhatsAppAgentEntity
import com.example.model.WhatsAppAgentEventEntity
import com.example.model.WhatsAppAgentEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WhatsAppAgentHourlyEngineTest {

    private val testAgents = listOf(
        WhatsAppAgentEntity(
            id = "agent_thandiwe_01",
            name = "Thandiwe",
            specialization = AgentSpecialization.SALON_CONCIERGE.name,
            avatarEmoji = "💇‍♀️",
            status = AgentOperationalStatus.ACTIVE_ONLINE.name,
            totalChatsToday = 45,
            avgResponseLatencyMs = 750L
        ),
        WhatsAppAgentEntity(
            id = "agent_sipho_02",
            name = "Sipho",
            specialization = AgentSpecialization.APPOINTMENT_SCHEDULER.name,
            avatarEmoji = "💈",
            status = AgentOperationalStatus.ACTIVE_ONLINE.name,
            totalChatsToday = 30,
            avgResponseLatencyMs = 620L
        )
    )

    @Test
    fun testHourlyMetricsComputationWithDefaults() {
        val metrics = WhatsAppAgentHourlyEngine.computeHourlyMetrics(
            events = emptyList(),
            agents = testAgents,
            selectedAgentId = null,
            timeframe = "Today"
        )

        assertNotNull(metrics)
        assertTrue(metrics.totalMessagesProcessed > 0)
        assertTrue(metrics.hourlyPoints.isNotEmpty())
        assertNotNull(metrics.peakHourLabel)
        assertTrue(metrics.peakHourVolume > 0)
        assertTrue(metrics.overallAutonomyPercentage in 75..100)

        // Peak hour must exist among points
        val peakPoint = metrics.hourlyPoints.firstOrNull { it.isPeakHour }
        assertNotNull("Must flag at least one peak hour", peakPoint)
        assertEquals(metrics.peakHourVolume, peakPoint?.totalProcessed)
    }

    @Test
    fun testRealEventHourlyAggregation() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 14)
        cal.set(Calendar.MINUTE, 15)
        val time14 = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 30)
        val time10 = cal.timeInMillis

        val events = listOf(
            WhatsAppAgentEventEntity(
                id = "evt_01",
                agentId = "agent_thandiwe_01",
                agentName = "Thandiwe",
                eventType = WhatsAppAgentEventType.MESSAGE_INBOUND.name,
                title = "Client Question",
                detail = "Need braided styles",
                timestampMillis = time14,
                formattedTime = "14:15"
            ),
            WhatsAppAgentEventEntity(
                id = "evt_02",
                agentId = "agent_thandiwe_01",
                agentName = "Thandiwe",
                eventType = WhatsAppAgentEventType.MESSAGE_OUTBOUND.name,
                title = "AI Reply",
                detail = "We have slots open at 15:00",
                timestampMillis = time14,
                formattedTime = "14:15",
                latencyMs = 640L
            ),
            WhatsAppAgentEventEntity(
                id = "evt_03",
                agentId = "agent_sipho_02",
                agentName = "Sipho",
                eventType = WhatsAppAgentEventType.MESSAGE_INBOUND.name,
                title = "Reschedule Request",
                detail = "Move to tomorrow",
                timestampMillis = time10,
                formattedTime = "10:30"
            )
        )

        val metrics = WhatsAppAgentHourlyEngine.computeHourlyMetrics(
            events = events,
            agents = testAgents,
            selectedAgentId = "agent_thandiwe_01",
            timeframe = "Today"
        )

        assertEquals("agent_thandiwe_01", metrics.selectedAgentId)
        assertEquals("Thandiwe", metrics.selectedAgentName)

        val pt14 = metrics.hourlyPoints.firstOrNull { it.hourNumber == 14 }
        assertNotNull(pt14)
        assertTrue((pt14?.inboundCustomerCount ?: 0) >= 1)
        assertTrue((pt14?.autonomousReplyCount ?: 0) >= 1)
    }

    @Test
    fun testRechartsHtmlGeneration() {
        val metrics = WhatsAppAgentHourlyEngine.computeHourlyMetrics(
            events = emptyList(),
            agents = testAgents,
            selectedAgentId = null,
            timeframe = "Today"
        )

        val html = WhatsAppHourlyRechartsGenerator.buildHourlyRechartsHtml(metrics)

        assertNotNull(html)
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("MESSAGES PROCESSED PER HOUR (RECHARTS)"))
        assertTrue(html.contains("AUTONOMOUS RESOLUTION RATE (%)"))
        assertTrue(html.contains("recharts-tooltip"))
        assertTrue(html.contains("hourly-barchart"))
        assertTrue(html.contains("autonomy-areachart"))
    }
}
