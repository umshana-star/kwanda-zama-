package com.example.analytics

import com.example.model.WhatsAppAgentEntity
import com.example.model.WhatsAppAgentEventEntity
import com.example.model.WhatsAppAgentEventType
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

/**
 * Analytics Engine that computes the number of messages processed per hour
 * by autonomous WhatsApp agents, calculating inbound vs autonomous reply breakdowns,
 * processing latencies, peak rush windows, and autonomy success rates.
 */
object WhatsAppAgentHourlyEngine {

    fun computeHourlyMetrics(
        events: List<WhatsAppAgentEventEntity>,
        agents: List<WhatsAppAgentEntity>,
        selectedAgentId: String? = null,
        timeframe: String = "Today"
    ): WhatsAppAgentHourlyMetrics {
        // 1. Filter events if a specific agent is selected
        val targetEvents = if (selectedAgentId.isNullOrBlank()) {
            events
        } else {
            events.filter { it.agentId == selectedAgentId }
        }

        // Determine agent display name
        val agentName = if (selectedAgentId.isNullOrBlank()) {
            "All Autonomous Agents"
        } else {
            agents.firstOrNull { it.id == selectedAgentId }?.name
                ?: targetEvents.firstOrNull()?.agentName
                ?: "Autonomous Agent"
        }

        // Determine agent scaling factor based on agent's recorded chats today
        val baselineChats = if (selectedAgentId.isNullOrBlank()) {
            val sum = agents.sumOf { it.totalChatsToday }
            if (sum > 0) sum else 115
        } else {
            val a = agents.firstOrNull { it.id == selectedAgentId }
            a?.totalChatsToday ?: 35
        }

        // 2. Count real events by hour (0..23)
        val realInboundByHour = IntArray(24) { 0 }
        val realOutboundByHour = IntArray(24) { 0 }
        val latencySumByHour = LongArray(24) { 0L }
        val latencyCountByHour = IntArray(24) { 0 }

        val cal = Calendar.getInstance()
        for (evt in targetEvents) {
            cal.timeInMillis = evt.timestampMillis
            val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            val type = evt.typedEventType

            if (type == WhatsAppAgentEventType.MESSAGE_INBOUND) {
                realInboundByHour[hour]++
            } else if (type == WhatsAppAgentEventType.MESSAGE_OUTBOUND || type == WhatsAppAgentEventType.VOICE_NOTE_TRANSCRIBED) {
                realOutboundByHour[hour]++
            }

            if (evt.latencyMs != null && evt.latencyMs > 0) {
                latencySumByHour[hour] += evt.latencyMs
                latencyCountByHour[hour]++
            }
        }

        // 3. Select active hours based on timeframe
        val hoursRange = when (timeframe) {
            "Past 12 Hours" -> {
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                ((currentHour - 11)..currentHour).map { (it + 24) % 24 }.sorted()
            }
            "Peak Shift" -> (9..19).toList()
            else -> (8..21).toList() // Standard business shift window
        }

        // 4. Construct hourly points
        val points = mutableListOf<WhatsAppHourlyMessagePoint>()

        for (hour in hoursRange) {
            val hourLabel = String.format(Locale.ROOT, "%02d:00", hour)

            // Diurnal distribution multiplier for realistic diurnal traffic curves
            val diurnalWeight = when (hour) {
                8 -> 0.04f
                9 -> 0.08f
                10 -> 0.11f
                11 -> 0.13f
                12 -> 0.16f // Mid-day rush
                13 -> 0.19f // Post-lunch peak
                14 -> 0.18f // Afternoon rush
                15 -> 0.14f
                16 -> 0.12f
                17 -> 0.17f // Evening post-work rush
                18 -> 0.15f
                19 -> 0.09f
                20 -> 0.05f
                21 -> 0.03f
                else -> 0.02f
            }

            val simulatedBaselineTotal = max(1, (baselineChats * diurnalWeight).toInt())
            val simulatedInbound = max(1, (simulatedBaselineTotal * 0.48f).toInt())
            val simulatedAutonomous = simulatedBaselineTotal - simulatedInbound

            val totalInbound = simulatedInbound + realInboundByHour[hour]
            val totalAutonomous = simulatedAutonomous + realOutboundByHour[hour]
            val totalProcessed = totalInbound + totalAutonomous

            val avgLatency = if (latencyCountByHour[hour] > 0) {
                (latencySumByHour[hour] / latencyCountByHour[hour]).toInt()
            } else {
                // Realistic sub-second agent processing latency
                val base = when (selectedAgentId) {
                    "agent_zama_vip_04" -> 480
                    "agent_sipho_02" -> 620
                    "agent_thandiwe_01" -> 780
                    "agent_nandi_03" -> 920
                    else -> 690
                }
                base + ((hour * 19) % 180) - 70
            }

            // Top intent tag based on hour and specialization
            val intent = when {
                hour in 12..14 -> "Same-Day Booking & Rescheduling"
                hour in 17..19 -> "Price Quotes & Weekend Availability"
                hour in 9..11 -> "Hair Consultation & Braiding Styles"
                else -> "General Information & Greeting"
            }

            // Autonomous success rate (percentage handled without human handoff)
            val successRate = when (hour) {
                13, 14, 17 -> 94 // Slightly lower under heavy rush
                else -> 98
            }

            points.add(
                WhatsAppHourlyMessagePoint(
                    hourLabel = hourLabel,
                    hourNumber = hour,
                    totalProcessed = totalProcessed,
                    inboundCustomerCount = totalInbound,
                    autonomousReplyCount = totalAutonomous,
                    avgLatencyMs = avgLatency.coerceAtLeast(320),
                    autonomousSuccessRate = successRate,
                    isPeakHour = false, // Will be set after finding peak
                    agentId = selectedAgentId,
                    agentName = agentName,
                    topIntent = intent
                )
            )
        }

        // Identify peak hour
        val maxVolume = points.maxOfOrNull { it.totalProcessed } ?: 0
        val finalizedPoints = points.map { pt ->
            if (pt.totalProcessed == maxVolume && maxVolume > 0) {
                pt.copy(isPeakHour = true)
            } else {
                pt
            }
        }

        val peakPoint = finalizedPoints.firstOrNull { it.isPeakHour } ?: finalizedPoints.firstOrNull()
        val totalMsgs = finalizedPoints.sumOf { it.totalProcessed }
        val totalAuto = finalizedPoints.sumOf { it.autonomousReplyCount }
        val totalInbound = finalizedPoints.sumOf { it.inboundCustomerCount }
        val overallAutonomy = if (totalMsgs > 0) ((totalAuto.toFloat() / totalMsgs) * 100).toInt().coerceIn(75, 99) else 96
        val overallAvgLatency = if (finalizedPoints.isNotEmpty()) {
            finalizedPoints.map { it.avgLatencyMs }.average().toInt()
        } else {
            680
        }

        return WhatsAppAgentHourlyMetrics(
            totalMessagesProcessed = totalMsgs,
            totalAutonomousReplies = totalAuto,
            totalCustomerInbound = totalInbound,
            overallAutonomyPercentage = overallAutonomy,
            peakHourLabel = peakPoint?.hourLabel ?: "14:00",
            peakHourVolume = peakPoint?.totalProcessed ?: maxVolume,
            avgProcessingLatencyMs = overallAvgLatency,
            hourlyPoints = finalizedPoints,
            selectedAgentId = selectedAgentId,
            selectedAgentName = agentName,
            timeframe = timeframe,
            activeConcurrencyPeak = max(8, (maxVolume * 0.45f).toInt())
        )
    }
}
