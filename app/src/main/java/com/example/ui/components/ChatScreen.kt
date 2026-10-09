package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.VoiceRecordingState
import com.example.data.local.ChatMessage
import com.example.service.whatsapp.WhatsAppAgentServiceStatus
import com.example.ui.theme.ChatThemeMode
import com.example.ui.theme.ChatThemePalette
import com.example.ui.theme.LocalChatThemeMode
import com.example.ui.theme.LocalChatThemePalette
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaPurple
import com.example.ui.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    onRequestMicrophonePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by chatViewModel.chatMessages.collectAsStateWithLifecycle()
    val inputText by chatViewModel.inputMessageText.collectAsStateWithLifecycle()
    val chatThemeMode by chatViewModel.chatThemeMode.collectAsStateWithLifecycle()
    val chatThemePalette by chatViewModel.chatThemePalette.collectAsStateWithLifecycle()
    val voiceState by chatViewModel.voiceRecordingState.collectAsStateWithLifecycle()
    val serviceStatus by chatViewModel.serviceStatus.collectAsStateWithLifecycle()
    val isAutoReplyActive by chatViewModel.isAutoReplyActive.collectAsStateWithLifecycle()
    val isAwayModeActive by chatViewModel.isAwayModeActive.collectAsStateWithLifecycle()
    val awayMessage by chatViewModel.awayMessage.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    CompositionLocalProvider(
        LocalChatThemeMode provides chatThemeMode,
        LocalChatThemePalette provides chatThemePalette
    ) {
        val palette = LocalChatThemePalette.current

        val backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                palette.canvasGradientTop,
                palette.canvasGradientMiddle,
                palette.canvasGradientBottom
            )
        )

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .imePadding()
                .testTag("chat_screen_container")
        ) {
            // Header Bar
            ChatTopHeader(
                palette = palette,
                currentTheme = chatThemeMode,
                serviceStatus = serviceStatus,
                isAutoReplyActive = isAutoReplyActive,
                isAwayModeActive = isAwayModeActive,
                onToggleTheme = { chatViewModel.toggleChatTheme() },
                onExportJson = {
                    val (_, intent) = chatViewModel.exportChatHistory(context)
                    context.startActivity(intent)
                },
                onClearChat = { chatViewModel.clearChat() },
                onOpenSettings = { showSettingsDialog = true }
            )

            // Segmented Digital Art Theme Bar
            ChatThemeSwitcherBar(
                currentChatTheme = chatThemeMode,
                onSelectChatTheme = { mode -> chatViewModel.setChatThemeMode(mode) },
                onToggleChatTheme = { chatViewModel.toggleChatTheme() }
            )

            // Away Mode Active Notice Banner
            AnimatedVisibility(visible = isAwayModeActive) {
                Surface(
                    color = Color(0x33FFB300),
                    border = BorderStroke(0.5.dp, Color(0xFFFFB300)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_away_mode_notice_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Away Responder Active: Auto-replying with away notice",
                                color = Color(0xFFFFD54F),
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "SETTINGS",
                            color = Color(0xFFFFB300),
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showSettingsDialog = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Agent Status Banner if processing
            AnimatedVisibility(visible = serviceStatus is WhatsAppAgentServiceStatus.Processing) {
                if (serviceStatus is WhatsAppAgentServiceStatus.Processing) {
                    val processing = serviceStatus as WhatsAppAgentServiceStatus.Processing
                    Surface(
                        color = Color(0x3300E5FF),
                        border = BorderStroke(0.5.dp, ZamaElectricCyan),
                        modifier = Modifier.fillMaxWidth().testTag("service_processing_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = ZamaElectricCyan
                            )
                            Text(
                                text = "WhatsApp Agent: ${processing.stage}",
                                color = ZamaElectricCyan,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Messages Stream
            val displayMessages = remember(messages) {
                messages.distinctBy {
                    if (it.messageId.isNotBlank()) it.messageId
                    else if (it.id > 0L) "msg_id_${it.id}"
                    else "msg_time_${it.timestampMillis}"
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = displayMessages,
                    key = {
                        if (it.messageId.isNotBlank()) it.messageId
                        else if (it.id > 0L) "msg_id_${it.id}"
                        else "msg_time_${it.timestampMillis}"
                    }
                ) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        palette = palette,
                        onQuickActionClick = { actionText ->
                            chatViewModel.sendUserMessage(actionText)
                        }
                    )
                }
            }

            // Quick Prompt Chips
            QuickPromptChipsBar(
                palette = palette,
                onPromptSelected = { prompt -> chatViewModel.sendUserMessage(prompt) }
            )

            // Voice Recording Bar overlay if active
            if (voiceState is VoiceRecordingState.Recording) {
                VoiceRecordingOverlay(
                    recordingState = voiceState as VoiceRecordingState.Recording,
                    palette = palette,
                    onStop = { chatViewModel.stopVoiceDictation() },
                    onCancel = { chatViewModel.cancelVoiceDictation() }
                )
            }

            // Bottom Input Bar with Speech Dictation
            ChatInputBar(
                palette = palette,
                inputText = inputText,
                voiceState = voiceState,
                onTextChanged = { chatViewModel.onInputTextChanged(it) },
                onSend = { chatViewModel.sendUserMessage() },
                onStartVoice = {
                    val result = chatViewModel.startVoiceDictation()
                    if (result.isFailure) {
                        onRequestMicrophonePermission()
                    }
                },
                onStopVoice = { chatViewModel.stopVoiceDictation() }
            )

            // Settings & Automation Dialog
            if (showSettingsDialog) {
                AgentSettingsDialog(
                    palette = palette,
                    isAwayModeActive = isAwayModeActive,
                    awayMessage = awayMessage,
                    isAutoReplyActive = isAutoReplyActive,
                    onToggleAwayMode = { chatViewModel.toggleAwayMode(it) },
                    onUpdateAwayMessage = { chatViewModel.setAwayMessage(it) },
                    onResetAwayMessage = { chatViewModel.resetAwayMessage() },
                    onToggleAutoReply = { chatViewModel.toggleAutoReply(it) },
                    onSimulateAwayTest = { chatViewModel.simulateAwayMessageTest() },
                    onDismissRequest = { showSettingsDialog = false }
                )
            }
        }
    }
}

@Composable
fun ChatTopHeader(
    palette: ChatThemePalette,
    currentTheme: ChatThemeMode,
    serviceStatus: WhatsAppAgentServiceStatus,
    isAutoReplyActive: Boolean,
    isAwayModeActive: Boolean = false,
    onToggleTheme: () -> Unit,
    onExportJson: () -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = palette.headerBackground,
        border = BorderStroke(1.dp, palette.headerBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat_top_header")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Agent Avatar with Status Indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF142033), CircleShape)
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (isAwayModeActive) Color(0xFFFFB300) else if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFF52525B)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAwayModeActive) Icons.Default.Bedtime else Icons.Default.SmartToy,
                        contentDescription = "Zama WhatsApp Agent",
                        tint = if (isAwayModeActive) Color(0xFFFFB300) else if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Zama AI",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (isAwayModeActive) {
                            Surface(
                                color = Color(0x33FFB300),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFFFB300))
                            ) {
                                Text(
                                    text = "AWAY MODE",
                                    color = Color(0xFFFFB300),
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = if (isAutoReplyActive) Color(0x3300E676) else Color(0x33FFB300),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isAutoReplyActive) ZamaNeonGreen else Color(0xFFFFB300)
                                )
                            ) {
                                Text(
                                    text = if (isAutoReplyActive) "AUTONOMOUS" else "PAUSED",
                                    color = if (isAutoReplyActive) ZamaNeonGreen else Color(0xFFFFB300),
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isAwayModeActive) "Auto-Away Responder Active" else "WhatsApp Cloud API • Graph v21.0",
                        color = if (isAwayModeActive) Color(0xFFFFD54F) else palette.textSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1-Tap Theme Switcher Pill
                ChatThemeSwitcherPill(
                    currentChatTheme = currentTheme,
                    onToggleChatTheme = onToggleTheme
                )

                // Settings & Automation button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(34.dp).testTag("chat_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Agent Settings & Automation",
                        tint = if (isAwayModeActive) Color(0xFFFFB300) else palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Export to JSON button
                IconButton(
                    onClick = onExportJson,
                    modifier = Modifier.size(34.dp).testTag("chat_export_json_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export chat history to JSON",
                        tint = palette.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear chat button
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.size(34.dp).testTag("chat_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear chat history",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    palette: ChatThemePalette,
    onQuickActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpandedTrace by remember { mutableStateOf(false) }

    val isUser = message.isFromUser
    val bubbleBg = if (isUser) palette.customerBubbleBackground else palette.aiBubbleBackground
    val bubbleBorder = if (isUser) palette.customerBubbleBorder else palette.aiBubbleBorder

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (isUser) "customer_message_bubble" else "agent_message_bubble"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleBg,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            border = BorderStroke(
                width = if (palette.isNeonGlow) 1.dp else 0.5.dp,
                color = bubbleBorder
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Sender label & Voice tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "Customer" else "Zama WhatsApp Agent",
                        color = if (isUser) palette.secondaryAccent else palette.primaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    if (message.isVoiceNote) {
                        Surface(
                            color = Color(0x3300E676),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(0.5.dp, ZamaNeonGreen)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Note",
                                    tint = ZamaNeonGreen,
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    text = "VOICE",
                                    color = ZamaNeonGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Message Text Content
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content,
                    color = palette.textPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )

                // Timestamp and WhatsApp ticks
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp,
                        color = palette.textSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = message.statusTicks,
                        color = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFA1A1AA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Agent intent tag and reasoning trace expander
                if (!isUser && !message.aiTrace.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0x1F00E5FF),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color(0x4D00E5FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpandedTrace = !isExpandedTrace }
                            .testTag("ai_reasoning_trace_btn")
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Trace",
                                        tint = ZamaElectricCyan,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = message.intentTag ?: "AGENT INTELLIGENCE TRACE",
                                        color = ZamaElectricCyan,
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpandedTrace) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Trace",
                                    tint = ZamaElectricCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            if (isExpandedTrace) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.aiTrace,
                                    color = Color(0xFFB0D0F0),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPromptChipsBar(
    palette: ChatThemePalette,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val samplePrompts = listOf(
        "Knotless Braids cost & slot",
        "Book Silk Press this Saturday",
        "Urgent: speak to manager",
        "Send salon location & hours"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        samplePrompts.forEach { prompt ->
            Surface(
                color = if (palette.isNeonGlow) Color(0x1F00E5FF) else Color(0xFF1E1E24),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(
                    0.5.dp,
                    if (palette.isNeonGlow) Color(0x6600E5FF) else Color(0xFF3F3F46)
                ),
                modifier = Modifier
                    .clickable { onPromptSelected(prompt) }
                    .testTag("quick_prompt_${prompt.take(6).lowercase()}")
            ) {
                Text(
                    text = prompt,
                    color = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFE4E4E7),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun VoiceRecordingOverlay(
    recordingState: VoiceRecordingState.Recording,
    palette: ChatThemePalette,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_recording")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = Color(0xFA0B1324),
        border = BorderStroke(1.dp, ZamaNeonGreen.copy(alpha = pulseAlpha)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_recording_overlay")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x3300E676), CircleShape)
                        .border(BorderStroke(1.dp, ZamaNeonGreen), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Recording Voice",
                        tint = ZamaNeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "LISTENING... (${recordingState.durationSeconds}s)",
                        color = ZamaNeonGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (recordingState.liveDictatedText.isNotBlank())
                            recordingState.liveDictatedText
                        else "Speak clearly into microphone...",
                        color = Color(0xFFC0E0FF),
                        fontSize = 10.5.sp,
                        maxLines = 1
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(34.dp).testTag("cancel_voice_dictation_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Voice Dictation",
                        tint = Color(0xFFFF5252)
                    )
                }
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(34.dp).testTag("stop_voice_dictation_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Finish Voice Dictation",
                        tint = ZamaNeonGreen
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    palette: ChatThemePalette,
    inputText: String,
    voiceState: VoiceRecordingState,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = voiceState is VoiceRecordingState.Recording

    Surface(
        color = palette.inputBarBackground,
        border = BorderStroke(1.dp, palette.headerBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat_bottom_input_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Voice Dictation Button
            IconButton(
                onClick = {
                    if (isRecording) onStopVoice() else onStartVoice()
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isRecording) Color(0x33FF5252) else Color(0x1F00E5FF),
                        CircleShape
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isRecording) Color(0xFFFF5252) else palette.primaryAccent
                        ),
                        CircleShape
                    )
                    .testTag("chat_voice_dictate_button")
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop voice dictation" else "Dictate voice message",
                    tint = if (isRecording) Color(0xFFFF5252) else palette.primaryAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = "Message WhatsApp Agent...",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedContainerColor = palette.inputFieldFocusedContainer,
                    unfocusedContainerColor = palette.inputFieldUnfocusedContainer,
                    focusedBorderColor = palette.inputFieldFocusedBorder,
                    unfocusedBorderColor = palette.headerBorder
                ),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                singleLine = true
            )

            // Send Button
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank(),
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (inputText.isNotBlank())
                            (if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFF4F4F5))
                        else Color(0x33FFFFFF),
                        CircleShape
                    )
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (inputText.isNotBlank()) Color(0xFF05080E) else Color(0xFF71717A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
