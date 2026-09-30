package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Application
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.analytics.AgentPerformanceEngine
import com.example.analytics.AgentPerformanceMetrics
import com.example.analytics.RechartsHtmlGenerator
import com.example.analytics.TriageTaskStatusBreakdown
import com.example.ui.theme.ZamaAmberPulse
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaCardSurface
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaVoid
import com.example.ui.viewmodel.ChatViewModel

/**
 * Executive Agent Performance Analytics Screen using Recharts.
 * Displays:
 * 1. Agent response latency trend over time with SLA target threshold.
 * 2. Number of resolved vs. pending triage tasks across categories.
 * 3. Peak interaction hours over time with rush hour window detection.
 */
@Composable
fun AgentPerformanceAnalyticsScreen(
    chatViewModel: ChatViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveViewModel: ChatViewModel = chatViewModel ?: viewModel(
        factory = ChatViewModel.Factory(context.applicationContext as Application)
    )

    var selectedTimeframe by remember { mutableStateOf("Today") }
    var refreshKey by remember { mutableStateOf(0) }

    val rawLogs by effectiveViewModel.messages.collectAsStateWithLifecycle()

    val metrics: AgentPerformanceMetrics = remember(rawLogs, selectedTimeframe, refreshKey) {
        AgentPerformanceEngine.computeMetrics(rawLogs, selectedTimeframe)
    }

    val rechartsHtml = remember(metrics) {
        RechartsHtmlGenerator.buildRechartsHtml(metrics)
    }

    Surface(
        color = ZamaDarkSurface,
        border = BorderStroke(1.dp, ZamaBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("agent_performance_screen")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Bar
            PerformanceDashboardHeader(
                activeTimeframe = selectedTimeframe,
                onSelectTimeframe = { selectedTimeframe = it },
                onRefresh = { refreshKey++ }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Executive KPI Highlight Tiles
            KpiMetricGrid(metrics = metrics)

            Spacer(modifier = Modifier.height(14.dp))

            // Recharts Interactive WebView Canvas
            RechartsInteractiveWebView(
                htmlContent = rechartsHtml,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(580.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Detailed Task Triage Breakdown Section
            TriageTasksDeepDiveSection(metrics = metrics)
        }
    }
}

/**
 * Top Header with Title, Status Badges, and Timeframe Selectors.
 */
@Composable
private fun PerformanceDashboardHeader(
    activeTimeframe: String,
    onSelectTimeframe: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x1F00E5FF))
                        .border(1.dp, ZamaElectricCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Agent Performance Analytics",
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "AGENT PERFORMANCE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "POWERED BY RECHARTS • SLA TELEMETRY",
                        color = ZamaElectricCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .testTag("btn_refresh_analytics")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Metrics",
                    tint = ZamaChromeLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Timeframe Selector Chips (Today, Last 7 Days, Last 30 Days)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Today", "Last 7 Days", "Last 30 Days").forEach { timeframe ->
                val isSelected = activeTimeframe == timeframe
                Surface(
                    color = if (isSelected) Color(0x3300E5FF) else Color(0xFF0F1520),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) ZamaElectricCyan else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTimeframe(timeframe) }
                        .testTag("timeframe_chip_$timeframe")
                ) {
                    Text(
                        text = timeframe,
                        color = if (isSelected) ZamaElectricCyan else ZamaChromeMid,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 4 High-Priority Executive Performance Metric Cards.
 */
@Composable
private fun KpiMetricGrid(metrics: AgentPerformanceMetrics) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Response Latency
            KpiStatCard(
                title = "Avg Response Latency",
                value = "${metrics.averageLatencyMs} ms",
                subtext = "⚡ P95: ${metrics.p95LatencyMs}ms (Target <1.5s)",
                accentColor = ZamaElectricCyan,
                icon = Icons.Default.Bolt,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Triage Tasks Resolution Rate
            KpiStatCard(
                title = "Triage Tasks Resolved",
                value = "${metrics.triageSummary.resolutionPercentage}%",
                subtext = "✅ ${metrics.triageSummary.totalResolved} Done • ${metrics.triageSummary.totalPending} Pending",
                accentColor = ZamaNeonGreen,
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Peak Rush Hour Window
            KpiStatCard(
                title = "Peak Rush Window",
                value = metrics.busiestHourWindow,
                subtext = "🔥 Highest Inquiries / Hour",
                accentColor = ZamaAmberPulse,
                icon = Icons.Default.LocalFireDepartment,
                modifier = Modifier.weight(1f)
            )

            // Card 4: SLA Compliance Rate
            KpiStatCard(
                title = "SLA Compliance Rate",
                value = "${metrics.slaCompliancePercentage}%",
                subtext = "🎯 Response within target SLA",
                accentColor = Color(0xFF64B5F6),
                icon = Icons.Default.Insights,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual KPI Stat Card.
 */
@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtext: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF090E17),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    color = ZamaChromeMid,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtext,
                color = accentColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Embedded Hardware-Accelerated WebView rendering the Recharts SVG charts.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RechartsInteractiveWebView(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    if (androidx.compose.ui.platform.LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }
    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.setSupportMultipleWindows(false)
                    settings.blockNetworkLoads = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    setBackgroundColor(android.graphics.Color.parseColor("#06090E"))
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): Boolean = true
                    }
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Detailed Task Triage Status Breakdown.
 */
@Composable
private fun TriageTasksDeepDiveSection(metrics: AgentPerformanceMetrics) {
    Surface(
        color = Color(0xFF070B12),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ZamaBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRIAGE RESOLUTION BY CATEGORY",
                    color = ZamaNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${metrics.triageSummary.totalResolved} / ${metrics.triageSummary.totalTasks} TOTAL",
                    color = ZamaChromeMid,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            metrics.triageSummary.categoryBreakdown.forEach { breakdown ->
                TriageBreakdownRow(breakdown = breakdown)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TriageBreakdownRow(breakdown: TriageTaskStatusBreakdown) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = breakdown.category,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "✅ ${breakdown.resolvedCount}",
                    color = ZamaNeonGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (breakdown.pendingCount > 0) {
                    Text(
                        text = "⏳ ${breakdown.pendingCount} pending",
                        color = Color(0xFFFF5252),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "(${breakdown.resolutionRatePercentage}%)",
                    color = ZamaChromeMid,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Progress track: green resolved fraction
        val progress = if (breakdown.totalCount > 0) {
            breakdown.resolvedCount.toFloat() / breakdown.totalCount
        } else {
            1f
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = ZamaNeonGreen,
            trackColor = Color(0xFF1E2838)
        )
    }
}
