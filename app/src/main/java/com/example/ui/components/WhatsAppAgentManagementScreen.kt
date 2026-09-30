package com.example.ui.components

import android.app.Application
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AgentOperationalStatus
import com.example.model.AgentSpecialization
import com.example.model.HandoffTriggerMode
import com.example.model.WhatsAppAgentEntity
import com.example.model.WhatsAppGlobalSettings
import com.example.ui.theme.LocalZamaPalette
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.viewmodel.WhatsAppAgentViewModel
import kotlin.math.roundToInt

/**
 * Flagship UI Dashboard for managing Autonomous WhatsApp Agent Settings and Active Status Indicators.
 * Supports theme toggling (Futuristic Dark and High-Contrast Daylight) and real-time Room persistence.
 */
@Composable
fun WhatsAppAgentManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: WhatsAppAgentViewModel? = null
) {
    val context = LocalContext.current
    val effectiveViewModel: WhatsAppAgentViewModel = viewModel ?: viewModel(
        factory = WhatsAppAgentViewModel.Factory(context.applicationContext as Application)
    )

    val palette = LocalZamaPalette.current
    val agents by effectiveViewModel.filteredAgents.collectAsStateWithLifecycle()
    val allAgents by effectiveViewModel.allAgents.collectAsStateWithLifecycle()
    val globalSettings by effectiveViewModel.globalSettings.collectAsStateWithLifecycle()
    val selectedFilter by effectiveViewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val activeCount by effectiveViewModel.activeOnlineCount.collectAsStateWithLifecycle()
    val totalLiveChats by effectiveViewModel.totalLiveChats.collectAsStateWithLifecycle()
    val avgLatency by effectiveViewModel.avgSystemLatencyMs.collectAsStateWithLifecycle()
    val autoRate by effectiveViewModel.overallAutonomousRate.collectAsStateWithLifecycle()
    val editingAgent by effectiveViewModel.editingAgent.collectAsStateWithLifecycle()
    val isSimulatingPing by effectiveViewModel.isSimulatingPing.collectAsStateWithLifecycle()
    val simFeedback by effectiveViewModel.simulationFeedback.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showGlobalSettingsModal by remember { mutableStateOf(false) }
    var currentDashboardTab by remember { mutableIntStateOf(0) }
    val totalEventsCount by effectiveViewModel.totalEventCount.collectAsStateWithLifecycle()

    BackHandler(enabled = currentDashboardTab != 0) {
        currentDashboardTab = 0
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(BorderStroke(1.dp, palette.border), RoundedCornerShape(24.dp))
            .testTag("whatsapp_agent_management_screen"),
        color = palette.surface,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = palette.greenAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = palette.greenAccent,
                            modifier = Modifier
                                .padding(9.dp)
                                .size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "AUTONOMOUS WHATSAPP AGENTS",
                                color = palette.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            LiveHeartbeatBeacon()
                        }
                        Text(
                            text = "Meta Cloud API v19.0 • Homomorphic Triage Enclave",
                            color = palette.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showGlobalSettingsModal = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_agent_global_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Global WhatsApp Settings",
                            tint = palette.cyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        color = palette.cyanAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable { showCreateDialog = true }
                            .testTag("btn_add_whatsapp_agent")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Agent",
                                tint = palette.cyanAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "NEW AGENT",
                                color = palette.cyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Master Kill-Switch / Service Health Banner
            MasterKillSwitchBanner(
                isActive = globalSettings.masterAutonomousActive,
                onToggle = { effectiveViewModel.toggleMasterKillSwitch(it) }
            )

            // Dashboard Segmented Tab Bar (Agents vs History Log)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.cardSurface)
                    .border(BorderStroke(1.dp, palette.border), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = if (currentDashboardTab == 0) palette.cyanAccent.copy(alpha = 0.22f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (currentDashboardTab == 0) BorderStroke(1.dp, palette.cyanAccent) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentDashboardTab = 0 }
                        .testTag("tab_agents_management")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = if (currentDashboardTab == 0) palette.cyanAccent else palette.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AGENTS (${allAgents.size})",
                            color = if (currentDashboardTab == 0) palette.cyanAccent else palette.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    color = if (currentDashboardTab == 1) palette.cyanAccent.copy(alpha = 0.22f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (currentDashboardTab == 1) BorderStroke(1.dp, palette.cyanAccent) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentDashboardTab = 1 }
                        .testTag("tab_history_logs")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (currentDashboardTab == 1) palette.cyanAccent else palette.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LOGS ($totalEventsCount)",
                            color = if (currentDashboardTab == 1) palette.cyanAccent else palette.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    color = if (currentDashboardTab == 2) palette.cyanAccent.copy(alpha = 0.22f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (currentDashboardTab == 2) BorderStroke(1.dp, palette.cyanAccent) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentDashboardTab = 2 }
                        .testTag("tab_recharts_analytics")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = if (currentDashboardTab == 2) palette.cyanAccent else palette.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CHARTS",
                            color = if (currentDashboardTab == 2) palette.cyanAccent else palette.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (currentDashboardTab == 0) {
                // 4 Mini Live Stat Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AgentMetricCard(
                        title = "ONLINE AGENTS",
                        value = "$activeCount/${allAgents.size}",
                        subtext = "Active & Serving",
                        tint = palette.greenAccent,
                        icon = Icons.Default.SupportAgent,
                        modifier = Modifier.weight(1f)
                    )
                    AgentMetricCard(
                        title = "LIVE CHATS",
                        value = "$totalLiveChats",
                        subtext = "In Concurrency",
                        tint = palette.cyanAccent,
                        icon = Icons.Default.Sync,
                        modifier = Modifier.weight(1f)
                    )
                    AgentMetricCard(
                        title = "AUTO RATE",
                        value = "$autoRate%",
                        subtext = "Zero Human Need",
                        tint = Color(0xFFBA68C8),
                        icon = Icons.Default.AutoAwesome,
                        modifier = Modifier.weight(1f)
                    )
                    AgentMetricCard(
                        title = "AVG LATENCY",
                        value = "${avgLatency}ms",
                        subtext = "Lightning Fast",
                        tint = Color(0xFFFFB74D),
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }

                // AI Response Delay Time (ms) Configuration Slider Card
                AiResponseDelayConfigCard(
                    currentDelayMs = globalSettings.aiResponseDelayMs,
                    onDelayChanged = { newDelayMs ->
                        effectiveViewModel.updateAiResponseDelayMs(newDelayMs)
                    }
                )

                // Real-time Simulation Feedback Banner (if active)
                AnimatedVisibility(visible = simFeedback != null) {
                    Surface(
                        color = palette.cyanAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isSimulatingPing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = palette.cyanAccent
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = palette.greenAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = simFeedback.orEmpty(),
                                    color = palette.textPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            IconButton(
                                onClick = { effectiveViewModel.clearSimulationFeedback() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Active Agent Status Filter Chips
                AgentStatusFilterBar(
                    allAgents = allAgents,
                    selectedFilter = selectedFilter,
                    onSelectFilter = { effectiveViewModel.setFilter(it) }
                )

                // Agent Cards List
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (agents.isEmpty()) {
                        Surface(
                            color = palette.cardSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = palette.textMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No agents found matching this status filter",
                                    color = palette.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                OutlinedButton(
                                    onClick = { effectiveViewModel.setFilter(null) },
                                    modifier = Modifier.testTag("btn_clear_filter")
                                ) {
                                    Text("Show All Agents", color = palette.cyanAccent, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        agents.forEach { agent ->
                            WhatsAppAgentCard(
                                agent = agent,
                                onToggleAutonomy = { effectiveViewModel.toggleAgentAutonomy(agent.id, agent.isAutonomousEnabled) },
                                onEdit = { effectiveViewModel.setEditingAgent(agent) },
                                onSimulatePing = { effectiveViewModel.simulateCustomerWhatsAppPing(agent.id) },
                                isSimulating = isSimulatingPing,
                                onViewHistory = {
                                    effectiveViewModel.setSelectedAgentLogFilter(agent.id)
                                    currentDashboardTab = 1
                                },
                                onViewCharts = {
                                    effectiveViewModel.selectChartAgent(agent.id)
                                    currentDashboardTab = 2
                                }
                            )
                        }
                    }
                }

                // Activity Preview Banner at bottom of Tab 0
                RecentActivityPreviewBanner(
                    viewModel = effectiveViewModel,
                    onOpenFullHistory = { currentDashboardTab = 1 }
                )

                // Hourly Velocity Recharts Preview Banner at bottom of Tab 0
                HourlyVelocityPreviewBanner(
                    viewModel = effectiveViewModel,
                    onOpenFullCharts = { currentDashboardTab = 2 }
                )
            } else if (currentDashboardTab == 1) {
                // Tab 1: Full History Log View
                WhatsAppAgentHistoryLogView(
                    viewModel = effectiveViewModel,
                    onNavigateToAgentEdit = { agent -> effectiveViewModel.setEditingAgent(agent) }
                )
            } else if (currentDashboardTab == 2) {
                // Tab 2: Recharts Hourly Messages Section
                WhatsAppAgentChartsSection(
                    viewModel = effectiveViewModel
                )
            }
        }
    }

    // Modal Dialog for Editing Agent Settings
    editingAgent?.let { agent ->
        WhatsAppAgentConfigDialog(
            agent = agent,
            onSave = { updated -> effectiveViewModel.saveAgent(updated) },
            onDelete = { toDelete -> effectiveViewModel.deleteAgent(toDelete) },
            onDismiss = { effectiveViewModel.setEditingAgent(null) }
        )
    }

    // Modal Dialog for Adding a New Agent
    if (showCreateDialog) {
        CreateAgentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, spec, line, maxChats ->
                effectiveViewModel.createNewAgent(name, spec, line, maxChats)
                showCreateDialog = false
            }
        )
    }

    // Modal Dialog for Global Settings
    if (showGlobalSettingsModal) {
        WhatsAppGlobalSettingsDialog(
            settings = globalSettings,
            onSave = { updated ->
                effectiveViewModel.updateGlobalSettings(updated)
                showGlobalSettingsModal = false
            },
            onDismiss = { showGlobalSettingsModal = false }
        )
    }
}

/**
 * Animated Beacon representing the live heartbeat of the autonomous agent cluster.
 */
@Composable
fun LiveHeartbeatBeacon() {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartbeat_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartbeat_alpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(14.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .scale(scale)
                .background(ZamaNeonGreen.copy(alpha = alpha * 0.4f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(ZamaNeonGreen, CircleShape)
        )
    }
}

/**
 * Master Kill-Switch Banner allowing instantaneous 1-tap activation/suspension of all WhatsApp autonomous agents.
 */
@Composable
fun MasterKillSwitchBanner(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val palette = LocalZamaPalette.current
    val bgColor = if (isActive) {
        palette.greenAccent.copy(alpha = 0.12f)
    } else {
        Color(0xFFFFB74D).copy(alpha = 0.12f)
    }
    val borderColor = if (isActive) palette.greenAccent.copy(alpha = 0.35f) else Color(0xFFFFB74D).copy(alpha = 0.35f)
    val accentColor = if (isActive) palette.greenAccent else Color(0xFFFFB74D)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isActive) "AUTONOMOUS SYSTEM ONLINE" else "ALL AUTONOMOUS AGENTS PAUSED",
                        color = accentColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isActive) {
                            "All WhatsApp agent nodes actively monitoring client channels 24/7"
                        } else {
                            "Incoming customer messages hold for manual business owner reply"
                        },
                        color = palette.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.greenAccent,
                    checkedTrackColor = palette.greenAccent.copy(alpha = 0.35f),
                    uncheckedThumbColor = Color(0xFFFFB74D),
                    uncheckedTrackColor = Color(0xFFFFB74D).copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag("switch_master_autonomous_killswitch")
            )
        }
    }
}

/**
 * Metric summary tile.
 */
@Composable
fun AgentMetricCard(
    title: String,
    value: String,
    subtext: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = palette.textMuted,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
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
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = subtext,
                color = tint,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Filter bar displaying status categories with real-time count badges and color-coded status dots.
 */
@Composable
fun AgentStatusFilterBar(
    allAgents: List<WhatsAppAgentEntity>,
    selectedFilter: AgentOperationalStatus?,
    onSelectFilter: (AgentOperationalStatus?) -> Unit
) {
    val palette = LocalZamaPalette.current

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "ALL" chip
        item {
            val isSelected = selectedFilter == null
            Surface(
                color = if (isSelected) palette.cyanAccent.copy(alpha = 0.2f) else palette.cardSurface,
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, if (isSelected) palette.cyanAccent else palette.border),
                modifier = Modifier
                    .clickable { onSelectFilter(null) }
                    .testTag("filter_agent_all")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ALL (${allAgents.size})",
                        color = if (isSelected) palette.cyanAccent else palette.textPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Specific Status Chips
        items(AgentOperationalStatus.values()) { status ->
            val isSelected = selectedFilter == status
            val count = allAgents.count { it.operationalStatus == status }
            val statusColor = Color(status.colorHex)

            Surface(
                color = if (isSelected) statusColor.copy(alpha = 0.2f) else palette.cardSurface,
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, if (isSelected) statusColor else palette.border),
                modifier = Modifier
                    .clickable { onSelectFilter(status) }
                    .testTag("filter_agent_${status.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = "${status.label} ($count)",
                        color = if (isSelected) statusColor else palette.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * High-fidelity WhatsApp Agent Card with Active Status Indicators, Capacity Bars, and Quick Controls.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppAgentCard(
    agent: WhatsAppAgentEntity,
    onToggleAutonomy: () -> Unit,
    onEdit: () -> Unit,
    onSimulatePing: () -> Unit,
    isSimulating: Boolean,
    onViewHistory: (() -> Unit)? = null,
    onViewCharts: (() -> Unit)? = null
) {
    val palette = LocalZamaPalette.current
    val statusColor = Color(agent.operationalStatus.colorHex)

    Surface(
        color = palette.cardSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (agent.isAutonomousEnabled) statusColor.copy(alpha = 0.35f) else palette.border),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_card_${agent.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Avatar, Name, Live Status Indicator, Autonomy Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Emoji with glowing ring
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.5f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = agent.avatarEmoji, fontSize = 22.sp)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = agent.name,
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Active Status Indicator Pill with pulsating dot
                            AgentStatusIndicatorPill(
                                status = agent.operationalStatus,
                                isEnabled = agent.isAutonomousEnabled
                            )
                        }

                        Text(
                            text = agent.agentSpecialization.displayName,
                            color = palette.cyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Autonomy Toggle Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (agent.isAutonomousEnabled) "AUTO" else "MANUAL",
                        color = if (agent.isAutonomousEnabled) palette.greenAccent else palette.textMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Switch(
                        checked = agent.isAutonomousEnabled,
                        onCheckedChange = { onToggleAutonomy() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = palette.greenAccent,
                            checkedTrackColor = palette.greenAccent.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("switch_autonomy_${agent.id}")
                    )
                }
            }

            // WhatsApp Channel & Webhook Meta Badge
            Surface(
                color = palette.surface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.8.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(palette.greenAccent, CircleShape)
                        )
                        Text(
                            text = agent.phoneLine,
                            color = palette.textPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "${agent.uptimePercentage}% Uptime",
                        color = palette.greenAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Live Concurrency Capacity Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CONCURRENT CHATS CAPACITY",
                        color = palette.textMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${agent.currentActiveChats} / ${agent.maxConcurrentChats} Active (${agent.totalChatsToday} today)",
                        color = palette.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                val progress = (agent.currentActiveChats.toFloat() / agent.maxConcurrentChats.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(100.dp)),
                    color = statusColor,
                    trackColor = palette.border
                )
            }

            // Agent Parameters & Metrics Pills
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricChip(
                    label = "⚡ ${agent.avgResponseLatencyMs}ms Latency",
                    color = palette.cyanAccent
                )
                MetricChip(
                    label = "🎯 ${(agent.confidenceThreshold * 100).toInt()}% Confidence",
                    color = Color(0xFFBA68C8)
                )
                MetricChip(
                    label = "⭐ ${(agent.sentimentScore * 100).toInt()}% CSAT",
                    color = Color(0xFFFFB74D)
                )
                MetricChip(
                    label = "🗣️ ${agent.supportedLanguages}",
                    color = palette.textSecondary
                )
                if (agent.allowVoiceNoteReplies) {
                    MetricChip(label = "🎙️ Voice AI", color = palette.greenAccent)
                }
                if (agent.allowAutoCalendarSync) {
                    MetricChip(label = "📅 Auto-Booking", color = palette.cyanAccent)
                }
            }

            // System Directive Preview
            Surface(
                color = palette.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "“${agent.systemPromptDirective}”",
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            HorizontalDivider(color = palette.border.copy(alpha = 0.6f), thickness = 0.8.dp)

            // Bottom Actions: Simulate Ping & Tune Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Test WhatsApp Ping Simulation Button
                OutlinedButton(
                    onClick = onSimulatePing,
                    enabled = !isSimulating && agent.isAutonomousEnabled,
                    border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = palette.cyanAccent
                    ),
                    modifier = Modifier.testTag("btn_test_ping_${agent.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isSimulating) "PROCESSING..." else "TEST WHATSAPP PING",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onViewHistory != null) {
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .clickable { onViewHistory() }
                                .testTag("btn_logs_agent_${agent.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "LOGS",
                                    color = palette.textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    if (onViewCharts != null) {
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clickable { onViewCharts() }
                                .testTag("btn_charts_agent_${agent.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "Charts",
                                    tint = palette.cyanAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "CHARTS",
                                    color = palette.cyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Tune Settings Button
                    Surface(
                        color = palette.cyanAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable { onEdit() }
                            .testTag("btn_tune_agent_${agent.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Tune",
                                tint = palette.cyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "TUNE",
                                color = palette.cyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Status Indicator Pill with pulsating dot for active states.
 */
@Composable
fun AgentStatusIndicatorPill(
    status: AgentOperationalStatus,
    isEnabled: Boolean
) {
    val displayStatus = if (!isEnabled) AgentOperationalStatus.OFFLINE_PAUSED else status
    val statusColor = Color(displayStatus.colorHex)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val shouldPulse = displayStatus == AgentOperationalStatus.ACTIVE_ONLINE || displayStatus == AgentOperationalStatus.BUSY_HANDLING
    val pulseAlpha by if (shouldPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Surface(
        color = statusColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(statusColor.copy(alpha = pulseAlpha), CircleShape)
            )
            Text(
                text = displayStatus.label.uppercase(),
                color = statusColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun MetricChip(label: String, color: Color) {
    val palette = LocalZamaPalette.current
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.6.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

/**
 * Modal Dialog for deep tuning of an autonomous agent's parameters.
 */
@Composable
fun WhatsAppAgentConfigDialog(
    agent: WhatsAppAgentEntity,
    onSave: (WhatsAppAgentEntity) -> Unit,
    onDelete: (WhatsAppAgentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalZamaPalette.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf(agent.name) }
    var phoneLine by remember { mutableStateOf(agent.phoneLine) }
    var selectedSpec by remember { mutableStateOf(agent.agentSpecialization) }
    var isEnabled by remember { mutableStateOf(agent.isAutonomousEnabled) }
    var responseDelayMs by remember { mutableFloatStateOf(agent.avgResponseLatencyMs.coerceIn(100L, 5000L).toFloat()) }
    var typingDelay by remember { mutableFloatStateOf(agent.simulatedTypingDelaySec) }
    var confidence by remember { mutableFloatStateOf(agent.confidenceThreshold) }
    var maxChats by remember { mutableIntStateOf(agent.maxConcurrentChats) }
    var allowVoice by remember { mutableStateOf(agent.allowVoiceNoteReplies) }
    var allowBooking by remember { mutableStateOf(agent.allowAutoCalendarSync) }
    var handoffTrigger by remember { mutableStateOf(agent.triggerMode) }
    var systemDirective by remember { mutableStateOf(agent.systemPromptDirective) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("dialog_agent_config"),
            shape = RoundedCornerShape(22.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = palette.cyanAccent.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = selectedSpec.avatarEmoji, fontSize = 20.sp)
                            }
                        }

                        Column {
                            Text(
                                text = "AGENT CONFIGURATION",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Tuning ${agent.name} • WhatsApp Node",
                                color = palette.textMuted,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_close_agent_config")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = palette.border, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Config Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Agent Name & Phone
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "AGENT IDENTIFIER & WHATSAPP LINE",
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Display Name", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_agent_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.cyanAccent,
                                unfocusedBorderColor = palette.border,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            )
                        )

                        OutlinedTextField(
                            value = phoneLine,
                            onValueChange = { phoneLine = it },
                            label = { Text("WhatsApp Phone Channel", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_agent_phone"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.cyanAccent,
                                unfocusedBorderColor = palette.border,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            )
                        )
                    }

                    // Specialization Preset Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ROLE SPECIALIZATION",
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        AgentSpecialization.values().forEach { spec ->
                            val isSelected = selectedSpec == spec
                            Surface(
                                color = if (isSelected) palette.cyanAccent.copy(alpha = 0.18f) else palette.cardSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSelected) palette.cyanAccent else palette.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSpec = spec }
                                    .testTag("spec_${spec.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = spec.avatarEmoji, fontSize = 18.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = spec.displayName,
                                            color = if (isSelected) palette.cyanAccent else palette.textPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = spec.description,
                                            color = palette.textMuted,
                                            fontSize = 9.5.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = palette.cyanAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // AI Response Delay Time in Milliseconds Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AI RESPONSE DELAY TIME (MS)",
                                color = palette.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${responseDelayMs.roundToInt()} ms",
                                color = palette.cyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Controls exact millisecond AI response latency before dispatching autonomous WhatsApp replies (100ms - 5,000ms).",
                            color = palette.textMuted,
                            fontSize = 9.5.sp
                        )
                        Slider(
                            value = responseDelayMs,
                            onValueChange = {
                                responseDelayMs = it
                                typingDelay = (it / 1000f).coerceIn(0.5f, 5.0f)
                            },
                            valueRange = 100f..5000f,
                            steps = 48,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.cyanAccent,
                                activeTrackColor = palette.cyanAccent
                            ),
                            modifier = Modifier.testTag("slider_ai_response_delay_ms")
                        )
                    }

                    // Autonomous Response Latency Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "NATURAL TYPING DELAY",
                                color = palette.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${String.format("%.1f", typingDelay)}s delay",
                                color = palette.cyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Emulates natural human typing cadence on WhatsApp before dispatching message.",
                            color = palette.textMuted,
                            fontSize = 9.5.sp
                        )
                        Slider(
                            value = typingDelay,
                            onValueChange = { typingDelay = it },
                            valueRange = 0.5f..5.0f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.cyanAccent,
                                activeTrackColor = palette.cyanAccent
                            ),
                            modifier = Modifier.testTag("slider_typing_delay")
                        )
                    }

                    // Confidence Threshold Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AI CONFIDENCE THRESHOLD",
                                color = palette.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(confidence * 100).roundToInt()}% min score",
                                color = Color(0xFFBA68C8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Replies below this certainty level automatically transfer to the salon owner.",
                            color = palette.textMuted,
                            fontSize = 9.5.sp
                        )
                        Slider(
                            value = confidence,
                            onValueChange = { confidence = it },
                            valueRange = 0.50f..0.99f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFBA68C8),
                                activeTrackColor = Color(0xFFBA68C8)
                            ),
                            modifier = Modifier.testTag("slider_confidence")
                        )
                    }

                    // Max Concurrent Chats
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "MAX CONCURRENT CHATS",
                                color = palette.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$maxChats simultaneous",
                                color = palette.greenAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = maxChats.toFloat(),
                            onValueChange = { maxChats = it.roundToInt() },
                            valueRange = 5f..50f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.greenAccent,
                                activeTrackColor = palette.greenAccent
                            ),
                            modifier = Modifier.testTag("slider_max_chats")
                        )
                    }

                    // Feature Toggles: Voice Notes & Auto-Booking
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "AUTONOMOUS CAPABILITIES",
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Voice Notes
                        Surface(
                            color = palette.cardSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = palette.greenAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text("Voice Note AI Replies", color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Transcribes voice clips & synthesizes voice answers", color = palette.textMuted, fontSize = 9.sp)
                                    }
                                }
                                Switch(
                                    checked = allowVoice,
                                    onCheckedChange = { allowVoice = it },
                                    modifier = Modifier.testTag("switch_voice_notes")
                                )
                            }
                        }

                        // Auto-Booking Calendar Sync
                        Surface(
                            color = palette.cardSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = palette.cyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text("Auto-Commit Calendar Slots", color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Directly holds time on salon calendar without owner approval", color = palette.textMuted, fontSize = 9.sp)
                                    }
                                }
                                Switch(
                                    checked = allowBooking,
                                    onCheckedChange = { allowBooking = it },
                                    modifier = Modifier.testTag("switch_auto_booking")
                                )
                            }
                        }
                    }

                    // System Prompt Directive Text Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "NEURAL PROMPT DIRECTIVE",
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedTextField(
                            value = systemDirective,
                            onValueChange = { systemDirective = it },
                            label = { Text("Specialty Instructions & Boundary Rules", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .testTag("input_agent_directive"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.cyanAccent,
                                unfocusedBorderColor = palette.border,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = palette.border, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons: Save & Delete
                var showDeleteConfirm by remember { mutableStateOf(false) }
                var validationError by remember { mutableStateOf<String?>(null) }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = Color(0xFFFF5252),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        containerColor = palette.surface,
                        title = {
                            Text(
                                text = "REMOVE AGENT NODE?",
                                color = Color(0xFFFF5252),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        text = {
                            Text(
                                text = "Are you sure you want to decommission ${agent.name}? Active WhatsApp routing on ${agent.phoneLine} will be reassigned.",
                                color = palette.textSecondary,
                                fontSize = 11.5.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteConfirm = false
                                    onDelete(agent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                modifier = Modifier.testTag("btn_confirm_delete_agent")
                            ) {
                                Text("DELETE AGENT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("CANCEL", color = palette.textSecondary, fontSize = 11.sp)
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("btn_delete_agent")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Agent",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Cancel", fontSize = 11.sp, color = palette.textSecondary)
                        }

                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    validationError = "Agent name cannot be empty."
                                    return@Button
                                }
                                if (phoneLine.isBlank()) {
                                    validationError = "Assigned WhatsApp phone line is required."
                                    return@Button
                                }
                                validationError = null
                                val updated = agent.copy(
                                    name = name.trim(),
                                    phoneLine = phoneLine.trim(),
                                    specialization = selectedSpec.name,
                                    avatarEmoji = selectedSpec.avatarEmoji,
                                    isAutonomousEnabled = isEnabled,
                                    avgResponseLatencyMs = responseDelayMs.roundToInt().toLong(),
                                    simulatedTypingDelaySec = typingDelay,
                                    confidenceThreshold = confidence,
                                    maxConcurrentChats = maxChats,
                                    allowVoiceNoteReplies = allowVoice,
                                    allowAutoCalendarSync = allowBooking,
                                    handoffTrigger = handoffTrigger.name,
                                    systemPromptDirective = systemDirective.trim().ifBlank { agent.systemPromptDirective },
                                    lastActiveEpochMillis = System.currentTimeMillis()
                                )
                                onSave(updated)
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.cyanAccent
                            ),
                            modifier = Modifier.testTag("btn_save_agent_config")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SAVE SETTINGS",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal Dialog for quick deployment of a new specialized WhatsApp agent.
 */
@Composable
fun CreateAgentDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, spec: AgentSpecialization, phoneLine: String, maxChats: Int) -> Unit
) {
    val palette = LocalZamaPalette.current
    var name by remember { mutableStateOf("") }
    var selectedSpec by remember { mutableStateOf(AgentSpecialization.SALON_CONCIERGE) }
    var phoneLine by remember { mutableStateOf("+27 82 555 0199 [Line Extra]") }
    var maxChats by remember { mutableIntStateOf(20) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("dialog_create_agent")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DEPLOY NEW AGENT",
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = palette.textMuted)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Agent Name (e.g. Busisiwe)") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { err ->
                        { Text(err, color = Color(0xFFFF5252), fontSize = 10.sp) }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_create_agent_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.cyanAccent,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
                    )
                )

                OutlinedTextField(
                    value = phoneLine,
                    onValueChange = {
                        phoneLine = it
                        errorMessage = null
                    },
                    label = { Text("WhatsApp Business Line") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_create_agent_phone"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.cyanAccent,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
                    )
                )

                Text(
                    text = "SELECT SPECIALTY ROLE",
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                AgentSpecialization.values().forEach { spec ->
                    val isSelected = selectedSpec == spec
                    Surface(
                        color = if (isSelected) palette.cyanAccent.copy(alpha = 0.15f) else palette.cardSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) palette.cyanAccent else palette.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSpec = spec }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = spec.avatarEmoji, fontSize = 16.sp)
                            Text(
                                text = spec.displayName,
                                color = if (isSelected) palette.cyanAccent else palette.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(100.dp)) {
                        Text("Cancel", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Please enter an agent name before deploying."
                                return@Button
                            }
                            if (phoneLine.isBlank()) {
                                errorMessage = "Please enter a valid WhatsApp phone line."
                                return@Button
                            }
                            onCreate(name.trim(), selectedSpec, phoneLine.trim(), maxChats)
                        },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.cyanAccent),
                        modifier = Modifier.testTag("btn_confirm_deploy_agent")
                    ) {
                        Text("DEPLOY AGENT", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Modal Dialog for Global WhatsApp Meta API & Channel Webhook Settings.
 */
@Composable
fun WhatsAppGlobalSettingsDialog(
    settings: WhatsAppGlobalSettings,
    onSave: (WhatsAppGlobalSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalZamaPalette.current
    var businessHoursOnly by remember { mutableStateOf(settings.businessHoursOnly) }
    var autoSyncCalendar by remember { mutableStateOf(settings.autoSyncGoogleCalendar) }
    var requireOwnerApproval by remember { mutableStateOf(settings.requireOwnerApprovalForRefunds) }
    var typingSimulation by remember { mutableStateOf(settings.typingIndicatorSimulation) }
    var escalationPhone by remember { mutableStateOf(settings.emergencyEscalationPhone) }
    var aiResponseDelayMs by remember { mutableFloatStateOf(settings.aiResponseDelayMs.coerceIn(100L, 5000L).toFloat()) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("dialog_whatsapp_global_settings")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WHATSAPP GLOBAL CHANNEL",
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = palette.textMuted)
                    }
                }

                // Cloud API Webhook Status
                Surface(
                    color = palette.greenAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.greenAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = palette.greenAccent, modifier = Modifier.size(16.dp))
                        Column {
                            Text("META CLOUD API: CONNECTED", color = palette.greenAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Webhook: https://api.zama.ai/v1/whatsapp/webhook (HTTP 200 OK)", color = palette.textSecondary, fontSize = 9.sp)
                        }
                    }
                }

                // Owner Escalation Phone
                OutlinedTextField(
                    value = escalationPhone,
                    onValueChange = {
                        escalationPhone = it
                        phoneError = null
                    },
                    label = { Text("Owner Escalation WhatsApp Line") },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { err ->
                        { Text(err, color = Color(0xFFFF5252), fontSize = 10.sp) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.cyanAccent,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
                    )
                )

                // Global AI Response Delay Time (ms) Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI RESPONSE DELAY TIME (MS)",
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${aiResponseDelayMs.roundToInt()} ms",
                            color = palette.cyanAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Millisecond delay before autonomous agent dispatches WhatsApp reply",
                        color = palette.textMuted,
                        fontSize = 9.sp
                    )
                    Slider(
                        value = aiResponseDelayMs,
                        onValueChange = { aiResponseDelayMs = it },
                        valueRange = 100f..5000f,
                        steps = 48,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.cyanAccent,
                            activeTrackColor = palette.cyanAccent
                        ),
                        modifier = Modifier.testTag("slider_global_ai_response_delay_ms")
                    )
                }

                // Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Business Hours Only (08:00 - 20:00)", color = palette.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("Auto-responds with after-hours notice outside these times", color = palette.textMuted, fontSize = 9.5.sp)
                    }
                    Switch(checked = businessHoursOnly, onCheckedChange = { businessHoursOnly = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Sync Google Calendar", color = palette.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("Automatically commit confirmed bookings to device calendar", color = palette.textMuted, fontSize = 9.5.sp)
                    }
                    Switch(
                        checked = autoSyncCalendar,
                        onCheckedChange = { autoSyncCalendar = it },
                        modifier = Modifier.testTag("switch_global_auto_sync_calendar")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simulate Typing Indicators", color = palette.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("Displays WhatsApp 'typing...' status to customer", color = palette.textMuted, fontSize = 9.5.sp)
                    }
                    Switch(checked = typingSimulation, onCheckedChange = { typingSimulation = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require Owner Approval For Refunds", color = palette.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("Never process financial refunds autonomously", color = palette.textMuted, fontSize = 9.5.sp)
                    }
                    Switch(checked = requireOwnerApproval, onCheckedChange = { requireOwnerApproval = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel", fontSize = 11.sp) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (escalationPhone.isBlank() || escalationPhone.count { it.isDigit() } < 7) {
                                phoneError = "Enter a valid WhatsApp phone number (min 7 digits)"
                                return@Button
                            }
                            val updated = settings.copy(
                                businessHoursOnly = businessHoursOnly,
                                autoSyncGoogleCalendar = autoSyncCalendar,
                                typingIndicatorSimulation = typingSimulation,
                                requireOwnerApprovalForRefunds = requireOwnerApproval,
                                emergencyEscalationPhone = escalationPhone.trim(),
                                aiResponseDelayMs = aiResponseDelayMs.roundToInt().toLong()
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.cyanAccent),
                        modifier = Modifier.testTag("btn_save_global_whatsapp_settings")
                    ) {
                        Text("SAVE CONFIG", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Preview card embedded at the bottom of the Agents tab displaying recent stream activity.
 */
@Composable
fun RecentActivityPreviewBanner(
    viewModel: WhatsAppAgentViewModel,
    onOpenFullHistory: () -> Unit
) {
    val palette = LocalZamaPalette.current
    val recentEvents by viewModel.allEvents.collectAsStateWithLifecycle()
    val displayList = remember(recentEvents) { recentEvents.take(3) }

    Surface(
        color = palette.cardSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_activity_preview_banner")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = palette.cyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LIVE ACTIVITY & EVENT AUDIT STREAM",
                        color = palette.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "VIEW FULL LOG (${recentEvents.size}) →",
                    color = palette.cyanAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clickable { onOpenFullHistory() }
                        .testTag("btn_view_full_history_banner")
                )
            }

            if (displayList.isEmpty()) {
                Text(
                    text = "No recent events recorded yet. Simulate an incoming WhatsApp ping to test.",
                    color = palette.textMuted,
                    fontSize = 11.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    displayList.forEach { event ->
                        val eventType = event.typedEventType
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, palette.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenFullHistory() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(eventType.iconEmoji, fontSize = 12.sp)
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = event.title,
                                                color = palette.textPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "• ${event.agentName}",
                                                color = palette.textMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Text(
                                            text = event.detail.take(80) + if (event.detail.length > 80) "..." else "",
                                            color = palette.textSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(
                                    text = event.formattedTime,
                                    color = palette.textMuted,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview banner embedded in Tab 0 showcasing hourly messages throughput and quick link to Recharts.
 */
@Composable
fun HourlyVelocityPreviewBanner(
    viewModel: WhatsAppAgentViewModel,
    onOpenFullCharts: () -> Unit
) {
    val palette = LocalZamaPalette.current
    val hourlyMetrics by viewModel.hourlyMessageMetrics.collectAsStateWithLifecycle()

    Surface(
        color = palette.cardSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hourly_velocity_preview_banner")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = palette.cyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "HOURLY MESSAGE VELOCITY (RECHARTS)",
                        color = palette.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    color = palette.cyanAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clickable { onOpenFullCharts() }
                        .testTag("btn_view_full_recharts_tab")
                ) {
                    Text(
                        text = "EXPLORE RECHARTS →",
                        color = palette.cyanAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Preview Highlights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "TODAY TOTAL", color = palette.textSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${hourlyMetrics.totalMessagesProcessed} msgs",
                            color = palette.cyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "PEAK RUSH", color = palette.textSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${hourlyMetrics.peakHourVolume}/h @ ${hourlyMetrics.peakHourLabel}",
                            color = Color(0xFFFFB300),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "AUTONOMY", color = palette.textSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${hourlyMetrics.overallAutonomyPercentage}% Auto",
                            color = palette.greenAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive configuration slider card on the WhatsApp Agent dashboard
 * to control the AI response delay time in milliseconds (100ms - 5,000ms).
 */
@Composable
fun AiResponseDelayConfigCard(
    currentDelayMs: Long,
    onDelayChanged: (Long) -> Unit
) {
    val palette = LocalZamaPalette.current
    var sliderValue by remember(currentDelayMs) {
        mutableFloatStateOf(currentDelayMs.coerceIn(100L, 5000L).toFloat())
    }

    Surface(
        color = palette.cardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_ai_response_delay_ms")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = palette.cyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "AI RESPONSE DELAY TIME (MS)",
                            color = palette.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Controls autonomous WhatsApp reply dispatch latency in milliseconds",
                            color = palette.textSecondary,
                            fontSize = 9.5.sp
                        )
                    }
                }

                Surface(
                    color = palette.cyanAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${sliderValue.roundToInt()} ms",
                        color = palette.cyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("text_ai_response_delay_ms_value")
                    )
                }
            }

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    onDelayChanged(sliderValue.roundToInt().toLong())
                },
                valueRange = 100f..5000f,
                steps = 48,
                colors = SliderDefaults.colors(
                    thumbColor = palette.cyanAccent,
                    activeTrackColor = palette.cyanAccent,
                    inactiveTrackColor = palette.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slider_dashboard_ai_response_delay_ms")
            )

            // Quick Preset Chips (250ms, 600ms, 850ms, 1500ms, 3000ms)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    250L to "250ms Ultra",
                    600L to "600ms Fast",
                    850L to "850ms Balanced",
                    1500L to "1500ms Natural",
                    3000L to "3000ms Human"
                ).forEach { (presetMs, label) ->
                    val isSelected = kotlin.math.abs(sliderValue.roundToInt() - presetMs.toInt()) < 50
                    Surface(
                        color = if (isSelected) palette.cyanAccent.copy(alpha = 0.2f) else palette.surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) palette.cyanAccent else palette.border
                        ),
                        modifier = Modifier
                            .clickable {
                                sliderValue = presetMs.toFloat()
                                onDelayChanged(presetMs)
                            }
                            .testTag("preset_delay_${presetMs}ms")
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) palette.cyanAccent else palette.textSecondary,
                            fontSize = 8.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

