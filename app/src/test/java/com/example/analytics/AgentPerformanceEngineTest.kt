package com.example.analytics

import com.example.data.local.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentPerformanceEngineTest {

    @Test
    fun computeMetrics_returnsValidLatencyAndSlaMetrics() {
        val messages = listOf(
            ChatMessage(
                content = "Hi, how much for knotless braids?",
                isFromUser = true,
                timestamp = "10:00",
                timestampMillis = 1000000L
            ),
            ChatMessage(
                content = "Hello! Medium knotless braids are R650, waist length R800.",
                isFromUser = false,
                timestamp = "10:00",
                timestampMillis = 1000650L // 650ms latency
            ),
            ChatMessage(
                content = "Can I book Saturday 2pm?",
                isFromUser = true,
                timestamp = "10:02",
                timestampMillis = 1002000L
            ),
            ChatMessage(
                content = "Saturday at 2:00 PM is open! Would you like me to hold it for you?",
                isFromUser = false,
                timestamp = "10:02",
                timestampMillis = 1002820L // 820ms latency
            )
        )

        val metrics = AgentPerformanceEngine.computeMetrics(messages, "Today")

        assertTrue("Average latency should be positive", metrics.averageLatencyMs > 0)
        assertTrue("P95 latency should be positive", metrics.p95LatencyMs > 0)
        assertTrue("SLA compliance rate should be between 0 and 1", metrics.slaComplianceRate in 0.0f..1.0f)
        assertTrue("Latency trend points should be populated", metrics.latencyTrend.isNotEmpty())
    }

    @Test
    fun computeMetrics_categorizesResolvedVsPendingTriageTasks() {
        val messages = listOf(
            ChatMessage(
                content = "Book knotless braids for Saturday",
                isFromUser = true,
                timestamp = "11:00",
                timestampMillis = 2000000L
            ),
            ChatMessage(
                content = "Confirmed! Booking locked in.",
                isFromUser = false,
                timestamp = "11:01",
                timestampMillis = 2000500L
            )
        )

        val metrics = AgentPerformanceEngine.computeMetrics(messages, "Today")
        val summary = metrics.triageSummary

        assertTrue("Total tasks should be greater than 0", summary.totalTasks > 0)
        assertTrue("Resolved tasks should be positive", summary.totalResolved > 0)
        assertTrue("Resolution rate should be valid percentage", summary.resolutionPercentage in 0..100)
        assertTrue("Categories breakdown should have entries", summary.categoryBreakdown.isNotEmpty())
    }

    @Test
    fun computeMetrics_calculatesPeakInteractionHours() {
        val messages = emptyList<ChatMessage>()
        val metrics = AgentPerformanceEngine.computeMetrics(messages, "Today")

        assertTrue("Peak hours list should contain business hours", metrics.peakHours.isNotEmpty())
        assertNotNull("Busiest hour window should not be null", metrics.busiestHourWindow)
        assertTrue("Busiest hour window should format with colon", metrics.busiestHourWindow.contains(":00"))
    }

    @Test
    fun rechartsHtmlGenerator_producesCompleteRechartsDocument() {
        val messages = listOf(
            ChatMessage(
                content = "What services do you offer?",
                isFromUser = true,
                timestamp = "12:00",
                timestampMillis = 3000000L
            ),
            ChatMessage(
                content = "We offer Knotless Braids, Silk Press, and Custom Wig Installs.",
                isFromUser = false,
                timestamp = "12:00",
                timestampMillis = 3000720L
            )
        )

        val metrics = AgentPerformanceEngine.computeMetrics(messages, "Today")
        val html = RechartsHtmlGenerator.buildRechartsHtml(metrics)

        assertTrue("HTML should contain Recharts title", html.contains("Recharts") || html.contains("RECHARTS"))
        assertTrue("HTML should contain latency chart container", html.contains("latency-chart"))
        assertTrue("HTML should contain triage chart container", html.contains("triage-chart"))
        assertTrue("HTML should contain peak hours chart container", html.contains("peak-chart"))
        assertTrue("HTML should contain SLA reference", html.contains("SLA"))
    }
}
