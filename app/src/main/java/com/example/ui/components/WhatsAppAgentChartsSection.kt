package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.analytics.WhatsAppAgentHourlyMetrics
import com.example.analytics.WhatsAppHourlyMessagePoint
import com.example.analytics.WhatsAppHourlyRechartsGenerator
import com.example.ui.theme.LocalZamaPalette
import com.example.ui.viewmodel.WhatsAppAgentViewModel

/**
 * Flagship Recharts Charts Section for the Autonomous WhatsApp Agent Management Dashboard.
 * Visualizes the number of messages processed per hour by autonomous agents with:
 * - Hourly Message Inbound vs. Autonomous Reply breakdown
 * - Peak rush-hour throughput detection
 * - Autonomy success rate (%) trend
 * - Processing response latency across operational hours
 */
@Composable
fun WhatsAppAgentChartsSection(
    viewModel: WhatsAppAgentViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalZamaPalette.current
    val allAgents by viewModel.allAgents.collectAsStateWithLifecycle()
    val hourlyMetrics by viewModel.hourlyMessageMetrics.collectAsStateWithLifecycle()
    val selectedAgentId by viewModel.selectedChartAgentId.collectAsStateWithLifecycle()
    val selectedTimeframe by viewModel.selectedChartTimeframe.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulatingPing.collectAsStateWithLifecycle()

    var showHourlyBreakdownList by remember { mutableStateOf(false) }

    val rechartsHtml = remember(hourlyMetrics) {
        WhatsAppHourlyRechartsGenerator.buildHourlyRechartsHtml(hourlyMetrics)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("whatsapp_agent_charts_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Surface(
            color = palette.cardSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(palette.cyanAccent.copy(alpha = 0.15f))
                                .border(1.dp, palette.cyanAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Hourly Messages Charts",
                                tint = palette.cyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "HOURLY MESSAGE VELOCITY",
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "POWERED BY RECHARTS • MESSAGES PROCESSED / HOUR",
                                color = palette.cyanAccent,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Live Telemetry Badge
                    Surface(
                        color = palette.greenAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, palette.greenAccent.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(palette.greenAccent)
                            )
                            Text(
                                text = "LIVE RECHARTS",
                                color = palette.greenAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timeframe Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIMEFRAME WINDOW",
                        color = palette.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Today", "Past 12 Hours", "Peak Shift").forEach { tf ->
                            val isSelected = selectedTimeframe == tf
                            Surface(
                                color = if (isSelected) palette.cyanAccent.copy(alpha = 0.2f) else palette.surface,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) palette.cyanAccent else palette.border
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.selectChartTimeframe(tf) }
                                    .testTag("timeframe_chip_$tf")
                            ) {
                                Text(
                                    text = tf,
                                    color = if (isSelected) palette.cyanAccent else palette.textSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Autonomous Agent Filter Chips
                Text(
                    text = "FILTER BY AUTONOMOUS AGENT",
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        val isAllSelected = selectedAgentId == null
                        Surface(
                            color = if (isAllSelected) palette.cyanAccent.copy(alpha = 0.22f) else palette.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isAllSelected) palette.cyanAccent else palette.border
                            ),
                            modifier = Modifier
                                .clickable { viewModel.selectChartAgent(null) }
                                .testTag("agent_chart_filter_all")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = if (isAllSelected) palette.cyanAccent else palette.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Cluster (${allAgents.size} Agents)",
                                    color = if (isAllSelected) palette.cyanAccent else palette.textPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    items(allAgents) { agent ->
                        val isSelected = selectedAgentId == agent.id
                        Surface(
                            color = if (isSelected) palette.cyanAccent.copy(alpha = 0.22f) else palette.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) palette.cyanAccent else palette.border
                            ),
                            modifier = Modifier
                                .clickable { viewModel.selectChartAgent(agent.id) }
                                .testTag("agent_chart_filter_${agent.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = agent.avatarEmoji, fontSize = 12.sp)
                                Text(
                                    text = agent.name,
                                    color = if (isSelected) palette.cyanAccent else palette.textPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4 KPI Summary Metric Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChartKpiCard(
                title = "TOTAL PROCESSED",
                value = "${hourlyMetrics.totalMessagesProcessed}",
                subtext = "Messages Today",
                tint = palette.cyanAccent,
                icon = Icons.Default.BarChart,
                modifier = Modifier.weight(1f)
            )
            ChartKpiCard(
                title = "PEAK VELOCITY",
                value = "${hourlyMetrics.peakHourVolume}/h",
                subtext = "Rush @ ${hourlyMetrics.peakHourLabel}",
                tint = Color(0xFFFFB300),
                icon = Icons.Default.LocalFireDepartment,
                modifier = Modifier.weight(1f)
            )
            ChartKpiCard(
                title = "AUTONOMY RATE",
                value = "${hourlyMetrics.overallAutonomyPercentage}%",
                subtext = "Zero Human Need",
                tint = palette.greenAccent,
                icon = Icons.Default.Bolt,
                modifier = Modifier.weight(1f)
            )
            ChartKpiCard(
                title = "AVG LATENCY",
                value = "${hourlyMetrics.avgProcessingLatencyMs}ms",
                subtext = "Sub-Second SLA",
                tint = Color(0xFFBA68C8),
                icon = Icons.Default.Speed,
                modifier = Modifier.weight(1f)
            )
        }

        // Interactive Recharts WebView Canvas
        Surface(
            color = palette.cardSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recharts_webview_container")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RECHARTS DATA VISUALIZATION CANVAS",
                            color = palette.cyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Scope: ${hourlyMetrics.selectedAgentName} • Tap bars for turn analytics",
                            color = palette.textSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val targetAgent = selectedAgentId ?: allAgents.firstOrNull()?.id ?: "agent_thandiwe_01"
                                viewModel.simulateCustomerWhatsAppPing(targetAgent)
                            },
                            enabled = !isSimulating,
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isSimulating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = palette.cyanAccent
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Simulate Ping",
                                    tint = palette.cyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // The WebView Component
                WhatsAppRechartsWebView(
                    htmlContent = rechartsHtml,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, palette.border.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val targetAgent = selectedAgentId ?: allAgents.firstOrNull()?.id ?: "agent_thandiwe_01"
                            viewModel.simulateCustomerWhatsAppPing(targetAgent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.cyanAccent.copy(alpha = 0.2f),
                            contentColor = palette.cyanAccent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, palette.cyanAccent),
                        enabled = !isSimulating,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_hourly_ping_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSimulating) "Simulating Turn..." else "Simulate Live Ping",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = { showHourlyBreakdownList = !showHourlyBreakdownList },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = palette.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.testTag("toggle_hourly_table_button")
                    ) {
                        Icon(
                            imageVector = if (showHourlyBreakdownList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showHourlyBreakdownList) "Hide Table" else "Hourly Table",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Detailed Hourly Table / Accordion (Optional expansion)
        AnimatedVisibility(visible = showHourlyBreakdownList) {
            Surface(
                color = palette.cardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hourly_breakdown_table_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HOURLY MESSAGE THROUGHPUT AUDIT",
                            color = palette.greenAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${hourlyMetrics.hourlyPoints.size} HOURLY WINDOWS",
                            color = palette.textSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val maxThroughput = maxOf(1, hourlyMetrics.hourlyPoints.maxOfOrNull { it.totalProcessed } ?: 1)

                    hourlyMetrics.hourlyPoints.forEach { point ->
                        HourlyBreakdownRow(
                            point = point,
                            maxVolume = maxThroughput
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Reusable KPI Tile for the Charts Section.
 */
@Composable
private fun ChartKpiCard(
    title: String,
    value: String,
    subtext: String,
    tint: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val palette = LocalZamaPalette.current
    Surface(
        color = palette.cardSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = palette.textSecondary,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = value,
                color = tint,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtext,
                color = palette.textSecondary,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Hardware-accelerated WebView hosting the Recharts HTML canvas.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WhatsAppRechartsWebView(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
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
 * Single row inside the Hourly Message Throughput Audit Table.
 */
@Composable
private fun HourlyBreakdownRow(
    point: WhatsAppHourlyMessagePoint,
    maxVolume: Int
) {
    val palette = LocalZamaPalette.current
    val progress = (point.totalProcessed.toFloat() / maxVolume).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surface.copy(alpha = 0.6f))
            .border(
                BorderStroke(
                    1.dp,
                    if (point.isPeakHour) Color(0xFFFFB300).copy(alpha = 0.6f) else palette.border
                ),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = point.hourLabel,
                    color = if (point.isPeakHour) Color(0xFFFFB300) else palette.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (point.isPeakHour) {
                    Surface(
                        color = Color(0x33FFB300),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB300))
                    ) {
                        Text(
                            text = "PEAK RUSH",
                            color = Color(0xFFFFB300),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🤖 ${point.autonomousReplyCount} auto",
                    color = palette.greenAccent,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "💬 ${point.inboundCustomerCount} in",
                    color = palette.cyanAccent,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${point.totalProcessed} total",
                    color = palette.textPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (point.isPeakHour) Color(0xFFFFB300) else palette.cyanAccent,
            trackColor = palette.border
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Latency: ${point.avgLatencyMs}ms • Autonomy: ${point.autonomousSuccessRate}%",
                color = palette.textSecondary,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = point.topIntent,
                color = palette.textSecondary,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
