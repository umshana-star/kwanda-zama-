package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Application
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.analytics.ChatAnalyticsEngine
import com.example.analytics.ChatAnalyticsSummary
import com.example.analytics.D3ChartHtmlGenerator
import com.example.analytics.HourlyActivityPoint
import com.example.analytics.SentimentCategory
import com.example.analytics.SentimentTrendPoint
import com.example.data.local.ChatLogEntity
import com.example.ui.theme.ZamaAmberPulse
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaBorderGlow
import com.example.ui.theme.ZamaCardSurface
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaSilverMuted
import com.example.ui.theme.ZamaVoid
import com.example.ui.viewmodel.ChatHistoryViewModel
import java.util.Locale

enum class ChartEngineMode {
    NATIVE_RECHARTS,
    D3_JS_WEBVIEW
}

/**
 * Modern high-performance Dashboard visualizing Chat Activity Frequency
 * and Sentiment Trends queried live from the Room database.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatAnalyticsDashboardScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatViewModel: ChatHistoryViewModel = viewModel(
        factory = ChatHistoryViewModel.Factory(context.applicationContext as Application)
    )

    val chatLogs by chatViewModel.chatLogs.collectAsStateWithLifecycle()
    val totalCount by chatViewModel.totalMessageCount.collectAsStateWithLifecycle()

    // Compute live analytics from Room entities
    val analyticsSummary by remember(chatLogs) {
        derivedStateOf { ChatAnalyticsEngine.computeAnalytics(chatLogs) }
    }

    var selectedEngine by remember { mutableStateOf(ChartEngineMode.NATIVE_RECHARTS) }
    var selectedTrendPoint by remember { mutableStateOf<SentimentTrendPoint?>(null) }
    var selectedHourPoint by remember { mutableStateOf<HourlyActivityPoint?>(null) }
    var showMessageLogFeed by remember { mutableStateOf(false) }

    // Pulsing glow animation for Live Room status indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, ZamaBorderGlow, RoundedCornerShape(24.dp))
            .testTag("chat_analytics_dashboard"),
        color = ZamaDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- TOP HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    ZamaNeonGreen.copy(alpha = pulseGlowAlpha),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ROOM DATABASE SYNCS",
                            color = ZamaNeonGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Chat Activity & Sentiment Trends",
                        color = ZamaChromeLight,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Traffic Seeding / Reset Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ElevatedButton(
                        onClick = { chatViewModel.seedSimulatedTraffic() },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0x2600E5FF),
                            contentColor = ZamaElectricCyan
                        ),
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        modifier = Modifier.testTag("btn_seed_traffic")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Simulate 24h Traffic",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+Simulate 24h",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { chatViewModel.resetChatHistory() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0x1AFFFFFF), CircleShape)
                            .testTag("btn_reset_analytics")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Reset Chat Logs",
                            tint = ZamaSilverMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- VISUALIZATION ENGINE TOGGLE (D3.js vs Native Recharts) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F131C))
                    .border(1.dp, ZamaBorder, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Native Compose Charts (Recharts Style)
                val isNative = selectedEngine == ChartEngineMode.NATIVE_RECHARTS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isNative) Color(0xFF1E2838) else Color.Transparent)
                        .clickable { selectedEngine = ChartEngineMode.NATIVE_RECHARTS }
                        .padding(vertical = 8.dp)
                        .testTag("tab_engine_native"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "Native Recharts Engine",
                            tint = if (isNative) ZamaElectricCyan else ZamaSilverMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Native Charts (Recharts)",
                            color = if (isNative) ZamaChromeLight else ZamaSilverMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isNative) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // D3.js Webview Canvas
                val isD3 = selectedEngine == ChartEngineMode.D3_JS_WEBVIEW
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isD3) Color(0xFF1E2838) else Color.Transparent)
                        .clickable { selectedEngine = ChartEngineMode.D3_JS_WEBVIEW }
                        .padding(vertical = 8.dp)
                        .testTag("tab_engine_d3"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "D3.js SVG Engine",
                            tint = if (isD3) ZamaNeonGreen else ZamaSilverMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "D3.js SVG Canvas",
                            color = if (isD3) ZamaChromeLight else ZamaSilverMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isD3) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- KPI METRIC CARDS ROW ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Total Volume
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "TOTAL LOGGED",
                    value = "${analyticsSummary.totalMessages}",
                    sub = "${analyticsSummary.customerMessages} Cust / ${analyticsSummary.aiMessages} AI",
                    accentColor = ZamaElectricCyan
                )

                // Card 2: Positive Sentiment Rate
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "NET SENTIMENT",
                    value = "${analyticsSummary.positivePercentage}%",
                    sub = String.format(Locale.ROOT, "Index: %+.2f", analyticsSummary.averageSentiment),
                    accentColor = ZamaNeonGreen
                )

                // Card 3: Peak Frequency Hour
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "PEAK TRAFFIC",
                    value = analyticsSummary.peakActivityHour.split(" - ").firstOrNull() ?: "14:00",
                    sub = "High Intent",
                    accentColor = ZamaAmberPulse
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- VISUALIZATION SECTION: NATIVE VS D3 ---
            if (selectedEngine == ChartEngineMode.NATIVE_RECHARTS) {
                // NATIVE RECHARTS-STYLE COMPOSE CHARTS

                // 1. CHAT ACTIVITY FREQUENCY HISTOGRAM
                Surface(
                    color = ZamaCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ZamaBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("native_activity_frequency_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = "Chat Activity Frequency",
                                    tint = ZamaElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CHAT ACTIVITY FREQUENCY",
                                    color = ZamaElectricCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            // Series Legend
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(ZamaElectricCyan, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Customer",
                                        color = ZamaSilverMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(ZamaNeonGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "AI Agent",
                                        color = ZamaSilverMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Selected Hour Inspector Pill
                        AnimatedVisibility(visible = selectedHourPoint != null) {
                            selectedHourPoint?.let { pt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF16212E))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Slot: ${pt.hourLabel}  •  ${pt.totalCount} messages",
                                        color = ZamaChromeLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Cust: ${pt.customerCount} | AI: ${pt.aiCount} | Voice: ${pt.voiceCount}",
                                        color = ZamaElectricCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Native Frequency Canvas
                        NativeActivityFrequencyChart(
                            hourlyPoints = analyticsSummary.hourlyFrequency,
                            selectedHour = selectedHourPoint,
                            onSelectHour = { selectedHourPoint = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. SENTIMENT TRAJECTORY & TRENDLINE (RECHARTS STYLE)
                Surface(
                    color = ZamaCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ZamaBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("native_sentiment_trajectory_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = "Sentiment Trajectory",
                                    tint = ZamaNeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SENTIMENT TRENDS",
                                    color = ZamaNeonGreen,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Text(
                                text = "Avg: ${String.format(Locale.ROOT, "%+.2f", analyticsSummary.averageSentiment)}",
                                color = ZamaNeonGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Selected Trend Point Inspector Card
                        AnimatedVisibility(visible = selectedTrendPoint != null) {
                            selectedTrendPoint?.let { pt ->
                                Surface(
                                    color = Color(0xFF141F29),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.5.dp, Color(0x6600E676)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${if (pt.isCustomer) "Customer" else "AI Agent"} • ${pt.timeFormatted}",
                                                    color = ZamaSilverMuted,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "[${pt.intentTag}]",
                                                    color = ZamaElectricCyan,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "\"${pt.textSnippet}\"",
                                                color = ZamaChromeLight,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Surface(
                                            color = when (pt.category) {
                                                SentimentCategory.POSITIVE -> Color(0x2E00E676)
                                                SentimentCategory.INQUISITIVE -> Color(0x2E00E5FF)
                                                SentimentCategory.CONCERNED -> Color(0x2EFF5252)
                                                else -> Color(0x2E9CA3AF)
                                            },
                                            shape = RoundedCornerShape(100.dp)
                                        ) {
                                            Text(
                                                text = String.format(Locale.ROOT, "%+.2f", pt.sentimentScore),
                                                color = when (pt.category) {
                                                    SentimentCategory.POSITIVE -> ZamaNeonGreen
                                                    SentimentCategory.INQUISITIVE -> ZamaElectricCyan
                                                    SentimentCategory.CONCERNED -> Color(0xFFFF5252)
                                                    else -> ZamaSilverMuted
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Native Sentiment Trendline Canvas
                        NativeSentimentTrajectoryChart(
                            trendPoints = analyticsSummary.sentimentTrend,
                            selectedPoint = selectedTrendPoint,
                            onSelectPoint = { selectedTrendPoint = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. SENTIMENT DISTRIBUTION BARS
                SentimentBreakdownCard(summary = analyticsSummary)

            } else {
                // D3.JS EMBEDDED WEBVIEW CANVAS
                Surface(
                    color = ZamaCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x6600E5FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .testTag("d3_webview_chart_container")
                ) {
                    val htmlContent = remember(analyticsSummary) {
                        D3ChartHtmlGenerator.buildD3Html(analyticsSummary)
                    }

                    D3WebViewChart(htmlData = htmlContent)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- RECENT ANALYZED MESSAGES FROM ROOM DATABASE ---
            Surface(
                color = Color(0xFF0D1017),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ZamaBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMessageLogFeed = !showMessageLogFeed }
                    .testTag("toggle_analyzed_log_feed")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Room Logs",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Scored Messages from Room (${chatLogs.size})",
                            color = ZamaChromeLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = if (showMessageLogFeed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Feed",
                        tint = ZamaSilverMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showMessageLogFeed) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val recentLogs = chatLogs.takeLast(6).reversed()
                    if (recentLogs.isEmpty()) {
                        Text(
                            text = "No messages stored yet in Room.",
                            color = ZamaSilverMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        recentLogs.forEach { log ->
                            val score = ChatAnalyticsEngine.analyzeMessageSentiment(log)
                            val cat = ChatAnalyticsEngine.classifySentiment(score, log.text)
                            val intent = ChatAnalyticsEngine.extractIntentTag(log)

                            Surface(
                                color = Color(0xFF11151F),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.5.dp, Color(0x33262C38)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    if (log.isFromCustomer) Color(0x3300E5FF) else Color(0x3300E676),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (log.isFromCustomer) Icons.Default.Person else Icons.Default.SmartToy,
                                                contentDescription = null,
                                                tint = if (log.isFromCustomer) ZamaElectricCyan else ZamaNeonGreen,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = log.timestampFormatted,
                                                    color = ZamaSilverMuted,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = intent,
                                                    color = ZamaElectricCyan,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = log.text,
                                                color = ZamaChromeLight,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Sentiment Badge
                                    Surface(
                                        color = when (cat) {
                                            SentimentCategory.POSITIVE -> Color(0x2600E676)
                                            SentimentCategory.INQUISITIVE -> Color(0x2600E5FF)
                                            SentimentCategory.CONCERNED -> Color(0x26FF5252)
                                            else -> Color(0x269CA3AF)
                                        },
                                        shape = RoundedCornerShape(100.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.ROOT, "%+.2f", score),
                                            color = when (cat) {
                                                SentimentCategory.POSITIVE -> ZamaNeonGreen
                                                SentimentCategory.INQUISITIVE -> ZamaElectricCyan
                                                SentimentCategory.CONCERNED -> Color(0xFFFF5252)
                                                else -> ZamaSilverMuted
                                            },
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-COMPONENTS: KPI CARDS, SENTIMENT BREAKDOWN, NATIVE CHARTS
// -------------------------------------------------------------

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    sub: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F131C),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x26FFFFFF)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                color = ZamaSilverMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                color = ZamaChromeMid,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SentimentBreakdownCard(summary: ChatAnalyticsSummary) {
    Surface(
        color = ZamaCardSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ZamaBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "SENTIMENT DISTRIBUTION BREAKDOWN",
                color = ZamaSilverMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment progress distribution
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF161B26))
            ) {
                if (summary.positivePercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(summary.positivePercentage.toFloat().coerceAtLeast(1f))
                            .fillMaxSize()
                            .background(ZamaNeonGreen)
                    )
                }
                if (summary.inquisitivePercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(summary.inquisitivePercentage.toFloat().coerceAtLeast(1f))
                            .fillMaxSize()
                            .background(ZamaElectricCyan)
                    )
                }
                if (summary.neutralPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(summary.neutralPercentage.toFloat().coerceAtLeast(1f))
                            .fillMaxSize()
                            .background(ZamaSilverMuted)
                    )
                }
                if (summary.concernedPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(summary.concernedPercentage.toFloat().coerceAtLeast(1f))
                            .fillMaxSize()
                            .background(Color(0xFFFF5252))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend rows with counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SentimentStatItem(label = "Positive", pct = "${summary.positivePercentage}%", color = ZamaNeonGreen)
                SentimentStatItem(label = "Inquisitive", pct = "${summary.inquisitivePercentage}%", color = ZamaElectricCyan)
                SentimentStatItem(label = "Neutral", pct = "${summary.neutralPercentage}%", color = ZamaSilverMuted)
                SentimentStatItem(label = "Critical", pct = "${summary.concernedPercentage}%", color = Color(0xFFFF5252))
            }
        }
    }
}

@Composable
fun SentimentStatItem(label: String, pct: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: ",
            color = ZamaSilverMuted,
            fontSize = 10.sp
        )
        Text(
            text = pct,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// -------------------------------------------------------------
// NATIVE ACTIVITY FREQUENCY CANVAS (Jetpack Compose)
// -------------------------------------------------------------

@Composable
fun NativeActivityFrequencyChart(
    hourlyPoints: List<HourlyActivityPoint>,
    selectedHour: HourlyActivityPoint?,
    onSelectHour: (HourlyActivityPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter to active window (or standard 08:00 - 20:00 range)
    val displayPoints = remember(hourlyPoints) {
        val active = hourlyPoints.filter { it.totalCount > 0 }
        if (active.size >= 4) active else hourlyPoints.filter { it.hourOfDay in 8..20 }
    }

    val maxVal = remember(displayPoints) {
        (displayPoints.maxOfOrNull { it.totalCount } ?: 1).coerceAtLeast(4)
    }

    Canvas(
        modifier = modifier
            .pointerInput(displayPoints) {
                detectTapGestures { tapOffset ->
                    val n = displayPoints.size
                    if (n > 0) {
                        val slotW = size.width / n
                        val tappedIdx = (tapOffset.x / slotW).toInt().coerceIn(0, n - 1)
                        onSelectHour(displayPoints[tappedIdx])
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val n = displayPoints.size
        if (n == 0) return@Canvas

        val slotW = w / n
        val barW = (slotW * 0.38f).coerceIn(4f, 22f)

        // Draw horizontal gridlines
        for (step in 1..3) {
            val y = h - (h * (step / 3f))
            drawLine(
                color = Color(0xFF1E232E),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        displayPoints.forEachIndexed { i, pt ->
            val xCenter = (i + 0.5f) * slotW
            val isSelected = selectedHour?.hourOfDay == pt.hourOfDay

            // Heights
            val custH = (pt.customerCount.toFloat() / maxVal) * (h - 20f)
            val aiH = (pt.aiCount.toFloat() / maxVal) * (h - 20f)

            val custX = xCenter - barW - 2f
            val aiX = xCenter + 2f

            // Customer Bar (Electric Cyan)
            if (custH > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ZamaElectricCyan,
                            ZamaElectricCyan.copy(alpha = 0.2f)
                        ),
                        startY = h - custH,
                        endY = h
                    ),
                    topLeft = Offset(custX, h - custH),
                    size = Size(barW, custH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // AI Agent Bar (Neon Green)
            if (aiH > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ZamaNeonGreen,
                            ZamaNeonGreen.copy(alpha = 0.2f)
                        ),
                        startY = h - aiH,
                        endY = h
                    ),
                    topLeft = Offset(aiX, h - aiH),
                    size = Size(barW, aiH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // Highlight glow line if selected
            if (isSelected) {
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(xCenter, h - 6.dp.toPx())
                )
            }
        }

        // Bottom baseline
        drawLine(
            color = ZamaBorder,
            start = Offset(0f, h),
            end = Offset(w, h),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

// -------------------------------------------------------------
// NATIVE SENTIMENT TRAJECTORY CANVAS (Smooth Bezier Line & Area)
// -------------------------------------------------------------

@Composable
fun NativeSentimentTrajectoryChart(
    trendPoints: List<SentimentTrendPoint>,
    selectedPoint: SentimentTrendPoint?,
    onSelectPoint: (SentimentTrendPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    if (trendPoints.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No sentiment points logged yet. Tap '+Simulate 24h' to test.",
                color = ZamaSilverMuted,
                fontSize = 11.sp
            )
        }
        return
    }

    Canvas(
        modifier = modifier
            .pointerInput(trendPoints) {
                detectTapGestures { tapOffset ->
                    val n = trendPoints.size
                    if (n > 0) {
                        val w = size.width
                        var closestDist = Float.MAX_VALUE
                        var closestPt = trendPoints.first()

                        trendPoints.forEachIndexed { i, pt ->
                            val x = if (n == 1) w / 2f else (i.toFloat() / (n - 1)) * w
                            val dist = kotlin.math.abs(tapOffset.x - x)
                            if (dist < closestDist) {
                                closestDist = dist
                                closestPt = pt
                            }
                        }
                        onSelectPoint(closestPt)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val n = trendPoints.size
        val zeroY = h * 0.5f

        // Draw Neutral Zero Baseline
        drawLine(
            color = Color(0xFF262F3D),
            start = Offset(0f, zeroY),
            end = Offset(w, zeroY),
            strokeWidth = 1.dp.toPx()
        )

        fun getX(i: Int): Float = if (n == 1) w / 2f else (i.toFloat() / (n - 1)) * w
        fun getY(score: Float): Float {
            val normalized = (score + 1f) / 2f // 0..1
            return (h - 20f) - (normalized * (h - 40f))
        }

        // Build continuous curve path
        val path = Path()
        val areaPath = Path()

        trendPoints.forEachIndexed { i, pt ->
            val x = getX(i)
            val y = getY(pt.sentimentScore)
            if (i == 0) {
                path.moveTo(x, y)
                areaPath.moveTo(x, zeroY)
                areaPath.lineTo(x, y)
            } else {
                val prevX = getX(i - 1)
                val prevY = getY(trendPoints[i - 1].sentimentScore)
                val cx1 = (prevX + x) / 2f
                path.cubicTo(cx1, prevY, cx1, y, x, y)
                areaPath.cubicTo(cx1, prevY, cx1, y, x, y)
            }
        }

        areaPath.lineTo(getX(n - 1), zeroY)
        areaPath.close()

        // Area Gradient under curve
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    ZamaNeonGreen.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h
            )
        )

        // Trajectory Line
        drawPath(
            path = path,
            color = ZamaNeonGreen,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw clickable data points
        trendPoints.forEachIndexed { i, pt ->
            val x = getX(i)
            val y = getY(pt.sentimentScore)
            val isSelected = selectedPoint?.messageId == pt.messageId

            val nodeColor = when (pt.category) {
                SentimentCategory.POSITIVE -> ZamaNeonGreen
                SentimentCategory.INQUISITIVE -> ZamaElectricCyan
                SentimentCategory.CONCERNED -> Color(0xFFFF5252)
                else -> ZamaSilverMuted
            }

            // Outer glow if selected
            if (isSelected) {
                drawCircle(
                    color = nodeColor.copy(alpha = 0.35f),
                    radius = 10.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            drawCircle(
                color = nodeColor,
                radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = ZamaDarkSurface,
                radius = 1.5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

// -------------------------------------------------------------
// D3.JS EMBEDDED WEBVIEW (Interactive SVG Canvas)
// -------------------------------------------------------------

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun D3WebViewChart(
    htmlData: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.setSupportMultipleWindows(false)
                settings.blockNetworkLoads = true
                setBackgroundColor(0xFF050505.toInt())
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean = true
                }
                loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
        }
    )
}
