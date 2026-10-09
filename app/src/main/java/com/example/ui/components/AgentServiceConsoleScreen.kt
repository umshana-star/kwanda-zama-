package com.example.ui.components

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.WhatsAppInteractionEntity
import com.example.service.whatsapp.WhatsAppAgentServiceStatus
import com.example.service.whatsapp.intelligence.AgentDecision
import com.example.ui.theme.LocalChatThemePalette
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaPurple
import com.example.ui.viewmodel.ChatViewModel

@Composable
fun AgentServiceConsoleScreen(
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalChatThemePalette.current

    val serviceStatus by chatViewModel.serviceStatus.collectAsStateWithLifecycle()
    val isAutoReplyActive by chatViewModel.isAutoReplyActive.collectAsStateWithLifecycle()
    val latestDecision by chatViewModel.latestDecision.collectAsStateWithLifecycle()
    val interactionCount by chatViewModel.interactionCount.collectAsStateWithLifecycle()
    val escalatedCount by chatViewModel.escalatedCount.collectAsStateWithLifecycle()
    val interactions by chatViewModel.interactions.collectAsStateWithLifecycle()

    var showWebhookEditor by remember { mutableStateOf(false) }
    var customWebhookPayload by remember {
        mutableStateOf(
            """{
  "object": "whatsapp_business_account",
  "entry": [{
    "changes": [{
      "value": {
        "messaging_product": "whatsapp",
        "contacts": [{"profile": {"name": "Lindiwe Ndlovu"}}],
        "messages": [{
          "from": "+27831234567",
          "id": "wamid_demo_9823",
          "text": {"body": "Hi Zama! Do you have slots for Bridal Styling next month?"}
        }]
      }
    }]
  }]
}"""
        )
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            palette.canvasGradientTop,
            palette.canvasGradientMiddle,
            palette.canvasGradientBottom
        )
    )

    val auditLogs = remember(interactions) {
        interactions.distinctBy {
            if (it.interactionId.isNotBlank()) it.interactionId
            else if (it.id > 0L) "log_id_${it.id}"
            else "log_time_${it.timestampMillis}"
        }.take(15)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(14.dp)
            .testTag("agent_service_console_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Service Header Card
        item {
            ServiceHeaderCard(
                palette = palette,
                serviceStatus = serviceStatus,
                isAutoReplyActive = isAutoReplyActive,
                onToggleAutoReply = { chatViewModel.toggleAutoReply() },
                onExportLogs = {
                    val (_, intent) = chatViewModel.exportChatHistory(context)
                    context.startActivity(intent)
                }
            )
        }

        // Autonomous Agent Settings & Away Responder Automation Panel
        item {
            val isAwayModeActive by chatViewModel.isAwayModeActive.collectAsStateWithLifecycle()
            val awayMessage by chatViewModel.awayMessage.collectAsStateWithLifecycle()

            AgentSettingsPanel(
                palette = palette,
                isAwayModeActive = isAwayModeActive,
                awayMessage = awayMessage,
                isAutoReplyActive = isAutoReplyActive,
                onToggleAwayMode = { chatViewModel.toggleAwayMode(it) },
                onUpdateAwayMessage = { chatViewModel.setAwayMessage(it) },
                onResetAwayMessage = { chatViewModel.resetAwayMessage() },
                onToggleAutoReply = { chatViewModel.toggleAutoReply(it) },
                onSimulateAwayTest = { chatViewModel.simulateAwayMessageTest() }
            )
        }

        // WhatsApp Business Cloud API Communication Layer Card
        item {
            val telemetry by (chatViewModel.whatsAppBusinessManager?.telemetry
                ?: remember { kotlinx.coroutines.flow.MutableStateFlow(com.example.service.whatsapp.api.WhatsAppBusinessTelemetry()) })
                .collectAsStateWithLifecycle()

            WhatsAppBusinessApiCard(
                palette = palette,
                telemetry = telemetry,
                onToggleLiveMode = { isLive ->
                    chatViewModel.whatsAppBusinessManager?.toggleLiveMode(isLive)
                }
            )
        }

        // Real-Time Metrics Row
        item {
            MetricsRow(
                palette = palette,
                totalInteractions = interactionCount,
                escalatedInteractions = escalatedCount,
                status = serviceStatus
            )
        }

        // Autonomous Intelligence Telemetry (Latest Decision)
        item {
            IntelligenceDecisionCard(
                palette = palette,
                decision = latestDecision
            )
        }

        // Webhook Simulation Scenarios
        item {
            WebhookSimulationPanel(
                palette = palette,
                onTriggerBooking = {
                    chatViewModel.simulateInboundCustomerMessage(
                        messageText = "Hi Zama! Can I book Knotless Braids this Saturday at 2:00 PM?",
                        senderName = "Nandi Khumalo",
                        senderPhone = "+27 82 444 8910"
                    )
                },
                onTriggerPricing = {
                    chatViewModel.simulateInboundCustomerMessage(
                        messageText = "Hello, please send me the price list for Silk Press & deep conditioning.",
                        senderName = "Ayanda Dlamini",
                        senderPhone = "+27 83 999 1023"
                    )
                },
                onTriggerEscalation = {
                    chatViewModel.simulateInboundCustomerMessage(
                        messageText = "I am extremely angry! My appointment was cancelled without notice. I demand to speak to the manager immediately.",
                        senderName = "Gillian Meyer",
                        senderPhone = "+27 71 888 4402"
                    )
                },
                onTriggerVoiceNote = {
                    chatViewModel.simulateInboundCustomerMessage(
                        messageText = "Hi Zama, sending an audio voice note inquiry about your luxury bridal treatments.",
                        senderName = "Precious Zulu",
                        senderPhone = "+27 84 555 1209",
                        isVoice = true
                    )
                },
                onToggleCustomWebhook = { showWebhookEditor = !showWebhookEditor }
            )
        }

        // Custom Webhook JSON Runner
        if (showWebhookEditor) {
            item {
                CustomWebhookEditorCard(
                    palette = palette,
                    payload = customWebhookPayload,
                    onPayloadChanged = { customWebhookPayload = it },
                    onDispatchPayload = {
                        chatViewModel.processWebhookPayload(customWebhookPayload)
                    }
                )
            }
        }

        // Live Interaction Audit Logs
        item {
            Text(
                text = "COMMUNICATION API INTERACTION AUDIT LOGS (${interactions.size})",
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            items = auditLogs,
            key = {
                if (it.interactionId.isNotBlank()) it.interactionId
                else if (it.id > 0L) "log_id_${it.id}"
                else "log_time_${it.timestampMillis}"
            }
        ) { log ->
            InteractionLogItem(palette = palette, log = log)
        }
    }
}

@Composable
fun ServiceHeaderCard(
    palette: com.example.ui.theme.ChatThemePalette,
    serviceStatus: WhatsAppAgentServiceStatus,
    isAutoReplyActive: Boolean,
    onToggleAutoReply: () -> Unit,
    onExportLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.headerBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.headerBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_header_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(34.dp)
                            .background(Color(0x3300E5FF), CircleShape)
                            .border(BorderStroke(1.dp, ZamaElectricCyan), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Api,
                            contentDescription = "API Hub",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "WhatsApp Agent Service",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Meta Graph API v21.0 • Webhook Ready",
                            color = palette.textSecondary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Button(
                    onClick = onExportLogs,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (palette.isNeonGlow) Color(0x3300E5FF) else Color(0xFF27272A),
                        contentColor = palette.primaryAccent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, palette.primaryAccent),
                    modifier = Modifier.testTag("export_audit_logs_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export JSON",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "EXPORT JSON", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-Reply Switch Row
            Surface(
                color = Color(0x1F00E5FF),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, Color(0x3300E5FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Autonomous Auto-Reply Pipeline",
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isAutoReplyActive)
                                "Replies automatically dispatched via WhatsApp communication API"
                            else "Paused - interactions will be logged without sending outbound replies",
                            color = palette.textSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = isAutoReplyActive,
                        onCheckedChange = { onToggleAutoReply() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ZamaNeonGreen,
                            checkedTrackColor = Color(0x3300E676),
                            uncheckedThumbColor = Color(0xFF71717A),
                            uncheckedTrackColor = Color(0xFF27272A)
                        ),
                        modifier = Modifier.testTag("auto_reply_toggle_switch")
                    )
                }
            }
        }
    }
}

@Composable
fun MetricsRow(
    palette: com.example.ui.theme.ChatThemePalette,
    totalInteractions: Int,
    escalatedInteractions: Int,
    status: WhatsAppAgentServiceStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Metric 1: Interactions Processed
        MetricCard(
            palette = palette,
            label = "INTERACTIONS",
            value = totalInteractions.toString(),
            subtext = "Logged in Room DB",
            accentColor = palette.primaryAccent,
            modifier = Modifier.weight(1f).testTag("metric_interactions_count")
        )

        // Metric 2: Escalations
        MetricCard(
            palette = palette,
            label = "ESCALATIONS",
            value = escalatedInteractions.toString(),
            subtext = if (escalatedInteractions > 0) "Flagged for Manager" else "All Handled by AI",
            accentColor = if (escalatedInteractions > 0) Color(0xFFFF5252) else ZamaNeonGreen,
            modifier = Modifier.weight(1f).testTag("metric_escalations_count")
        )

        // Metric 3: Service Status
        MetricCard(
            palette = palette,
            label = "SERVICE STATE",
            value = when (status) {
                is WhatsAppAgentServiceStatus.Idle -> "IDLE"
                is WhatsAppAgentServiceStatus.Processing -> "ACTIVE"
                is WhatsAppAgentServiceStatus.Replied -> "READY"
                is WhatsAppAgentServiceStatus.Error -> "ERROR"
            },
            subtext = "Port 8443 / SSL",
            accentColor = when (status) {
                is WhatsAppAgentServiceStatus.Processing -> ZamaElectricCyan
                is WhatsAppAgentServiceStatus.Error -> Color(0xFFFF5252)
                else -> ZamaNeonGreen
            },
            modifier = Modifier.weight(1f).testTag("metric_service_state")
        )
    }
}

@Composable
fun MetricCard(
    palette: com.example.ui.theme.ChatThemePalette,
    label: String,
    value: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = palette.inputBarBackground,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, palette.headerBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                color = palette.textSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = palette.textSecondary,
                fontSize = 8.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun IntelligenceDecisionCard(
    palette: com.example.ui.theme.ChatThemePalette,
    decision: AgentDecision?,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.headerBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (palette.isNeonGlow) Color(0x6600E5FF) else palette.headerBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("intelligence_decision_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Intelligence",
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "AGENT INTELLIGENCE DECISION TELEMETRY",
                        color = palette.textPrimary,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (decision != null) {
                    Surface(
                        color = Color(0x3300E676),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, ZamaNeonGreen)
                    ) {
                        Text(
                            text = "${(decision.confidence * 100).toInt()}% CONFIDENCE",
                            color = ZamaNeonGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (decision == null) {
                Text(
                    text = "Awaiting inbound WhatsApp interaction. Dispatch a simulation or send a message to trigger real-time AI reasoning.",
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
            } else {
                // Classified Intent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CLASSIFIED INTENT:",
                        color = palette.textSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        color = if (decision.requiresHumanEscalation) Color(0x33FF5252) else Color(0x2600E5FF),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(
                            0.5.dp,
                            if (decision.requiresHumanEscalation) Color(0xFFFF5252) else ZamaElectricCyan
                        )
                    ) {
                        Text(
                            text = "${decision.intent.iconEmoji} ${decision.intent.displayName}",
                            color = if (decision.requiresHumanEscalation) Color(0xFFFF5252) else ZamaElectricCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reasoning Trace
                Surface(
                    color = Color(0x1F00E5FF),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color(0x4D00E5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "AI REASONING TRACE",
                            color = ZamaElectricCyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = decision.reasoningTrace,
                            color = Color(0xFFC0E0FF),
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reply formulated
                Text(
                    text = "OUTBOUND WHATSAPP REPLY:",
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = decision.replyText,
                    color = palette.textPrimary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun WebhookSimulationPanel(
    palette: com.example.ui.theme.ChatThemePalette,
    onTriggerBooking: () -> Unit,
    onTriggerPricing: () -> Unit,
    onTriggerEscalation: () -> Unit,
    onTriggerVoiceNote: () -> Unit,
    onToggleCustomWebhook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.headerBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.headerBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("webhook_simulation_panel")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMMUNICATION API WEBHOOK SIMULATOR",
                    color = palette.textPrimary,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Color(0x2600E5FF),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, ZamaElectricCyan),
                    modifier = Modifier.clickable { onToggleCustomWebhook() }
                ) {
                    Text(
                        text = "CUSTOM JSON",
                        color = ZamaElectricCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Dispatch simulated inbound WhatsApp client webhooks to exercise the autonomous agent service and communication API pipeline:",
                color = palette.textSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimulationButton(
                    label = "📅 BOOKING",
                    subtext = "Saturday 2pm",
                    color = ZamaElectricCyan,
                    onClick = onTriggerBooking,
                    modifier = Modifier.weight(1f).testTag("sim_booking_btn")
                )
                SimulationButton(
                    label = "💰 PRICING",
                    subtext = "Silk Press",
                    color = ZamaNeonGreen,
                    onClick = onTriggerPricing,
                    modifier = Modifier.weight(1f).testTag("sim_pricing_btn")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimulationButton(
                    label = "🚨 ESCALATION",
                    subtext = "Complaint",
                    color = Color(0xFFFF5252),
                    onClick = onTriggerEscalation,
                    modifier = Modifier.weight(1f).testTag("sim_escalation_btn")
                )
                SimulationButton(
                    label = "🎙️ VOICE NOTE",
                    subtext = "Bridal Audio",
                    color = ZamaPurple,
                    onClick = onTriggerVoiceNote,
                    modifier = Modifier.weight(1f).testTag("sim_voice_btn")
                )
            }
        }
    }
}

@Composable
fun SimulationButton(
    label: String,
    subtext: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.6f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtext,
                color = color.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun CustomWebhookEditorCard(
    palette: com.example.ui.theme.ChatThemePalette,
    payload: String,
    onPayloadChanged: (String) -> Unit,
    onDispatchPayload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.headerBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.headerBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("custom_webhook_editor_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "INJECT RAW WHATSAPP WEBHOOK JSON",
                color = ZamaElectricCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = payload,
                onValueChange = onPayloadChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("custom_webhook_text_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFC0E0FF),
                    unfocusedTextColor = Color(0xFFC0E0FF),
                    focusedContainerColor = Color(0xFF090E17),
                    unfocusedContainerColor = Color(0xFF090E17),
                    focusedBorderColor = ZamaElectricCyan,
                    unfocusedBorderColor = palette.headerBorder
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDispatchPayload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZamaElectricCyan,
                    contentColor = Color(0xFF05080E)
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().testTag("dispatch_custom_webhook_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Dispatch",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PARSE & ROUTE WEBHOOK VIA INTERACTION ROUTER",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InteractionLogItem(
    palette: com.example.ui.theme.ChatThemePalette,
    log: WhatsAppInteractionEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        color = palette.inputBarBackground,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, palette.headerBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("interaction_log_item")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
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
                        text = log.customerDisplayName,
                        color = palette.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${log.senderPhoneNumber})",
                        color = palette.textSecondary,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    color = if (log.isEscalatedToHuman) Color(0x33FF5252) else Color(0x3300E676),
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(
                        0.5.dp,
                        if (log.isEscalatedToHuman) Color(0xFFFF5252) else ZamaNeonGreen
                    )
                ) {
                    Text(
                        text = if (log.isEscalatedToHuman) "ESCALATED" else log.deliveryStatus,
                        color = if (log.isEscalatedToHuman) Color(0xFFFF5252) else ZamaNeonGreen,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Intent: ${log.resolvedIntent}",
                    color = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFC0D0E0),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${log.latencyMs}ms",
                    color = palette.textSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (!log.replyText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Agent Reply: \"${log.replyText}\"",
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun WhatsAppBusinessApiCard(
    palette: com.example.ui.theme.ChatThemePalette,
    telemetry: com.example.service.whatsapp.api.WhatsAppBusinessTelemetry,
    onToggleLiveMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.headerBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.headerBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("whatsapp_business_api_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .background(
                                if (telemetry.isLiveCloudApiActive) Color(0x3300E676) else Color(0x3300E5FF),
                                CircleShape
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (telemetry.isLiveCloudApiActive) ZamaNeonGreen else ZamaElectricCyan
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Api,
                            contentDescription = "WhatsApp Cloud API",
                            tint = if (telemetry.isLiveCloudApiActive) ZamaNeonGreen else ZamaElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "WhatsApp Business API",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (telemetry.isLiveCloudApiActive) "LIVE META CLOUD API (Active)" else "SANDBOX EMULATION MODE",
                            color = if (telemetry.isLiveCloudApiActive) ZamaNeonGreen else ZamaElectricCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Switch(
                    checked = telemetry.isLiveCloudApiActive,
                    onCheckedChange = onToggleLiveMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ZamaNeonGreen,
                        checkedTrackColor = Color(0x3300E676),
                        uncheckedThumbColor = palette.textSecondary,
                        uncheckedTrackColor = palette.containerSurface
                    ),
                    modifier = Modifier.testTag("toggle_live_api_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry Grid (Inbound, Outbound, Receipts, Errors)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryStatChip(
                    title = "INBOUND",
                    value = "${telemetry.totalInboundMessages}",
                    color = ZamaElectricCyan,
                    modifier = Modifier.weight(1f).testTag("whatsapp_inbound_count")
                )
                TelemetryStatChip(
                    title = "OUTBOUND",
                    value = "${telemetry.totalOutboundMessages}",
                    color = ZamaNeonGreen,
                    modifier = Modifier.weight(1f).testTag("whatsapp_outbound_count")
                )
                TelemetryStatChip(
                    title = "RECEIPTS",
                    value = "${telemetry.totalDeliveryReceipts}",
                    color = Color(0xFFD500F9),
                    modifier = Modifier.weight(1f).testTag("whatsapp_receipts_count")
                )
                TelemetryStatChip(
                    title = "ERRORS",
                    value = "${telemetry.totalFailures}",
                    color = if (telemetry.totalFailures > 0) Color(0xFFFF5252) else palette.textSecondary,
                    modifier = Modifier.weight(1f).testTag("whatsapp_errors_count")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Endpoint & Account Metadata
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.containerSurface.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, palette.headerBorder.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = "Endpoint: https://graph.facebook.com/v21.0/{phone_id}/messages",
                        color = palette.textSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Phone ID: 109847291823901 • Webhook: /v1/whatsapp/webhook",
                        color = palette.textSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = color,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

