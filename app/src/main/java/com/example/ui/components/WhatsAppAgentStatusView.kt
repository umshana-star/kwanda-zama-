package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.PreviewWrapper
import com.example.data.remote.WhatsAppAgentBackendService
import com.example.data.remote.WhatsAppAgentServiceContract
import com.example.data.remote.WhatsAppHeartbeatState
import com.example.ui.theme.ZamaAmberPulse
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Connection states for the autonomous WhatsApp neural bridge.
 */
enum class WhatsAppConnectionStatus(
    val badgeLabel: String,
    val detailLabel: String,
    val colorHex: Long
) {
    CONNECTED(
        badgeLabel = "CONNECTED • WHATSAPP CLOUD BRIDGE",
        detailLabel = "Encrypted Webhook Tunnel Active • 99.98% Uptime",
        colorHex = 0xFF00E676
    ),
    SYNCING(
        badgeLabel = "SYNCING • NEURAL MESSAGE QUEUE",
        detailLabel = "Synchronizing multi-agent conversation state...",
        colorHex = 0xFF00E5FF
    ),
    RECONNECTING(
        badgeLabel = "RECONNECTING • HANDSHAKE RETRY",
        detailLabel = "Re-establishing WhatsApp Business API session...",
        colorHex = 0xFFFFB300
    ),
    DISCONNECTED(
        badgeLabel = "DISCONNECTED • LOCAL FALLBACK GUARD",
        detailLabel = "Webhook offline • Queuing incoming customer pings locally",
        colorHex = 0xFFFF5252
    )
}

/**
 * Represents the active real-time conversation state being handled by the autonomous WhatsApp agent.
 */
data class ActiveConversationState(
    val agentName: String = "Zama Concierge Alpha",
    val phoneLine: String = "+27 82 555 0194",
    val activeThreadsCount: Int = 4,
    val maxConcurrency: Int = 20,
    val activeCustomerName: String = "Naledi Khumalo",
    val activeCustomerPhone: String = "+27 82 419 8820",
    val currentTopic: String = "Knotless Braids Booking • Saturday 14:00",
    val processingStage: String = "AUTONOMOUS_REPLY_SYNTHESIS",
    val confidencePercent: Int = 99,
    val avgLatencyMs: Long = 620L,
    val isAgentGeneratingReply: Boolean = true
)

/**
 * Represents an incoming or queued message processed by the autonomous WhatsApp agent.
 */
data class WhatsAppIncomingAgentMessage(
    val id: String,
    val customerName: String,
    val customerPhone: String,
    val incomingText: String,
    val agentDraftedReply: String,
    val intentTag: String,
    val timestampLabel: String,
    val confidencePercent: Int = 98,
    val isUnreadOrIncoming: Boolean = true
)

/**
 * Default realistic incoming messages from the autonomous WhatsApp agent pipeline.
 */
val DefaultIncomingAgentMessages = listOf(
    WhatsAppIncomingAgentMessage(
        id = "wa_inc_01",
        customerName = "Naledi Khumalo",
        customerPhone = "+27 82 419 8820",
        incomingText = "Hi Zama! Do you have an opening for Knotless Braids this Saturday at 2pm?",
        agentDraftedReply = "Saturday at 2:00 PM is available for Knotless Braids (R650, 2.5h). Shall I lock in Slot #BK-749 for you?",
        intentTag = "BOOKING_TRIAGE",
        timestampLabel = "Just now",
        confidencePercent = 99,
        isUnreadOrIncoming = true
    ),
    WhatsAppIncomingAgentMessage(
        id = "wa_inc_02",
        customerName = "Thandiwe Mbeki",
        customerPhone = "+27 71 903 4412",
        incomingText = "Voice Note (0:14) • Asking price for Silk Press & Deep Moisture treatment tomorrow",
        agentDraftedReply = "Silk Press & Deep Treatment is R500 (1.5h). We have 11:30 AM and 3:00 PM open tomorrow!",
        intentTag = "VOICE_TRANSCRIPTION",
        timestampLabel = "24s ago",
        confidencePercent = 97,
        isUnreadOrIncoming = true
    ),
    WhatsAppIncomingAgentMessage(
        id = "wa_inc_03",
        customerName = "Zanele Dlamini",
        customerPhone = "+27 83 612 0954",
        incomingText = "Can I reschedule my Bridal Styling trial to Sunday at 11am?",
        agentDraftedReply = "Your Bridal Luxury Styling trial (R1,400) has been moved to Sunday at 11:00 AM. Calendar invite updated!",
        intentTag = "CALENDAR_SYNC",
        timestampLabel = "1m ago",
        confidencePercent = 98,
        isUnreadOrIncoming = false
    )
)

/**
 * Futuristic Composable that displays the autonomous WhatsApp agent's real-time connection status,
 * active conversation state matrix, periodic heartbeat telemetry, and incoming message stream.
 * Automatically updates when bound to a [WhatsAppAgentServiceContract] (`WhatsAppAgentBackendService`).
 */
@Composable
fun WhatsAppAgentStatusView(
    modifier: Modifier = Modifier,
    connectionStatus: WhatsAppConnectionStatus = WhatsAppConnectionStatus.CONNECTED,
    conversationState: ActiveConversationState = ActiveConversationState(),
    incomingMessages: List<WhatsAppIncomingAgentMessage> = DefaultIncomingAgentMessages,
    agentService: WhatsAppAgentServiceContract? = null,
    enableAutoHeartbeat: Boolean = true,
    heartbeatIntervalMs: Long = WhatsAppAgentBackendService.DEFAULT_HEARTBEAT_INTERVAL_MS,
    heartbeatStateOverride: WhatsAppHeartbeatState? = null,
    onCycleConnectionStatus: ((WhatsAppConnectionStatus) -> Unit)? = null,
    onSimulateIncomingMessage: (() -> Unit)? = null,
    onMessageClick: (WhatsAppIncomingAgentMessage) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var internalStatus by remember(connectionStatus) { mutableStateOf(connectionStatus) }
    var simulatedPingCounter by remember { mutableIntStateOf(0) }
    var dynamicMessages by remember(incomingMessages) { mutableStateOf(incomingMessages) }
    var dynamicConversation by remember(conversationState) { mutableStateOf(conversationState) }
    var dynamicHeartbeat by remember(heartbeatStateOverride) {
        mutableStateOf(
            heartbeatStateOverride ?: WhatsAppHeartbeatState(
                isRunning = enableAutoHeartbeat,
                sequenceNumber = 1L,
                sessionActive = connectionStatus != WhatsAppConnectionStatus.DISCONNECTED,
                intervalMs = heartbeatIntervalMs
            )
        )
    }

    // Automatically start/stop the periodic background heartbeat coroutine on the bound service
    DisposableEffect(agentService, enableAutoHeartbeat, heartbeatIntervalMs) {
        if (agentService != null && enableAutoHeartbeat) {
            agentService.startPeriodicHeartbeat(
                intervalMs = heartbeatIntervalMs
            )
        }
        onDispose {
            if (agentService != null && enableAutoHeartbeat) {
                agentService.stopPeriodicHeartbeat()
            }
        }
    }

    if (agentService != null) {
        val observedStatus by agentService.connectionStatus.collectAsStateWithLifecycle()
        val observedConversation by agentService.activeConversationState.collectAsStateWithLifecycle()
        val observedMessages by agentService.incomingAgentMessages.collectAsStateWithLifecycle()
        val observedHeartbeat by agentService.heartbeatState.collectAsStateWithLifecycle()

        LaunchedEffect(observedStatus) {
            internalStatus = observedStatus
        }
        LaunchedEffect(observedConversation) {
            dynamicConversation = observedConversation
        }
        LaunchedEffect(observedMessages) {
            dynamicMessages = observedMessages
        }
        LaunchedEffect(observedHeartbeat) {
            dynamicHeartbeat = observedHeartbeat
        }
    }

    val statusColor = Color(internalStatus.colorHex)

    val infiniteTransition = rememberInfiniteTransition(label = "whatsapp_agent_status_transition")
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("whatsapp_agent_status_view"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF060B12)),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    statusColor.copy(alpha = 0.65f),
                    ZamaElectricCyan.copy(alpha = 0.35f),
                    Color(0xFF101B2B)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TOP TELEMETRY HEADER & CONNECTION STATUS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("whatsapp_agent_connection_status_banner"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Futuristic Radar Node
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B1522))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = (size.minDimension / 2f) - 4f
                            drawCircle(
                                color = statusColor.copy(alpha = 0.2f),
                                radius = r,
                                style = Stroke(width = 1.2f)
                            )
                            if (internalStatus != WhatsAppConnectionStatus.DISCONNECTED) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            Color.Transparent,
                                            statusColor.copy(alpha = 0.3f),
                                            statusColor
                                        )
                                    ),
                                    startAngle = radarAngle,
                                    sweepAngle = 100f,
                                    useCenter = false,
                                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Autonomous WhatsApp Agent Node",
                            tint = statusColor,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(
                                    if (internalStatus == WhatsAppConnectionStatus.CONNECTED) pulseScale else 1f
                                )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Surface(
                            color = statusColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("whatsapp_agent_connection_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Text(
                                    text = internalStatus.badgeLabel,
                                    color = statusColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.testTag("whatsapp_agent_connection_text")
                                )
                            }
                        }

                        Text(
                            text = internalStatus.detailLabel,
                            color = ZamaChromeMid,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Latency Chip
                Surface(
                    color = Color(0xFF0D1826),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ZamaElectricCyan.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${dynamicConversation.avgLatencyMs}ms",
                            color = ZamaElectricCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 1-B. PERIODIC HEARTBEAT & SESSION KEEPALIVE TELEMETRY BAR
            Surface(
                color = Color(0xFF09131F),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("whatsapp_agent_heartbeat_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Heartbeat Signal",
                            tint = statusColor,
                            modifier = Modifier
                                .size(15.dp)
                                .scale(if (dynamicHeartbeat.sessionActive) pulseScale else 1f)
                        )
                        Column {
                            Text(
                                text = if (dynamicHeartbeat.sessionActive && internalStatus != WhatsAppConnectionStatus.DISCONNECTED) {
                                    "HEARTBEAT #${dynamicHeartbeat.sequenceNumber} • SESSION ACTIVE (${dynamicHeartbeat.sessionId})"
                                } else {
                                    "HEARTBEAT #${dynamicHeartbeat.sequenceNumber} • SESSION RECONNECTING"
                                },
                                color = statusColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("whatsapp_agent_heartbeat_text")
                            )
                            Text(
                                text = "Keepalive RTT ${dynamicHeartbeat.roundTripLatencyMs}ms • Interval ${dynamicHeartbeat.intervalMs / 1000}s",
                                color = ZamaChromeMid,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clickable(
                                onClickLabel = "Send immediate heartbeat signal"
                            ) {
                                if (agentService != null) {
                                    coroutineScope.launch {
                                        agentService.sendHeartbeatPulse()
                                    }
                                } else {
                                    val nextSeq = dynamicHeartbeat.sequenceNumber + 1L
                                    val nextRtt = 95L + (nextSeq % 25L)
                                    dynamicHeartbeat = dynamicHeartbeat.copy(
                                        sequenceNumber = nextSeq,
                                        roundTripLatencyMs = nextRtt,
                                        sessionActive = true,
                                        lastHeartbeatMillis = System.currentTimeMillis()
                                    )
                                    internalStatus = WhatsAppConnectionStatus.CONNECTED
                                    dynamicConversation = dynamicConversation.copy(avgLatencyMs = nextRtt)
                                }
                            }
                            .testTag("whatsapp_agent_heartbeat_ping_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Send Heartbeat Pulse",
                                tint = statusColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "PULSE",
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 2. ACTIVE CONVERSATION STATE MATRIX
            Surface(
                color = Color(0xFF0B1320),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ZamaElectricCyan.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("whatsapp_agent_active_conversation_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = ZamaElectricCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "ACTIVE CONVERSATION STATE",
                                color = ZamaElectricCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            )
                        }

                        // Live Concurrency Counter
                        Surface(
                            color = ZamaNeonGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ZamaNeonGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${dynamicConversation.activeThreadsCount}/${dynamicConversation.maxConcurrency} LIVE THREADS",
                                color = ZamaNeonGreen,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("whatsapp_agent_active_threads_badge")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "${dynamicConversation.agentName} • ${dynamicConversation.phoneLine}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Client: ${dynamicConversation.activeCustomerName} (${dynamicConversation.activeCustomerPhone})",
                                color = ZamaChromeLight,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = dynamicConversation.currentTopic,
                                color = ZamaNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("whatsapp_agent_current_topic_text")
                            )
                        }

                        // Futuristic Neural Waveform Canvas
                        NeuralWaveformVisualizer(
                            phase = wavePhase,
                            isActive = dynamicConversation.isAgentGeneratingReply &&
                                internalStatus != WhatsAppConnectionStatus.DISCONNECTED,
                            accentColor = ZamaNeonGreen
                        )
                    }

                    // Processing Stage & Confidence Bar
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STAGE: ${dynamicConversation.processingStage}",
                                color = ZamaChromeMid,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "CONFIDENCE ${dynamicConversation.confidencePercent}%",
                                color = ZamaElectricCyan,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (dynamicConversation.confidencePercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ZamaNeonGreen,
                            trackColor = Color(0xFF162436)
                        )
                    }
                }
            }

            // 3. POTENTIAL INCOMING MESSAGES & AUTONOMOUS DISPATCH STREAM
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("whatsapp_agent_incoming_messages_section"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = ZamaNeonGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "INCOMING & AUTONOMOUS MESSAGE STREAM (${dynamicMessages.size})",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Text(
                        text = "REAL-TIME WEBHOOK",
                        color = ZamaElectricCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                dynamicMessages.take(3).forEachIndexed { index, msg ->
                    IncomingAgentMessageItemCard(
                        message = msg,
                        index = index,
                        onClick = { onMessageClick(msg) }
                    )
                }
            }

            HorizontalDivider(color = ZamaBorder.copy(alpha = 0.5f))

            // 4. INTERACTIVE FUTURISTIC CONTROLS (Minimum 48.dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        simulatedPingCounter += 1
                        val newMsg = WhatsAppIncomingAgentMessage(
                            id = "wa_sim_${System.currentTimeMillis()}",
                            customerName = "Lindiwe Zulu #${simulatedPingCounter}",
                            customerPhone = "+27 82 774 190$simulatedPingCounter",
                            incomingText = "Hi! Can I book Goddess Box Braids tomorrow at 11:30 AM?",
                            agentDraftedReply = "Goddess Box Braids (R850, 3h) is open tomorrow at 11:30 AM! Slot #BK-80$simulatedPingCounter reserved.",
                            intentTag = "LIVE_WEBHOOK_PING",
                            timestampLabel = "Just now",
                            confidencePercent = 99,
                            isUnreadOrIncoming = true
                        )
                        dynamicMessages = listOf(newMsg) + dynamicMessages.take(2)
                        dynamicConversation = dynamicConversation.copy(
                            activeThreadsCount = (dynamicConversation.activeThreadsCount + 1).coerceAtMost(
                                dynamicConversation.maxConcurrency
                            ),
                            activeCustomerName = newMsg.customerName,
                            activeCustomerPhone = newMsg.customerPhone,
                            currentTopic = "Goddess Box Braids • Tomorrow 11:30 AM"
                        )
                        onSimulateIncomingMessage?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZamaNeonGreen,
                        contentColor = Color(0xFF040A06)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("whatsapp_agent_simulate_message_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Simulate Incoming Message",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SIMULATE INCOMING PING",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val values = WhatsAppConnectionStatus.entries
                        val next = values[(internalStatus.ordinal + 1) % values.size]
                        internalStatus = next
                        onCycleConnectionStatus?.invoke(next)
                    },
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("whatsapp_agent_cycle_status_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Cycle Connection State",
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "STATUS",
                            color = statusColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingAgentMessageItemCard(
    message: WhatsAppIncomingAgentMessage,
    index: Int,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF0A121E),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (message.isUnreadOrIncoming) {
                ZamaNeonGreen.copy(alpha = 0.42f)
            } else {
                ZamaBorder.copy(alpha = 0.6f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("whatsapp_incoming_message_item_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (message.isUnreadOrIncoming) ZamaNeonGreen else ZamaElectricCyan,
                                CircleShape
                            )
                    )
                    Text(
                        text = "${message.customerName} (${message.customerPhone})",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = ZamaElectricCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, ZamaElectricCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = message.intentTag,
                            color = ZamaElectricCyan,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = message.timestampLabel,
                        color = ZamaChromeMid,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Incoming Customer Message
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = ZamaChromeMid,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Text(
                    text = message.incomingText,
                    color = ZamaChromeLight,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Autonomous Agent Synthesized Reply Preview
            Surface(
                color = Color(0xFF071914),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.8.dp, ZamaNeonGreen.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = ZamaNeonGreen,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = "Agent Auto-Reply (${message.confidencePercent}%): ${message.agentDraftedReply}",
                        color = ZamaNeonGreen,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NeuralWaveformVisualizer(
    phase: Float,
    isActive: Boolean,
    accentColor: Color
) {
    Canvas(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .testTag("whatsapp_agent_neural_waveform")
    ) {
        val barCount = 6
        val spacing = size.width / (barCount + 1)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val x = spacing * (i + 1)
            val normalizedAmp = if (isActive) {
                ((sin(phase + i * 0.9f) + 1f) / 2f).coerceIn(0.2f, 1f)
            } else {
                0.22f
            }
            val barHalfHeight = (size.height * 0.42f) * normalizedAmp
            drawLine(
                color = accentColor,
                start = Offset(x, centerY - barHalfHeight),
                end = Offset(x, centerY + barHalfHeight),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true, name = "WhatsAppAgentStatusView - Connected")
@Composable
fun WhatsAppAgentStatusViewConnectedPreview() {
    PreviewWrapper(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            WhatsAppAgentStatusView(
                connectionStatus = WhatsAppConnectionStatus.CONNECTED
            )
        }
    }
}

@Preview(showBackground = true, name = "WhatsAppAgentStatusView - Syncing")
@Composable
fun WhatsAppAgentStatusViewSyncingPreview() {
    PreviewWrapper(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            WhatsAppAgentStatusView(
                connectionStatus = WhatsAppConnectionStatus.SYNCING
            )
        }
    }
}
