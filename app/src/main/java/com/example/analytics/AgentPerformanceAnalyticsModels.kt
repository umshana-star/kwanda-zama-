package com.example.analytics

/**
 * Data point representing agent response latency for an interaction turn.
 */
data class ResponseLatencyPoint(
    val turnIndex: Int,
    val timeLabel: String,
    val latencyMs: Int,
    val targetSlaMs: Int = 1500,
    val isVoiceProcessing: Boolean = false,
    val intentTag: String = "General"
) {
    val isWithinSla: Boolean get() = latencyMs <= targetSlaMs
}

/**
 * Breakdown of resolved vs pending triage tasks by customer intent category.
 */
data class TriageTaskStatusBreakdown(
    val category: String,
    val resolvedCount: Int,
    val pendingCount: Int,
    val totalCount: Int,
    val resolutionRatePercentage: Int
)

/**
 * Aggregated summary of triage tasks status.
 */
data class TriageStatusSummary(
    val totalTasks: Int,
    val totalResolved: Int,
    val totalPending: Int,
    val overallResolutionRate: Float, // 0.0 .. 1.0
    val categoryBreakdown: List<TriageTaskStatusBreakdown>
) {
    val resolutionPercentage: Int get() = (overallResolutionRate * 100).toInt()
}

/**
 * Hourly bucket data point for peak interaction hours over time.
 */
data class PeakInteractionHourPoint(
    val hourOfDay: Int,
    val hourLabel: String,
    val messageVolume: Int,
    val customerQueries: Int,
    val agentReplies: Int,
    val isPeakHour: Boolean
)

/**
 * Complete executive metrics model for agent performance analytics.
 */
data class AgentPerformanceMetrics(
    val averageLatencyMs: Int,
    val p95LatencyMs: Int,
    val slaComplianceRate: Float, // 0.0 .. 1.0
    val triageSummary: TriageStatusSummary,
    val latencyTrend: List<ResponseLatencyPoint>,
    val peakHours: List<PeakInteractionHourPoint>,
    val busiestHourWindow: String,
    val totalInteractions: Int,
    val activeTimeframe: String = "Today"
) {
    val slaCompliancePercentage: Int get() = (slaComplianceRate * 100).toInt()
}
