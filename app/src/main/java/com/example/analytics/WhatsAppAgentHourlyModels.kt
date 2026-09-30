package com.example.analytics

/**
 * Data point representing message volume and autonomous processing telemetry for a single hour.
 */
data class WhatsAppHourlyMessagePoint(
    val hourLabel: String,                 // e.g. "08:00", "14:00"
    val hourNumber: Int,                   // 0..23
    val totalProcessed: Int,               // Total messages processed in this hour
    val inboundCustomerCount: Int,         // Customer inquiries ingested
    val autonomousReplyCount: Int,         // Autonomous AI agent responses generated
    val avgLatencyMs: Int,                 // Average reply latency in milliseconds
    val autonomousSuccessRate: Int,        // Percentage handled autonomously (e.g. 96%)
    val isPeakHour: Boolean = false,       // Flag if this hour is the peak traffic rush
    val agentId: String? = null,           // Target agent ID if filtered, or null for cluster
    val agentName: String = "All Agents",  // Associated agent name
    val topIntent: String = "General Support" // Dominant customer intent for this hour
)

/**
 * Aggregated hourly message telemetry metrics for the WhatsApp Agent Dashboard.
 */
data class WhatsAppAgentHourlyMetrics(
    val totalMessagesProcessed: Int,
    val totalAutonomousReplies: Int,
    val totalCustomerInbound: Int,
    val overallAutonomyPercentage: Int,
    val peakHourLabel: String,
    val peakHourVolume: Int,
    val avgProcessingLatencyMs: Int,
    val hourlyPoints: List<WhatsAppHourlyMessagePoint>,
    val selectedAgentId: String? = null,
    val selectedAgentName: String = "All Autonomous Agents",
    val timeframe: String = "Today",
    val activeConcurrencyPeak: Int = 12
)
