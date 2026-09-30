package com.example.ui.components

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import com.example.calendar.BookingTriageParser
import com.example.calendar.CalendarSyncManager
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AudioRecorderManager
import com.example.audio.VoiceRecordingState
import com.example.autoreply.AutoReplyBar
import com.example.data.remote.GeminiAudioTranscriber
import com.example.data.remote.VoiceTranscriptionResult
import com.example.data.remote.VoiceTriageResult
import com.example.model.ChatMessage
import com.example.triage.MessageTriageEngine
import com.example.triage.TriageAnalysis
import com.example.triage.TriageIntent
import com.example.triage.TriagePriorityLevel
import com.example.triage.TriageUrgency
import com.example.ui.theme.LocalZamaPalette
import com.example.ui.theme.LocalZamaThemeMode
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaBorderGlow
import com.example.ui.theme.ZamaCardSurface
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricBlue
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaPurple
import com.example.ui.theme.ZamaVoid
import com.example.ui.viewmodel.ChatHistoryViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.util.rememberHapticFeedbackManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern, dark-themed ChatScreen UI tailored for interacting with the Zama AI autonomous agent.
 * Features:
 * - Ambient obsidian & electric cyan dark canvas
 * - Live message list backed by Room SQLite database
 * - Dynamic input bar with text typing & speech recording (Gemini audio transcription)
 * - Autonomous agent reasoning trace modal / inspector
 * - Quick prompt chips & simulated customer intents
 * - Accessible, 48dp+ interactive targets & clear visual hierarchy
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val hapticManager = rememberHapticFeedbackManager()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messageInput by remember { mutableStateOf("") }
    val isAiTyping by chatViewModel.isAiThinking.collectAsStateWithLifecycle()
    val vmTypingStage by chatViewModel.aiTypingStage.collectAsStateWithLifecycle()
    var localTypingStage by remember { mutableStateOf<String?>(null) }
    val aiTypingStage = localTypingStage ?: vmTypingStage
    var showReasoningTrace by remember { mutableStateOf(false) }
    val latestTraceFromVm by chatViewModel.latestAiTrace.collectAsStateWithLifecycle()
    var inspectedMessageTrace by remember { mutableStateOf<String?>(null) }
    val selectedTraceText = inspectedMessageTrace ?: latestTraceFromVm

    val audioRecorder = remember { AudioRecorderManager(context) }
    val geminiTranscriber = remember { GeminiAudioTranscriber() }
    var voiceState by remember { mutableStateOf<VoiceRecordingState>(VoiceRecordingState.Idle) }

    val rawMessages by chatViewModel.messages.collectAsStateWithLifecycle()
    val chatMessages = remember(rawMessages) {
        rawMessages.map { it.toUiModel() }
    }
    val latestCustomerMessage = remember(chatMessages) {
        chatMessages.lastOrNull { it.isFromCustomer }?.text ?: chatMessages.lastOrNull()?.text
    }
    val totalLogCount = chatMessages.size

    // Search and Sender/Priority Filter State
    var searchQuery by remember { mutableStateOf("") }
    var selectedSenderFilter by remember { mutableStateOf(SenderFilter.ALL) }
    var selectedPriorityFilter by remember { mutableStateOf(PriorityFilter.ALL) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSentimentFlowOverlay by remember { mutableStateOf(false) }
    var specificTriageTarget by remember { mutableStateOf<Pair<ChatMessage, TriageAnalysis>?>(null) }

    val customerCount = remember(chatMessages) {
        chatMessages.count { it.isFromCustomer }
    }
    val aiCount = remember(chatMessages) {
        chatMessages.count { !it.isFromCustomer }
    }

    // Triage metadata cache & urgency counts for high-visibility visual distinction
    val messageTriageMap = remember(chatMessages) {
        chatMessages.filter { it.isFromCustomer }.associateWith { msg ->
            MessageTriageEngine.analyze(msg.text, isCustomer = true)
        }
    }
    val urgentCount = remember(messageTriageMap) {
        messageTriageMap.values.count { it.isUrgent }
    }
    val generalCount = remember(messageTriageMap) {
        messageTriageMap.values.count { !it.isUrgent }
    }

    val isSearchOrFilterActive = remember(searchQuery, selectedSenderFilter, selectedPriorityFilter) {
        searchQuery.isNotBlank() ||
                selectedSenderFilter != SenderFilter.ALL ||
                selectedPriorityFilter != PriorityFilter.ALL
    }

    val filteredMessages = remember(chatMessages, searchQuery, selectedSenderFilter, selectedPriorityFilter, messageTriageMap) {
        chatMessages.filter { msg ->
            val matchesSender = when (selectedSenderFilter) {
                SenderFilter.ALL -> true
                SenderFilter.CUSTOMER -> msg.isFromCustomer
                SenderFilter.AI -> !msg.isFromCustomer
            }
            val matchesPriority = when (selectedPriorityFilter) {
                PriorityFilter.ALL -> true
                PriorityFilter.URGENT -> {
                    if (msg.isFromCustomer) {
                        messageTriageMap[msg]?.isUrgent == true
                    } else false
                }
                PriorityFilter.GENERAL -> {
                    if (msg.isFromCustomer) {
                        messageTriageMap[msg]?.isUrgent == false
                    } else false
                }
            }
            val matchesContent = if (searchQuery.isBlank()) {
                true
            } else {
                msg.text.contains(searchQuery.trim(), ignoreCase = true) ||
                        (msg.actionDetail?.contains(searchQuery.trim(), ignoreCase = true) == true)
            }
            matchesSender && matchesPriority && matchesContent
        }
    }

    // Auto-scroll smoothly when messages change or AI starts typing (unless user is actively browsing filtered search results)
    LaunchedEffect(filteredMessages.size, isAiTyping) {
        if (!isSearchOrFilterActive) {
            if (isAiTyping) {
                // Smoothly scroll down to reveal the active AI typing indicator
                listState.animateScrollToItem(chatMessages.size)
            } else if (chatMessages.isNotEmpty()) {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    // Automatically parse incoming customer messages for booking triage and persist to calendar database
    LaunchedEffect(chatMessages.size) {
        val latest = chatMessages.lastOrNull()
        if (latest != null && latest.isFromCustomer && BookingTriageParser.isBookingRelated(latest.text)) {
            CalendarSyncManager.parseAndIngestMessage(
                context = context,
                messageId = latest.id,
                messageText = latest.text,
                clientName = "WhatsApp Client #${latest.id.takeLast(4)}"
            )
        }
    }

    // Core message handler powered by ChatViewModel and Google Gemini API
    fun handleSendMessage(
        userText: String,
        isVoiceNote: Boolean = false,
        audioModelUsed: String? = null
    ) {
        if (userText.isBlank()) return
        messageInput = ""
        localTypingStage = null
        chatViewModel.sendMessage(
            userText = userText,
            isVoiceNote = isVoiceNote,
            audioModelUsed = audioModelUsed
        )
    }

    // Permission launcher for RECORD_AUDIO
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hapticManager.recordingStarted()
            val startRes = audioRecorder.startRecording()
            if (startRes.isSuccess) {
                voiceState = VoiceRecordingState.Recording(durationSeconds = 0, normalizedAmplitude = 0.1f)
            } else {
                voiceState = VoiceRecordingState.Error("Microphone initialization error: ${startRes.exceptionOrNull()?.message}")
            }
        } else {
            voiceState = VoiceRecordingState.Error("Microphone permission was denied. Please grant permission to record audio.")
        }
    }

    fun startVoiceRecording() {
        hapticManager.recordingStarted()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val startRes = audioRecorder.startRecording()
            if (startRes.isSuccess) {
                voiceState = VoiceRecordingState.Recording(durationSeconds = 0, normalizedAmplitude = 0.1f)
            } else {
                voiceState = VoiceRecordingState.Error("Failed to start mic: ${startRes.exceptionOrNull()?.message}")
            }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun cancelVoiceRecording() {
        hapticManager.recordingCancelled()
        audioRecorder.cancelRecording()
        voiceState = VoiceRecordingState.Idle
    }

    fun finishAndTranscribeVoice() {
        hapticManager.recordingStopped()
        val recordedFile = audioRecorder.stopRecording()
        if (recordedFile == null || recordedFile.length() == 0L) {
            voiceState = VoiceRecordingState.Error("Audio file was empty. Please speak clearly into the microphone.")
            return
        }

        voiceState = VoiceRecordingState.Transcribing(durationSeconds = 1, audioFile = recordedFile)
        coroutineScope.launch {
            val transResult = geminiTranscriber.transcribeAndTriageAudio(recordedFile, isCustomer = true)
            when (transResult) {
                is VoiceTriageResult.Success -> {
                    voiceState = VoiceRecordingState.Transcribed(
                        text = transResult.transcribedText,
                        audioFile = recordedFile,
                        modelUsed = transResult.modelUsed,
                        triageAnalysis = transResult.triageAnalysis
                    )
                    handleSendMessage(
                        userText = transResult.transcribedText,
                        isVoiceNote = true,
                        audioModelUsed = transResult.modelUsed
                    )
                    delay(1500)
                    voiceState = VoiceRecordingState.Idle
                }
                is VoiceTriageResult.Error -> {
                    voiceState = VoiceRecordingState.Error(
                        message = transResult.message,
                        isApiKeyMissing = transResult.isApiKeyIssue
                    )
                }
            }
        }
    }

    // Dynamic amplitude tracking during voice recording
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceRecordingState.Recording) {
            var elapsed = 0
            var ticks = 0
            while (voiceState is VoiceRecordingState.Recording) {
                delay(100)
                ticks++
                if (ticks % 10 == 0) {
                    elapsed++
                }
                val amp = audioRecorder.getNormalizedAmplitude()
                voiceState = VoiceRecordingState.Recording(
                    durationSeconds = elapsed,
                    normalizedAmplitude = amp
                )
            }
        }
    }

    val palette = LocalZamaPalette.current
    val isDarkTheme = palette.isDark

    // Main Container (Adapts to Futuristic Dark or High-Contrast Daylight Light Mode)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (isDarkTheme) {
                        Brush.linearGradient(listOf(ZamaBorderGlow, ZamaElectricCyan.copy(alpha = 0.3f), ZamaBorder))
                    } else {
                        Brush.linearGradient(listOf(palette.border, palette.cyanAccent.copy(alpha = 0.5f), palette.borderGlow))
                    }
                ),
                RoundedCornerShape(24.dp)
            )
            .testTag("chat_screen_container"),
        color = palette.surface,
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDarkTheme) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF07090D),
                                Color(0xFF0D1117),
                                Color(0xFF090D14)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFF8FAFC),
                                Color(0xFFF1F5F9)
                            )
                        )
                    }
                )
        ) {
            // Header Bar
            ChatScreenHeader(
                totalLogs = totalLogCount,
                isAiTyping = isAiTyping,
                typingStage = aiTypingStage,
                isSearchActive = isSearchOrFilterActive || isSearchExpanded,
                urgentCount = urgentCount,
                generalCount = generalCount,
                onFilterUrgent = {
                    hapticManager.click()
                    selectedPriorityFilter = if (selectedPriorityFilter == PriorityFilter.URGENT) PriorityFilter.ALL else PriorityFilter.URGENT
                },
                onToggleSearch = {
                    hapticManager.click()
                    isSearchExpanded = !isSearchExpanded
                },
                onOpenExport = {
                    hapticManager.click()
                    specificTriageTarget = null
                    showExportDialog = true
                },
                onClearHistory = {
                    hapticManager.click()
                    chatViewModel.clearChatHistory()
                },
                onToggleReasoning = {
                    hapticManager.click()
                    showReasoningTrace = !showReasoningTrace
                },
                onOpenSentimentFlow = {
                    hapticManager.click()
                    showSentimentFlowOverlay = true
                }
            )

            // Search and Filter Bar at the top of the chat history screen
            ChatSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedSender = selectedSenderFilter,
                onSenderSelected = { selectedSenderFilter = it },
                selectedPriority = selectedPriorityFilter,
                onPrioritySelected = { selectedPriorityFilter = it },
                urgentCount = urgentCount,
                generalCount = generalCount,
                totalCount = totalLogCount,
                filteredCount = filteredMessages.size,
                customerCount = customerCount,
                aiCount = aiCount,
                isExpanded = isSearchExpanded,
                onToggleExpand = { isSearchExpanded = !isSearchExpanded },
                onExportClick = {
                    hapticManager.click()
                    specificTriageTarget = null
                    showExportDialog = true
                }
            )

            // Chat & Triage Records Export Dialog
            if (showExportDialog) {
                ChatExportDialog(
                    allMessages = chatMessages,
                    filteredMessages = filteredMessages,
                    activeFilterQuery = searchQuery,
                    singleTriageTarget = specificTriageTarget,
                    onDismiss = {
                        showExportDialog = false
                        specificTriageTarget = null
                    }
                )
            }

            // Optional Expandable AI Reasoning / Thought Trace Inspector
            AnimatedVisibility(
                visible = showReasoningTrace,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                AiReasoningTracePanel(
                    traceText = selectedTraceText,
                    onClose = { showReasoningTrace = false }
                )
            }

            // Message List (Room-backed, filtered by content keyword or sender)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(if (isDarkTheme) Color(0xFF080C12) else Color(0xFFF8FAFC))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("chat_screen_message_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        EmptyChatStatePlaceholder()
                    }
                } else if (filteredMessages.isEmpty()) {
                    item {
                        EmptySearchStatePlaceholder(
                            query = searchQuery,
                            senderFilter = selectedSenderFilter,
                            priorityFilter = selectedPriorityFilter,
                            onResetFilters = {
                                searchQuery = ""
                                selectedSenderFilter = SenderFilter.ALL
                                selectedPriorityFilter = PriorityFilter.ALL
                            }
                        )
                    }
                } else {
                    items(
                        items = filteredMessages,
                        key = { it.id }
                    ) { message ->
                        ChatMessageItem(
                            message = message,
                            onInspectTrace = {
                                inspectedMessageTrace = "Inspection for Message #${message.id}:\n" +
                                        "Speaker: ${if (message.isFromCustomer) "Customer" else "Zama AI Agent"}\n" +
                                        "Timestamp: ${message.timestamp}\n" +
                                        "Payload: \"${message.text}\""
                                showReasoningTrace = true
                            },
                            onExportTriage = { msg, triage ->
                                hapticManager.click()
                                specificTriageTarget = Pair(msg, triage)
                                showExportDialog = true
                            }
                        )
                    }
                }

                // Animated AI Typing Indicator item with smooth organic spring physics
                item(key = "ai_typing_indicator_item") {
                    AnimatedVisibility(
                        visible = isAiTyping,
                        enter = fadeIn(animationSpec = tween(220)) +
                                slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) +
                                scaleIn(
                                    initialScale = 0.85f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ),
                        exit = fadeOut(animationSpec = tween(180)) +
                                slideOutVertically(
                                    targetOffsetY = { it / 3 },
                                    animationSpec = tween(180)
                                ) +
                                scaleOut(targetScale = 0.85f, animationSpec = tween(180))
                    ) {
                        AiTypingIndicatorBubble(
                            typingStage = aiTypingStage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Voice Status Notification Banners (Transcribing / Error)
            VoiceStatusBanners(
                voiceState = voiceState,
                onDismissError = { voiceState = VoiceRecordingState.Idle }
            )

            // Auto-Reply Mode Selector Pill Tabs: ML History Patterns vs Triage Quick Replies
            var activeReplyTab by remember { mutableStateOf("ML_AUTOREPLY") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090E16))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (activeReplyTab == "ML_AUTOREPLY") Color(0x3300E5FF) else Color(0x14FFFFFF),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(0.5.dp, if (activeReplyTab == "ML_AUTOREPLY") ZamaElectricCyan else Color(0x2EFFFFFF)),
                    modifier = Modifier
                        .clickable { activeReplyTab = "ML_AUTOREPLY" }
                        .testTag("tab_ml_autoreply")
                ) {
                    Text(
                        text = "🧠 ML Auto-Reply (History Patterns)",
                        color = if (activeReplyTab == "ML_AUTOREPLY") ZamaElectricCyan else ZamaChromeLight,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = if (activeReplyTab == "TRIAGE_QUICK") Color(0x3325D366) else Color(0x14FFFFFF),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(0.5.dp, if (activeReplyTab == "TRIAGE_QUICK") ZamaNeonGreen else Color(0x2EFFFFFF)),
                    modifier = Modifier
                        .clickable { activeReplyTab = "TRIAGE_QUICK" }
                        .testTag("tab_triage_quick")
                ) {
                    Text(
                        text = "⚡ Triage Quick Replies",
                        color = if (activeReplyTab == "TRIAGE_QUICK") ZamaNeonGreen else ZamaChromeLight,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            if (activeReplyTab == "ML_AUTOREPLY") {
                AutoReplyBar(
                    conversationHistory = chatMessages,
                    onSendAutoReply = { replyText ->
                        hapticManager.click()
                        handleSendMessage(
                            userText = replyText,
                            isVoiceNote = false,
                            audioModelUsed = null
                        )
                    },
                    onInsertIntoInput = { insertedText ->
                        hapticManager.click()
                        messageInput = insertedText
                    }
                )
            } else {
                // AI-Powered Customer Message Triage Quick Reply Bar (one-tap replies)
                QuickReplyBar(
                    latestCustomerMessage = latestCustomerMessage,
                    onSendQuickReply = { replyText ->
                        hapticManager.click()
                        handleSendMessage(
                            userText = replyText,
                            isVoiceNote = false,
                            audioModelUsed = null
                        )
                    }
                )
            }

            // Bottom Input Bar
            ChatInputBar(
                inputText = messageInput,
                onInputChange = { messageInput = it },
                voiceState = voiceState,
                onSendMessage = {
                    hapticManager.click()
                    handleSendMessage(messageInput)
                },
                onStartRecording = { startVoiceRecording() },
                onCancelRecording = { cancelVoiceRecording() },
                onFinishRecording = { finishAndTranscribeVoice() }
            )
        }

        // Sentiment Flow Dashboard Overlay Visualizer (Recharts)
        SentimentFlowDashboardOverlay(
            isOpen = showSentimentFlowOverlay,
            onDismiss = { showSentimentFlowOverlay = false },
            chatViewModel = chatViewModel
        )
    }
}

/**
 * Top Header with Status Indicators and Action Buttons.
 */
@Composable
private fun ChatScreenHeader(
    totalLogs: Int,
    isAiTyping: Boolean,
    typingStage: String = "typing...",
    isSearchActive: Boolean = false,
    urgentCount: Int = 0,
    generalCount: Int = 0,
    onFilterUrgent: (() -> Unit)? = null,
    onToggleSearch: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    onClearHistory: () -> Unit,
    onToggleReasoning: () -> Unit,
    onOpenSentimentFlow: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F151E))
            .border(BorderStroke(0.5.dp, ZamaBorder))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_screen_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Agent Identity
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(ZamaElectricCyan, Color(0xFF0A2233))
                        )
                    )
                    .border(1.dp, ZamaElectricCyan.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Zama AI Agent Avatar",
                    tint = ZamaChromeLight,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ZAMA AI AGENT",
                        color = ZamaChromeLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isAiTyping) Color(0xFF25D366) else ZamaNeonGreen, CircleShape)
                    )
                }

                Text(
                    text = if (isAiTyping) "typing • $typingStage" else "Online • Room DB Synced ($totalLogs logs)",
                    color = if (isAiTyping) ZamaElectricCyan else ZamaChromeMid,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )

                // High-visibility live triage tags in header
                if (urgentCount > 0 || generalCount > 0) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (urgentCount > 0) {
                            Surface(
                                color = Color(0xFFFF1744).copy(alpha = 0.28f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.8.dp, Color(0xFFFF1744)),
                                modifier = Modifier
                                    .clickable { onFilterUrgent?.invoke() }
                                    .testTag("header_urgent_triage_chip")
                            ) {
                                Text(
                                    text = "🚨 $urgentCount URGENT",
                                    color = Color(0xFFFF5252),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (generalCount > 0) {
                            Surface(
                                color = Color(0xFF00E5FF).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0x6600E5FF)),
                                modifier = Modifier.testTag("header_general_triage_chip")
                            ) {
                                Text(
                                    text = "💬 $generalCount GENERAL",
                                    color = ZamaElectricCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons: Search Toggle, AI Brain Trace & Clear History
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Search Toggle Button
            Surface(
                color = if (isSearchActive) Color(0x3300E5FF) else Color(0x1A8696A0),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, if (isSearchActive) ZamaElectricCyan else Color(0x338696A0)),
                modifier = Modifier
                    .clickable { onToggleSearch() }
                    .testTag("chat_toggle_search_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search and filter chat history",
                        tint = if (isSearchActive) ZamaElectricCyan else ZamaChromeLight,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SEARCH",
                        color = if (isSearchActive) ZamaElectricCyan else ZamaChromeLight,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Export Button
            Surface(
                color = Color(0x2600E5FF),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, Color(0x6600E5FF)),
                modifier = Modifier
                    .clickable { onOpenExport() }
                    .testTag("chat_open_export_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export chat conversation as PDF or Text",
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "EXPORT",
                        color = ZamaElectricCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                color = Color(0x2600E5FF),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, Color(0x6600E5FF)),
                modifier = Modifier
                    .clickable { onToggleReasoning() }
                    .testTag("chat_toggle_trace_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Inspect AI Trace",
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TRACE",
                        color = ZamaElectricCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sentiment Flow Dashboard Overlay Visualizer Button
            Surface(
                color = Color(0x2600E676),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, Color(0x6600E676)),
                modifier = Modifier
                    .clickable { onOpenSentimentFlow() }
                    .testTag("chat_open_sentiment_flow_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Waves,
                        contentDescription = "Open Sentiment Flow Overlay",
                        tint = ZamaNeonGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FLOW",
                        color = ZamaNeonGreen,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onClearHistory,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("chat_clear_history_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Chat History",
                    tint = ZamaGraphite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Collapsible neural thought trace panel displaying agent internals.
 */
@Composable
private fun AiReasoningTracePanel(
    traceText: String,
    onClose: () -> Unit
) {
    Surface(
        color = Color(0xFF0B141E),
        border = BorderStroke(1.dp, ZamaElectricCyan.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("ai_reasoning_trace_panel")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AGENT REASONING TRACE",
                        color = ZamaElectricCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Trace",
                        tint = ZamaChromeMid,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = traceText,
                color = Color(0xFFB0C5E0),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Individual Chat Message Bubble styled with futuristic dark card aesthetics.
 */
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onInspectTrace: () -> Unit,
    onExportTriage: ((ChatMessage, TriageAnalysis) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isCustomer = message.isFromCustomer
    val triageInfo = remember(message.text, isCustomer) {
        if (isCustomer) MessageTriageEngine.analyze(message.text, isCustomer = true) else null
    }
    val isUrgent = triageInfo?.isUrgent == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_msg_${message.id}"),
        horizontalArrangement = if (isCustomer) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = when {
                isCustomer && isUrgent -> Color(0xFF280E14)
                isCustomer -> Color(0xFF0A332C)
                else -> Color(0xFF131A24)
            },
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isCustomer) 14.dp else 2.dp,
                bottomEnd = if (isCustomer) 2.dp else 14.dp
            ),
            border = BorderStroke(
                if (isCustomer && isUrgent) 1.5.dp else 1.dp,
                when {
                    isCustomer && isUrgent -> Color(0xFFFF1744)
                    isCustomer -> Color(0x4D00E676)
                    else -> Color(0x3300E5FF)
                }
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Header badge if voice note
                if (message.isVoiceNote) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x2600E5FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Query",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VOICE INPUT • ${message.audioModelUsed ?: "GEMINI FLASH"}",
                            color = ZamaElectricCyan,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // AI Triage Category & Urgency Tag for Incoming WhatsApp Customer Messages
                if (isCustomer && triageInfo != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 5.dp)
                    ) {
                        // High-visibility 'URGENT' vs 'GENERAL' priority tag
                        if (isUrgent) {
                            Surface(
                                color = Color(0xFFFF1744).copy(alpha = 0.32f),
                                shape = RoundedCornerShape(3.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF1744)),
                                modifier = Modifier.testTag("triage_tag_urgent_${message.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🚨 URGENT",
                                        color = Color(0xFFFF5252),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.4.sp
                                    )
                                    if (triageInfo.urgency == TriageUrgency.CRITICAL) {
                                        Text(
                                            text = "!",
                                            color = Color(0xFFFF1744),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(3.dp),
                                border = BorderStroke(0.8.dp, Color(0x8000E5FF)),
                                modifier = Modifier.testTag("triage_tag_general_${message.id}")
                            ) {
                                Text(
                                    text = "💬 GENERAL",
                                    color = ZamaElectricCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.4.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Intent Pill (Booking, Inquiry, Complaint, etc.)
                        Surface(
                            color = Color(triageInfo.intent.colorHex).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(0.5.dp, Color(triageInfo.intent.colorHex).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "${triageInfo.intent.iconEmoji} ${triageInfo.intent.displayName.uppercase()}",
                                color = Color(triageInfo.intent.colorHex),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        if (triageInfo.requiresImmediateHumanAttention && triageInfo.intent != TriageIntent.HUMAN_ESCALATION) {
                            Surface(
                                color = Color(0xFFD50000).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(3.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFFF1744))
                            ) {
                                Text(
                                    text = "⚠️ ESCALATED",
                                    color = Color(0xFFFF5252),
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Quick 1-tap Export Triage Incident Record
                        Surface(
                            color = Color(0x2600E5FF),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(0.5.dp, Color(0x6600E5FF)),
                            modifier = Modifier
                                .clickable { onExportTriage?.invoke(message, triageInfo) }
                                .testTag("export_triage_btn_${message.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export Triage Record",
                                    tint = ZamaElectricCyan,
                                    modifier = Modifier.size(8.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "EXPORT",
                                    color = ZamaElectricCyan,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Message Text
                Text(
                    text = message.text,
                    color = ZamaChromeLight,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                // Inline Calendar Sync Action for Booking Triage Messages
                if (isCustomer && triageInfo?.intent == TriageIntent.BOOKING) {
                    val parsedBooking = remember(message.text) {
                        BookingTriageParser.parse(message.text)
                    }
                    if (parsedBooking != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0x2200E676),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x6600E676)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "CALENDAR SYNC READY",
                                        color = Color(0xFF00E676),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${parsedBooking.serviceName} • ${parsedBooking.quotedPrice}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = parsedBooking.formattedDateTime,
                                    color = ZamaChromeMid,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = Color(0xFF00E676),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .clickable {
                                            coroutineScope.launch {
                                                CalendarSyncManager.parseAndIngestMessage(
                                                    context = context,
                                                    messageId = message.id,
                                                    messageText = message.text
                                                )?.let { entity ->
                                                    val intent = CalendarSyncManager.createCalendarInsertIntent(entity)
                                                    context.startActivity(intent)
                                                }
                                            }
                                        }
                                        .testTag("btn_sync_msg_booking_${message.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "ADD TO DEVICE CALENDAR",
                                            color = Color.Black,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer with Timestamp, Status Ticks, and Trace Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isCustomer) {
                        Text(
                            text = "Agent Verified",
                            color = ZamaElectricCyan.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable { onInspectTrace() }
                                .padding(end = 4.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.timestamp,
                            color = ZamaChromeMid.copy(alpha = 0.6f),
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (!isCustomer) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = message.statusTicks,
                                color = ZamaElectricCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Placeholder when no messages exist in the conversation.
 */
@Composable
private fun EmptyChatStatePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFF131B26), CircleShape)
                    .border(1.dp, Color(0x3300E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Empty State",
                    tint = ZamaElectricCyan,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = "Zama AI Autonomous Workspace",
                color = ZamaChromeLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Send a message or voice note to initiate automated client conversations and task execution.",
                color = ZamaChromeMid,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Animated Typing Indicator Bubble for the AI Agent with human-like wave kinetics and living pulse.
 */
@Composable
private fun AiTypingIndicatorBubble(
    typingStage: String,
    modifier: Modifier = Modifier
) {
    val beaconTransition = rememberInfiniteTransition(label = "beacon_transition")
    val beaconScale by beaconTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_scale"
    )
    val beaconAlpha by beaconTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_alpha"
    )

    val borderTransition = rememberInfiniteTransition(label = "shimmer_border")
    val shimmerProgress by borderTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )
    val dynamicBorder = Brush.horizontalGradient(
        colors = listOf(
            Color(0x3300E5FF),
            Color(0x8825D366),
            Color(0x4400E5FF)
        ),
        startX = shimmerProgress * 300f,
        endX = (shimmerProgress * 300f) + 200f
    )

    val dotsTransition = rememberInfiniteTransition(label = "dots_wave")

    val dot1Y by dotsTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 0
                -6.5f at 180 using FastOutSlowInEasing
                0f at 420 using FastOutSlowInEasing
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1_y"
    )
    val dot1Scale by dotsTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.85f at 0
                1.3f at 180 using FastOutSlowInEasing
                0.85f at 420 using FastOutSlowInEasing
                0.85f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1_scale"
    )
    val dot1Alpha by dotsTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.45f at 0
                1.0f at 180 using FastOutSlowInEasing
                0.45f at 420 using FastOutSlowInEasing
                0.45f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1_alpha"
    )

    val dot2Y by dotsTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 0
                0f at 150
                -6.5f at 330 using FastOutSlowInEasing
                0f at 570 using FastOutSlowInEasing
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2_y"
    )
    val dot2Scale by dotsTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.85f at 0
                0.85f at 150
                1.3f at 330 using FastOutSlowInEasing
                0.85f at 570 using FastOutSlowInEasing
                0.85f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2_scale"
    )
    val dot2Alpha by dotsTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.45f at 0
                0.45f at 150
                1.0f at 330 using FastOutSlowInEasing
                0.45f at 570 using FastOutSlowInEasing
                0.45f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2_alpha"
    )

    val dot3Y by dotsTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 0
                0f at 300
                -6.5f at 480 using FastOutSlowInEasing
                0f at 720 using FastOutSlowInEasing
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3_y"
    )
    val dot3Scale by dotsTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.85f at 0
                0.85f at 300
                1.3f at 480 using FastOutSlowInEasing
                0.85f at 720 using FastOutSlowInEasing
                0.85f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3_scale"
    )
    val dot3Alpha by dotsTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.45f at 0
                0.45f at 300
                1.0f at 480 using FastOutSlowInEasing
                0.45f at 720 using FastOutSlowInEasing
                0.45f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3_alpha"
    )

    val ellipsisCycle by dotsTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ellipsis_cycle"
    )
    val dynamicEllipsis = when (ellipsisCycle.toInt()) {
        0 -> "."
        1 -> ".."
        else -> "..."
    }

    Row(
        modifier = modifier
            .semantics {
                contentDescription = "Zama AI is typing a response: $typingStage"
            }
            .testTag("ai_typing_indicator_bubble"),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        // Zama AI Avatar Badge with pulsating beacon
        Box(
            modifier = Modifier
                .padding(end = 8.dp, bottom = 2.dp)
                .size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = Color(0xFF0F1F2C),
                border = BorderStroke(1.dp, Color(0x6600E5FF))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Radar ripple halo
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .scale(beaconScale)
                    .alpha(beaconAlpha)
                    .size(8.dp)
                    .background(Color(0xFF25D366), CircleShape)
            )
            // Solid active beacon center
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(7.dp)
                    .background(Color(0xFF25D366), CircleShape)
                    .border(1.dp, Color(0xFF0F1F2C), CircleShape)
            )
        }

        // Typing Bubble Surface
        Surface(
            color = Color(0xFF14202B),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = 2.dp,
                bottomEnd = 14.dp
            ),
            border = BorderStroke(1.dp, dynamicBorder),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header micro-badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ZAMA AI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ZamaElectricCyan.copy(alpha = 0.9f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "•",
                        fontSize = 9.sp,
                        color = Color(0xFF8696A0)
                    )
                    Text(
                        text = "Autonomous Employee",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF8696A0)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Kinetic 3-dot cluster with wave bounce and glowing radial gradient
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(y = dot1Y.dp)
                                .scale(dot1Scale)
                                .alpha(dot1Alpha)
                                .size(7.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF00FFFF), Color(0xFF25D366))
                                    ),
                                    CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .offset(y = dot2Y.dp)
                                .scale(dot2Scale)
                                .alpha(dot2Alpha)
                                .size(7.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF00FFFF), Color(0xFF25D366))
                                    ),
                                    CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .offset(y = dot3Y.dp)
                                .scale(dot3Scale)
                                .alpha(dot3Alpha)
                                .size(7.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF00FFFF), Color(0xFF25D366))
                                    ),
                                    CircleShape
                                )
                        )
                    }

                    // Subtle separator
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(Color(0x2EFFFFFF))
                    )

                    // Human-like status message
                    Text(
                        text = "$typingStage$dynamicEllipsis",
                        color = Color(0xFFE1E7EC),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

/**
 * Status banners for voice transcription and errors.
 */
@Composable
private fun VoiceStatusBanners(
    voiceState: VoiceRecordingState,
    onDismissError: () -> Unit
) {
    AnimatedVisibility(
        visible = voiceState is VoiceRecordingState.Transcribing,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            color = Color(0xFF091F2C),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, ZamaElectricCyan.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = ZamaElectricCyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transcribing audio via Gemini 3.5 Flash & categorizing intent...",
                    color = ZamaElectricCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    AnimatedVisibility(
        visible = voiceState is VoiceRecordingState.Transcribed && (voiceState as? VoiceRecordingState.Transcribed)?.triageAnalysis != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val transcribed = voiceState as? VoiceRecordingState.Transcribed
        val triage = transcribed?.triageAnalysis
        if (triage != null) {
            Surface(
                color = Color(0xFF07241A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ZamaNeonGreen.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Triage Categorized",
                        tint = ZamaNeonGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice Triaged: ${triage.priorityLevel.iconEmoji} ${triage.priorityLevel.tagText} • ${triage.intent.displayName}",
                        color = ZamaNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = voiceState is VoiceRecordingState.Error,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val error = voiceState as? VoiceRecordingState.Error
        if (error != null) {
            Surface(
                color = Color(0xFF331117),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = error.message,
                        color = Color(0xFFFFCDD2),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick Suggestion Prompts for rapid testing and demonstration.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickPromptSuggestionChips(
    onSelectPrompt: (String, Boolean) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C1017))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Preset Voice Note 1
        Surface(
            color = Color(0xFF0E222D),
            shape = RoundedCornerShape(100.dp),
            border = BorderStroke(0.5.dp, Color(0x6600E5FF)),
            modifier = Modifier
                .clickable { onSelectPrompt("Can I book Knotless Braids this Saturday at 2pm?", true) }
                .testTag("chat_chip_voice_braids")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = ZamaElectricCyan,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "🎙️ \"Saturday 2pm Braids\"",
                    color = ZamaElectricCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Text Preset 1
        Surface(
            color = Color(0xFF182230),
            shape = RoundedCornerShape(100.dp),
            border = BorderStroke(0.5.dp, ZamaBorder),
            modifier = Modifier
                .clickable { onSelectPrompt("What is the cost for Silk Press?", false) }
                .testTag("chat_chip_pricing")
        ) {
            Text(
                text = "💬 \"Silk Press Price\"",
                color = ZamaChromeLight,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        // Text Preset 2
        Surface(
            color = Color(0xFF182230),
            shape = RoundedCornerShape(100.dp),
            border = BorderStroke(0.5.dp, ZamaBorder),
            modifier = Modifier
                .clickable { onSelectPrompt("Yes, please confirm my booking!", false) }
                .testTag("chat_chip_confirm_booking")
        ) {
            Text(
                text = "✅ \"Confirm Booking\"",
                color = ZamaNeonGreen,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

/**
 * Modern Dark-Themed Input Bar Supporting Text & Dynamic Voice Recording.
 */
@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    voiceState: VoiceRecordingState,
    onSendMessage: () -> Unit,
    onStartRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onFinishRecording: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF101620))
            .border(BorderStroke(0.5.dp, ZamaBorder))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("chat_screen_input_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (voiceState is VoiceRecordingState.Recording) {
            // Live Recording Interface with Soundwave and Timer
            val rec = voiceState
            val minutes = rec.durationSeconds / 60
            val seconds = rec.durationSeconds % 60
            val durationText = String.format("%02d:%02d", minutes, seconds)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF182333))
                    .border(BorderStroke(1.dp, Color(0x6600E5FF)), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("chat_active_recording_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFFF3B30), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = durationText,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dynamic Soundwave Bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    val amp = rec.normalizedAmplitude
                    val barMultipliers = listOf(0.4f, 0.7f, 1.0f, 0.5f, 0.9f, 0.6f, 0.8f)
                    barMultipliers.forEach { mult ->
                        val dynamicHeight = (6.dp + (20.dp * (amp * mult))).coerceIn(5.dp, 24.dp)
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(dynamicHeight)
                                .background(ZamaElectricCyan, RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Cancel Recording
                IconButton(
                    onClick = onCancelRecording,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("chat_cancel_recording_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancel Recording",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Send Audio Button
            IconButton(
                onClick = onFinishRecording,
                modifier = Modifier
                    .size(44.dp)
                    .background(ZamaElectricCyan, CircleShape)
                    .testTag("chat_send_voice_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Voice Note",
                    tint = ZamaVoid,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Standard Text Field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = "Message Zama AI agent or tap mic...",
                        color = Color(0xFF6B7A90),
                        fontSize = 12.5.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ZamaChromeLight,
                    unfocusedTextColor = ZamaChromeLight,
                    focusedBorderColor = ZamaElectricCyan,
                    unfocusedBorderColor = ZamaBorder,
                    focusedContainerColor = Color(0xFF161E2B),
                    unfocusedContainerColor = Color(0xFF131A24)
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_screen_text_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (inputText.isNotBlank()) {
                // Send Text Button
                IconButton(
                    onClick = onSendMessage,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(ZamaElectricCyan, Color(0xFF0091EA))
                            ),
                            CircleShape
                        )
                        .testTag("chat_screen_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = ZamaVoid,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Voice Record Microphone Button
                IconButton(
                    onClick = onStartRecording,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF182333), CircleShape)
                        .border(1.dp, Color(0x6600E5FF), CircleShape)
                        .testTag("chat_screen_mic_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak into mic",
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
