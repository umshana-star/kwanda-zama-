package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.EventCategoryFilter
import com.example.model.WhatsAppAgentEntity
import com.example.model.WhatsAppAgentEventEntity
import com.example.model.WhatsAppAgentEventType
import com.example.model.WhatsAppEventSeverity
import com.example.ui.theme.LocalZamaPalette
import com.example.ui.viewmodel.WhatsAppAgentViewModel

/**
 * Flagship History Log View for the WhatsApp agent cluster.
 * Displays recent customer inquiries, autonomous AI replies, and operational event status updates
 * in the Agent Management Dashboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppAgentHistoryLogView(
    viewModel: WhatsAppAgentViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAgentEdit: ((WhatsAppAgentEntity) -> Unit)? = null
) {
    val palette = LocalZamaPalette.current
    val historyLogs by viewModel.filteredHistoryLogs.collectAsStateWithLifecycle()
    val allAgents by viewModel.allAgents.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedEventCategory.collectAsStateWithLifecycle()
    val selectedAgentId by viewModel.selectedAgentLogFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.logSearchQuery.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalEventCount.collectAsStateWithLifecycle()
    val messagesCount by viewModel.totalMessagesCount.collectAsStateWithLifecycle()
    val escalationsCount by viewModel.totalEscalationCount.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulatingPing.collectAsStateWithLifecycle()

    var selectedEventForDetail by remember { mutableStateOf<WhatsAppAgentEventEntity?>(null) }
    var showConfirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("whatsapp_agent_history_log_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Audit Trail",
                        tint = palette.cyanAccent,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "EVENT & CONVERSATION HISTORY LOG",
                            color = palette.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                        LiveHeartbeatBeacon()
                    }
                    Text(
                        text = "Real-time audit trail of client chats & autonomous node status transitions",
                        color = palette.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick simulation action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = palette.cyanAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clickable(enabled = !isSimulating) {
                            val targetAgent = allAgents.firstOrNull()?.id ?: "agent_thandiwe_01"
                            viewModel.simulateCustomerWhatsAppPing(targetAgent)
                        }
                        .testTag("btn_sim_inbound_ping")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSimulating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = palette.cyanAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = palette.cyanAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = if (isSimulating) "STREAMING..." else "SIMULATE PING",
                            color = palette.cyanAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    color = Color(0xFFFF1744).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF1744).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clickable {
                            val target = allAgents.lastOrNull()?.id ?: "agent_zama_vip_04"
                            viewModel.triggerEscalationAlert(target, "Customer sentiment threshold breach (<40%)")
                        }
                        .testTag("btn_sim_escalation")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "ALERT",
                            color = Color(0xFFFF5252),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    color = palette.cardSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .clickable { showConfirmClear = true }
                        .testTag("btn_clear_history_logs")
                ) {
                    Text(
                        text = "CLEAR",
                        color = palette.textSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Telemetry Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogMiniStat(
                title = "TOTAL EVENTS",
                count = "$totalCount",
                subtext = "Persisted in Room",
                color = palette.cyanAccent,
                modifier = Modifier.weight(1f)
            )
            LogMiniStat(
                title = "MESSAGES",
                count = "$messagesCount",
                subtext = "In/Out WhatsApp",
                color = palette.greenAccent,
                modifier = Modifier.weight(1f)
            )
            LogMiniStat(
                title = "STATUS UPDATES",
                count = "${(totalCount - messagesCount - escalationsCount).coerceAtLeast(0)}",
                subtext = "State Transitions",
                color = Color(0xFFFFD600),
                modifier = Modifier.weight(1f)
            )
            LogMiniStat(
                title = "ESCALATIONS",
                count = "$escalationsCount",
                subtext = "Owner Handoffs",
                color = Color(0xFFFF5252),
                modifier = Modifier.weight(1f)
            )
        }

        // Search Bar & Filters
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setLogSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_history_search"),
                placeholder = {
                    Text(
                        "Search messages, client phones, or AI traces...",
                        color = palette.textMuted,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = palette.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setLogSearchQuery("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.cyanAccent,
                    unfocusedBorderColor = palette.border,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedContainerColor = palette.cardSurface,
                    unfocusedContainerColor = palette.cardSurface
                )
            )

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EventCategoryFilter.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        color = if (isSelected) palette.cyanAccent.copy(alpha = 0.22f) else palette.cardSurface,
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) palette.cyanAccent else palette.border
                        ),
                        modifier = Modifier
                            .clickable { viewModel.setEventCategoryFilter(category) }
                            .testTag("chip_log_filter_${category.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(category.iconEmoji, fontSize = 11.sp)
                            Text(
                                text = category.label,
                                color = if (isSelected) palette.cyanAccent else palette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Agent Filter Selector Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val isAll = selectedAgentId == null
                    Surface(
                        color = if (isAll) palette.greenAccent.copy(alpha = 0.2f) else palette.cardSurface,
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, if (isAll) palette.greenAccent else palette.border),
                        modifier = Modifier
                            .clickable { viewModel.setSelectedAgentLogFilter(null) }
                            .testTag("chip_agent_filter_all")
                    ) {
                        Text(
                            text = "All Agents (${allAgents.size})",
                            color = if (isAll) palette.greenAccent else palette.textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                items(allAgents) { agent ->
                    val isSelected = selectedAgentId == agent.id
                    Surface(
                        color = if (isSelected) palette.greenAccent.copy(alpha = 0.2f) else palette.cardSurface,
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, if (isSelected) palette.greenAccent else palette.border),
                        modifier = Modifier
                            .clickable { viewModel.setSelectedAgentLogFilter(agent.id) }
                            .testTag("chip_agent_filter_${agent.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(agent.avatarEmoji, fontSize = 11.sp)
                            Text(
                                text = agent.name,
                                color = if (isSelected) palette.greenAccent else palette.textSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // History Log List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (historyLogs.isEmpty()) {
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
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = palette.textMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No history events found matching current criteria",
                            color = palette.textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = {
                                viewModel.setEventCategoryFilter(EventCategoryFilter.ALL)
                                viewModel.setSelectedAgentLogFilter(null)
                                viewModel.setLogSearchQuery("")
                            },
                            modifier = Modifier.testTag("btn_reset_history_filters")
                        ) {
                            Text("Reset All Filters", color = palette.cyanAccent, fontSize = 11.5.sp)
                        }
                    }
                }
            } else {
                historyLogs.forEach { event ->
                    WhatsAppHistoryEventCard(
                        event = event,
                        onClick = { selectedEventForDetail = event }
                    )
                }
            }
        }
    }

    // Detail Inspection Modal
    selectedEventForDetail?.let { event ->
        EventDetailInspectionDialog(
            event = event,
            onDismiss = { selectedEventForDetail = null }
        )
    }

    // Confirm Clear Dialog
    if (showConfirmClear) {
        Dialog(
            onDismissRequest = { showConfirmClear = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.dp, palette.border), RoundedCornerShape(20.dp)),
                color = palette.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Clear WhatsApp History Log?",
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This will remove all stored message logs, webhooks, and operational status events from local database storage.",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showConfirmClear = false }) {
                            Text("CANCEL", color = palette.textSecondary, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                viewModel.clearAllLogs()
                                showConfirmClear = false
                            },
                            modifier = Modifier.testTag("btn_confirm_clear_logs")
                        ) {
                            Text("CLEAR ALL", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Timeline Card representing an event or message.
 */
@Composable
private fun WhatsAppHistoryEventCard(
    event: WhatsAppAgentEventEntity,
    onClick: () -> Unit
) {
    val palette = LocalZamaPalette.current
    val eventType = event.typedEventType
    val severity = event.typedSeverity
    var isExpanded by remember { mutableStateOf(false) }

    val accentColor = when {
        eventType == WhatsAppAgentEventType.HANDOFF_TRIGGERED -> Color(0xFFFF5252)
        eventType == WhatsAppAgentEventType.MESSAGE_OUTBOUND -> palette.greenAccent
        eventType == WhatsAppAgentEventType.MESSAGE_INBOUND -> palette.cyanAccent
        eventType == WhatsAppAgentEventType.SLOT_RESERVED -> Color(0xFFB388FF)
        eventType == WhatsAppAgentEventType.PRICING_QUOTE -> Color(0xFF80CBC4)
        eventType == WhatsAppAgentEventType.KILL_SWITCH_TOGGLED -> Color(0xFFFF9100)
        else -> palette.cyanAccent
    }

    Surface(
        color = palette.cardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (eventType == WhatsAppAgentEventType.HANDOFF_TRIGGERED) Color(0xFFFF5252).copy(alpha = 0.5f) else palette.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("card_history_event_${event.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Event Type, Agent Tag, Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${eventType.iconEmoji} ${eventType.label}",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, palette.border)
                    ) {
                        Text(
                            text = event.agentName,
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (event.latencyMs != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = palette.textMuted,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${event.latencyMs}ms",
                                color = palette.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = event.formattedTime,
                        color = palette.textMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Message or Event Title
            Text(
                text = event.title,
                color = palette.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // Detail / Message Text Box
            Surface(
                color = if (eventType == WhatsAppAgentEventType.MESSAGE_OUTBOUND) {
                    palette.greenAccent.copy(alpha = 0.08f)
                } else if (eventType == WhatsAppAgentEventType.MESSAGE_INBOUND) {
                    palette.cyanAccent.copy(alpha = 0.08f)
                } else {
                    palette.surface
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    0.5.dp,
                    if (eventType == WhatsAppAgentEventType.MESSAGE_OUTBOUND) {
                        palette.greenAccent.copy(alpha = 0.25f)
                    } else if (eventType == WhatsAppAgentEventType.MESSAGE_INBOUND) {
                        palette.cyanAccent.copy(alpha = 0.25f)
                    } else {
                        palette.border
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (event.customerPhone != null || event.customerName != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👤 ${event.customerName ?: "Customer"} (${event.customerPhone ?: ""})",
                                color = palette.cyanAccent,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (eventType == WhatsAppAgentEventType.MESSAGE_OUTBOUND) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Delivered",
                                        tint = palette.greenAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "AUTONOMOUS",
                                        color = palette.greenAccent,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = event.detail,
                        color = palette.textPrimary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // AI Reasoning Trace / Metadata expansion (if present)
            if (event.aiTrace != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = palette.cyanAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "AI REASONING TRACE",
                            color = palette.cyanAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (event.confidence != null) {
                            Text(
                                text = "• ${(event.confidence * 100).toInt()}% Match",
                                color = palette.textMuted,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = event.aiTrace,
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Bottom metadata badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                event.metadataBadge?.let { badge ->
                    Surface(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge,
                            color = accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Tap for payload details →",
                    color = palette.textMuted,
                    fontSize = 9.5.sp
                )
            }
        }
    }
}

/**
 * Audit Log Inspector Dialog for viewing raw event payload and telemetry diagnostics.
 */
@Composable
private fun EventDetailInspectionDialog(
    event: WhatsAppAgentEventEntity,
    onDismiss: () -> Unit
) {
    val palette = LocalZamaPalette.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(BorderStroke(1.dp, palette.border), RoundedCornerShape(20.dp))
                .testTag("dialog_event_inspector"),
            color = palette.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Surface(
                            color = palette.cyanAccent.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = event.typedEventType.iconEmoji,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "EVENT AUDIT INSPECTION",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Event ID: ${event.id}",
                                color = palette.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = palette.border)

                // Property Matrix
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropertyRow("Event Type", "${event.typedEventType.name} (${event.typedEventType.label})")
                    PropertyRow("Agent Node", "${event.agentName} [${event.agentId}]")
                    PropertyRow("Timestamp", "${event.formattedTime} (Epoch: ${event.timestampMillis})")
                    PropertyRow("Severity", event.severity)
                    if (event.customerPhone != null) {
                        PropertyRow("Customer", "${event.customerName.orEmpty()} • ${event.customerPhone}")
                    }
                    if (event.latencyMs != null) {
                        PropertyRow("Processing Latency", "${event.latencyMs}ms (Meta Webhook -> Synthesis)")
                    }
                    if (event.confidence != null) {
                        PropertyRow("Model Confidence", "${(event.confidence * 100).toInt()}% Match")
                    }
                }

                // Full detail
                Text(
                    text = "RAW PAYLOAD / CONTENT",
                    color = palette.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Surface(
                    color = palette.cardSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = event.detail,
                        color = palette.textPrimary,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (event.aiTrace != null) {
                    Text(
                        text = "NEURAL INFERENCE REASONING",
                        color = palette.cyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        color = palette.cardSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, palette.cyanAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = event.aiTrace,
                            color = palette.cyanAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("CLOSE", color = palette.cyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    val palette = LocalZamaPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = palette.textMuted, fontSize = 11.sp)
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun LogMiniStat(
    title: String,
    count: String,
    subtext: String,
    color: Color,
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
            Text(
                text = title,
                color = palette.textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = count,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtext,
                color = palette.textSecondary,
                fontSize = 8.5.sp
            )
        }
    }
}
