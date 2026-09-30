package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Application
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
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
import com.example.analytics.CustomerInteractionJourney
import com.example.analytics.SentimentFlowEngine
import com.example.analytics.SentimentFlowRechartsGenerator
import com.example.analytics.SentimentFlowSummary
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.viewmodel.ChatViewModel
import java.util.Locale

/**
 * Dashboard Overlay visualizing the 'sentiment flow' of recent customer interactions using Recharts.
 * Shows how the AI's triage has shifted customer sentiment from 'negative' to 'resolved' over time.
 */
@Composable
fun SentimentFlowDashboardOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel? = null
) {
    if (!isOpen) return

    BackHandler(enabled = isOpen) {
        onDismiss()
    }

    val context = LocalContext.current
    val effectiveViewModel: ChatViewModel = chatViewModel ?: viewModel(
        factory = ChatViewModel.Factory(context.applicationContext as Application)
    )

    val rawLogs by effectiveViewModel.messages.collectAsStateWithLifecycle()
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var refreshKey by remember { mutableStateOf(0) }
    var expandedJourneyId by remember { mutableStateOf<String?>("journey_01") }

    val summary: SentimentFlowSummary = remember(rawLogs, selectedCategoryFilter, refreshKey) {
        val convertedMsgs = rawLogs.map { it.toUiModel() }
        SentimentFlowEngine.computeSentimentFlow(convertedMsgs, selectedCategoryFilter)
    }

    val rechartsHtml = remember(summary) {
        SentimentFlowRechartsGenerator.buildSentimentFlowHtml(summary)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC03060A))
            .clickable { /* Block background touch */ }
            .testTag("sentiment_flow_dashboard_overlay")
    ) {
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(280)) +
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ),
            exit = fadeOut(animationSpec = tween(200)) +
                    slideOutVertically(
                        targetOffsetY = { it / 3 },
                        animationSpec = tween(250)
                    )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(ZamaNeonGreen.copy(alpha = 0.8f), ZamaElectricCyan.copy(alpha = 0.5f), ZamaBorder)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    ),
                color = Color(0xFF070B12),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Header Bar with Close Button
                    OverlayHeader(
                        totalNegative = summary.totalNegativeIngested,
                        totalResolved = summary.totalResolved,
                        onRefresh = { refreshKey++ },
                        onDismiss = onDismiss
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Chips (All, Tension, Pricing, Delays, Reschedules)
                    CategoryFilterChipsRow(
                        selectedCategory = selectedCategoryFilter,
                        onSelectCategory = { selectedCategoryFilter = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable Dashboard Body
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Executive Shift KPI Tiles
                        item(key = "kpi_ribbon") {
                            ShiftKpiRibbon(summary = summary)
                        }

                        // 2. Interactive Recharts Visualizations (WebView Canvas)
                        item(key = "recharts_canvas") {
                            RechartsSentimentFlowWebView(
                                htmlContent = rechartsHtml,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(580.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, Color(0x3300E676), RoundedCornerShape(14.dp))
                            )
                        }

                        // 3. Section Title for Customer Journey Inspector
                        item(key = "journey_inspector_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = "Customer Interaction Threads",
                                        tint = ZamaNeonGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "RECENT CUSTOMER INTERACTION THREADS",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Text(
                                    text = "${summary.journeys.size} THREADS",
                                    color = ZamaChromeMid,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // 4. Interactive Turn-by-Turn Journey Cards
                        items(
                            items = summary.journeys,
                            key = { it.id }
                        ) { journey ->
                            CustomerJourneyCard(
                                journey = journey,
                                isExpanded = expandedJourneyId == journey.id,
                                onToggleExpand = {
                                    expandedJourneyId = if (expandedJourneyId == journey.id) null else journey.id
                                }
                            )
                        }

                        // Bottom Spacer
                        item {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top Header of the Sentiment Flow Overlay.
 */
@Composable
private fun OverlayHeader(
    totalNegative: Int,
    totalResolved: Int,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(ZamaNeonGreen, Color(0xFF082618))
                        )
                    )
                    .border(1.dp, ZamaNeonGreen.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Waves,
                    contentDescription = "Sentiment Flow",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI TRIAGE SENTIMENT FLOW",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(ZamaNeonGreen, CircleShape)
                    )
                }

                Text(
                    text = "Negative → Resolved Shift Visualizer (Recharts)",
                    color = ZamaNeonGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("sentiment_flow_refresh_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Sentiment Flow Data",
                    tint = ZamaElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("sentiment_flow_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Sentiment Flow Overlay",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Filter Chips Row.
 */
@Composable
private fun CategoryFilterChipsRow(
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    val filters = listOf(
        "ALL" to "ALL FLOWS",
        "Tension" to "SCALP TENSION",
        "Pricing" to "PRICING DISPUTES",
        "Delay" to "DELAY FRUSTRATION",
        "Deposit" to "RESCHEDULE & CANCELLATION"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        filters.forEach { (catKey, label) ->
            val isSelected = selectedCategory == catKey
            Surface(
                color = if (isSelected) Color(0x3300E676) else Color(0x14FFFFFF),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, if (isSelected) ZamaNeonGreen else Color(0x22FFFFFF)),
                modifier = Modifier
                    .clickable { onSelectCategory(catKey) }
                    .testTag("filter_chip_$catKey")
            ) {
                Text(
                    text = label,
                    color = if (isSelected) ZamaNeonGreen else ZamaChromeMid,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 4 Executive KPI Ribbon Cards.
 */
@Composable
private fun ShiftKpiRibbon(summary: SentimentFlowSummary) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiTile(
                title = "Resolution Shift Rate",
                value = "${summary.shiftSuccessRatePct}%",
                subtext = "Negative → Delighted",
                accentColor = ZamaNeonGreen,
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
            KpiTile(
                title = "Net Sentiment Shift",
                value = String.format(Locale.ROOT, "+%.2f Δ", summary.averageNetShiftDelta),
                subtext = "From -0.81 to +0.87",
                accentColor = ZamaElectricCyan,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiTile(
                title = "De-escalation Turns",
                value = String.format(Locale.ROOT, "%.1f Turns", summary.averageTurnsToResolve),
                subtext = "~${summary.averageResolutionTimeSeconds}s Average Resolution",
                accentColor = Color(0xFFFF9100),
                icon = Icons.Default.AutoAwesome,
                modifier = Modifier.weight(1f)
            )
            KpiTile(
                title = "Negative Cases Recovered",
                value = "${summary.totalResolved} / ${summary.totalNegativeIngested}",
                subtext = "Client Churn Prevented",
                accentColor = Color(0xFF00E676),
                icon = Icons.Default.Mood,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiTile(
    title: String,
    value: String,
    subtext: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0D1420),
        border = BorderStroke(1.dp, Color(0x331E2D42)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(Locale.ROOT),
                    color = ZamaChromeMid,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
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

            Spacer(modifier = Modifier.height(2.dp))

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
 * Embedded Hardware-Accelerated WebView rendering Recharts.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RechartsSentimentFlowWebView(
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
 * Customer Interaction Journey Card showing turn-by-turn progression.
 */
@Composable
private fun CustomerJourneyCard(
    journey: CustomerInteractionJourney,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Surface(
        color = Color(0xFF0D1420),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isExpanded) Color(0x5500E676) else Color(0x221E2D42)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("journey_card_${journey.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Summary Header Row
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FF1744))
                            .border(1.dp, Color(0xFFFF1744), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = journey.customerAvatarInitials,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column {
                        Text(
                            text = journey.customerName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${journey.category} • ${journey.durationSeconds}s duration",
                            color = ZamaChromeMid,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0x1F00E676),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.8.dp, Color(0x6600E676))
                    ) {
                        Text(
                            text = String.format(Locale.ROOT, "+%.2f Δ", journey.netSentimentShift),
                            color = ZamaNeonGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = ZamaChromeMid,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Initial Customer Distress Quote snippet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x14FF1744), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoodBad,
                    contentDescription = null,
                    tint = Color(0xFFFF1744),
                    modifier = Modifier.size(13.dp)
                )
                Column {
                    Text(
                        text = "INITIAL NEGATIVE INFLUX",
                        color = Color(0xFFFF5252),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "\"${journey.initialCustomerQuote}\"",
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.5.sp,
                        maxLines = if (isExpanded) 5 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Final Resolved Outcome Quote snippet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x1400E676), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mood,
                    contentDescription = null,
                    tint = ZamaNeonGreen,
                    modifier = Modifier.size(13.dp)
                )
                Column {
                    Text(
                        text = "FINAL RESOLUTION (${journey.resolutionTag})",
                        color = ZamaNeonGreen,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "\"${journey.resolvedCustomerQuote}\"",
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.5.sp,
                        maxLines = if (isExpanded) 5 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Expandable Turn-by-Turn Timeline Trace
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = "TURN-BY-TURN TRIAGE AUDIT TRACE",
                        color = ZamaElectricCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    journey.turns.forEach { turn ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = when (turn.stage.label) {
                                    "Initial Negative" -> Color(0x33FF1744)
                                    "Resolved & Satisfied" -> Color(0x3300E676)
                                    else -> Color(0x3300E5FF)
                                },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(
                                    0.5.dp,
                                    when (turn.stage.label) {
                                        "Initial Negative" -> Color(0xFFFF1744)
                                        "Resolved & Satisfied" -> ZamaNeonGreen
                                        else -> ZamaElectricCyan
                                    }
                                )
                            ) {
                                Text(
                                    text = "T${turn.turnIndex}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${turn.speaker} • ${turn.timestampFormatted}",
                                        color = ZamaChromeMid,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = String.format(Locale.ROOT, "Sentiment: %+.2f", turn.sentimentScore),
                                        color = if (turn.sentimentScore >= 0) ZamaNeonGreen else Color(0xFFFF5252),
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = turn.messageSnippet,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )

                                Text(
                                    text = "↳ ${turn.triageNote}",
                                    color = ZamaElectricCyan,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
