package com.example.ui.components

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AudioRecorderManager
import com.example.audio.VoiceRecordingState
import com.example.data.remote.GeminiAudioTranscriber
import com.example.data.remote.VoiceTranscriptionResult
import com.example.model.ChatMessage
import com.example.model.WHATSAPP_COMPARISONS
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaPurple
import com.example.ui.theme.ZamaVoid
import com.example.ui.viewmodel.ChatHistoryViewModel
import com.example.util.rememberHapticFeedbackManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiEmployeeLiveLab(
    modifier: Modifier = Modifier
) {
    val hapticManager = rememberHapticFeedbackManager()
    var personalityConfig by remember { mutableStateOf(AgentPersonalityConfig()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: WhatsApp, 1: AI Brain, 2: Personality, 3: Comparison
    var messageInput by remember { mutableStateOf("") }
    var isAiTyping by remember { mutableStateOf(false) }
    var aiTypingStage by remember { mutableStateOf("AI Employee is thinking...") }
    val coroutineScope = rememberCoroutineScope()
    val chatScrollState = rememberScrollState()

    val context = LocalContext.current
    val audioRecorder = remember { AudioRecorderManager(context) }
    val geminiTranscriber = remember { GeminiAudioTranscriber() }
    var voiceState by remember { mutableStateOf<VoiceRecordingState>(VoiceRecordingState.Idle) }

    val chatViewModel: ChatHistoryViewModel = viewModel(
        factory = ChatHistoryViewModel.Factory(context.applicationContext as Application)
    )
    val chatMessages by chatViewModel.chatMessages.collectAsStateWithLifecycle()
    val totalLogCount by chatViewModel.totalMessageCount.collectAsStateWithLifecycle()

    // Auto scroll to bottom when new message arrives or typing starts
    LaunchedEffect(chatMessages.size, isAiTyping) {
        chatScrollState.animateScrollTo(chatScrollState.maxValue)
    }

    // AI Neural Thought Trace
    var lastAiTrace by remember {
        mutableStateOf(
            "Intent: PricingInquiry + BookingRequest (Confidence: 99.4%)\n" +
            "Catalog Match: Knotless Braids (Base: R650, Duration: 2.5h)\n" +
            "Calendar Check: Saturday @ 14:00 -> 1 Stylist Free (Slot ID: #BK-749)\n" +
            "Reasoning: Offer confirmed price & suggest instant slot reservation.\n" +
            "Action: Prepared hold on calendar; waiting for customer confirmation."
        )
    }

    fun handleCustomerSend(
        userText: String,
        isVoiceNote: Boolean = false,
        audioModelUsed: String? = null
    ) {
        if (userText.isBlank()) return
        chatViewModel.saveCustomerMessage(
            text = userText,
            formattedTime = "14:05",
            isVoiceNote = isVoiceNote,
            audioModelUsed = audioModelUsed
        )
        messageInput = ""
        isAiTyping = true
        aiTypingStage = "Zama AI is processing customer query..."

        coroutineScope.launch {
            // Natural human-like reading & processing delay (phase 1: thinking)
            delay(700)
            aiTypingStage = "typing..."
            // Phase 2: typing out response
            delay(1100)
            isAiTyping = false

            val lower = userText.lowercase()
            val reply: String
            val trace: String

            val greeting = when {
                personalityConfig.formality > 0.70f -> "Good day, Sarah."
                personalityConfig.formality < 0.35f -> "Hey Sarah! 🔥"
                else -> "Hello Sarah!"
            }

            val signOff = when {
                personalityConfig.formality > 0.70f -> "Our executive salon concierge looks forward to welcoming you."
                personalityConfig.formality < 0.35f -> "GLAM Studio is gonna get you looking fresh!"
                else -> "See you soon at GLAM Studio!"
            }

            val emoji = if (personalityConfig.creativity > 0.65f) " ✨💖" else ""

            when {
                lower.contains("yes") || lower.contains("book") -> {
                    reply = when {
                        personalityConfig.verbosity < 0.35f ->
                            "Confirmed: Knotless Braids, Saturday @ 14:00. Calendar sent."
                        personalityConfig.formality > 0.70f ->
                            "$greeting Your appointment for Knotless Braids is confirmed for this Saturday at 2:00 PM. A formal calendar reservation has been dispatched. $signOff"
                        personalityConfig.formality < 0.35f ->
                            "All locked in! 🎉 Saturday at 2:00 PM for Knotless Braids. Sent the invite to your WhatsApp! $signOff$emoji"
                        else ->
                            "Done Sarah! 🎉 You are booked for Knotless Braids this Saturday at 2:00 PM. A calendar invite and reminder has been sent to your WhatsApp. $signOff"
                    }
                    trace = "Intent: BookingConfirmed (100%)\n" +
                            "Agent Persona: ${personalityConfig.formalityLabel} | Creativity: ${(personalityConfig.creativity * 100).toInt()}%\n" +
                            "Action: Committed slot #BK-749 to Google Calendar & Salon CRM.\n" +
                            "Status: Customer locked; Reminder scheduled for Friday 10:00 AM."
                }
                lower.contains("quote") || lower.contains("bridal") || lower.contains("price") -> {
                    reply = when {
                        personalityConfig.verbosity < 0.35f ->
                            "Bridal Luxury: R1,400 (trial & argan moisture included). Secure date?"
                        personalityConfig.formality > 0.70f ->
                            "$greeting Here is the formal quotation for your bridal styling:\n• Bridal Luxury Styling: R1,400\n• Complimentary Argan Moisture Treatment included\nTotal: R1,400. Shall our concierge secure your wedding date?"
                        else ->
                            "Here is your quotation breakdown 🧾:\n• Bridal Luxury Styling: R1,400\n• Trial session & veil setting included\n• Argan Moisture Treatment: Complimentary$emoji\nTotal: R1,400. Shall I secure your wedding date?"
                    }
                    trace = "Intent: QuotationGeneration (98.7%)\n" +
                            "Agent Persona: ${personalityConfig.formalityLabel} | Verbosity: ${personalityConfig.verbosityLabel}\n" +
                            "Service: Bridal Package tier.\n" +
                            "Discount: Applied complementary moisture treatment rule."
                }
                lower.contains("silk press") || lower.contains("tomorrow") -> {
                    reply = when {
                        personalityConfig.verbosity < 0.35f ->
                            "Silk Press is R500. Open tomorrow: 11:30 or 15:00."
                        personalityConfig.formality > 0.70f ->
                            "$greeting Silk Press & Deep Treatment is R500. For tomorrow, we offer appointments at 11:30 AM or 3:00 PM. Which timing would you prefer?"
                        else ->
                            "We do! Silk Press & Deep Treatment is R500. Tomorrow (Thursday) we have slots open at 11:30 AM or 3:00 PM. Which works best for you?$emoji"
                    }
                    trace = "Intent: RelativeDateQuery ('tomorrow' parsed as 2026-09-17)\n" +
                            "Agent Persona: ${personalityConfig.formalityLabel} | Directive: ${personalityConfig.creativityLabel}\n" +
                            "Availability: 11:30 and 15:00 verified open."
                }
                lower.contains("braid") || lower.contains("knotless") || lower.contains("saturday") -> {
                    reply = when {
                        personalityConfig.verbosity < 0.35f ->
                            "Knotless Braids R650. Saturday 14:00 open. Reserve now?"
                        personalityConfig.formality > 0.70f ->
                            "$greeting For Knotless Braids (R650, 2.5h), we have a prime opening this Saturday at 2:00 PM. Would you like our concierge to reserve this slot?"
                        else ->
                            "$greeting For Knotless Braids (R650, 2.5h), we have a prime opening this Saturday at 2:00 PM. Shall I reserve this slot for you?$emoji"
                    }
                    trace = "Intent: VoiceAppointmentBooking\n" +
                            "Agent Persona: ${personalityConfig.formalityLabel} | Creativity: ${(personalityConfig.creativity * 100).toInt()}%\n" +
                            "Modality: Microphone Audio -> Gemini 3.5 Flash Transcriber\n" +
                            "Action: Reserved tentative slot #BK-749; awaiting customer confirmation."
                }
                else -> {
                    reply = when {
                        personalityConfig.verbosity < 0.35f ->
                            "Services: Knotless Braids (R650), Silk Press (R500), Bridal (R1,400). What would you like to book?"
                        personalityConfig.formality > 0.70f ->
                            "$greeting Our salon services include Knotless Braids from R650, Silk Press at R500, and Luxury Bridal at R1,400. How may our concierge assist your reservation today?"
                        else ->
                            "$greeting Our AI system checked our services: Knotless Braids start at R650, Silk Press R500, and Luxury Bridal R1,400. How can I assist your booking today?$emoji"
                    }
                    trace = "Intent: GeneralInquiry\n" +
                            "Agent Persona: ${personalityConfig.formalityLabel} | Directive: ${personalityConfig.creativityLabel}\n" +
                            "Action: Retrieved top catalog recommendations."
                }
            }

            chatViewModel.saveAiAgentReply(reply, aiTrace = trace, formattedTime = "14:05")
            lastAiTrace = trace
        }
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
                voiceState = VoiceRecordingState.Error("Failed to initialize microphone: ${startRes.exceptionOrNull()?.message}")
            }
        } else {
            voiceState = VoiceRecordingState.Error("Microphone permission was denied. Grant permission to speak with Zama AI.")
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
                voiceState = VoiceRecordingState.Error("Microphone start failed: ${startRes.exceptionOrNull()?.message}")
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
            voiceState = VoiceRecordingState.Error("Voice note was empty. Please speak clearly into the microphone.")
            return
        }

        voiceState = VoiceRecordingState.Transcribing(durationSeconds = 1, audioFile = recordedFile)
        coroutineScope.launch {
            val transResult = geminiTranscriber.transcribeAudio(recordedFile)
            when (transResult) {
                is VoiceTranscriptionResult.Success -> {
                    voiceState = VoiceRecordingState.Transcribed(
                        text = transResult.transcribedText,
                        audioFile = recordedFile,
                        modelUsed = transResult.modelUsed
                    )
                    handleCustomerSend(
                        userText = transResult.transcribedText,
                        isVoiceNote = true,
                        audioModelUsed = transResult.modelUsed
                    )
                    delay(1200)
                    voiceState = VoiceRecordingState.Idle
                }
                is VoiceTranscriptionResult.Error -> {
                    voiceState = VoiceRecordingState.Error(
                        message = transResult.message,
                        isApiKeyMissing = transResult.isApiKeyIssue
                    )
                }
            }
        }
    }

    // Periodic amplitude and timer updates while actively recording
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0C1019),
                        ZamaVoid
                    )
                )
            )
            .border(1.dp, ZamaBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "THE PRODUCT // AI EMPLOYEE",
                    color = ZamaElectricCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "BEHIND WHATSAPP",
                    color = ZamaChromeLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Surface(
                color = Color(0x3300E676),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E676))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ZamaNeonGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE AGENT ACTIVE",
                        color = ZamaNeonGreen,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "You don't want to build another WhatsApp. WhatsApp is the front door. ZAMA AI sits behind WhatsApp and acts as an intelligent, tireless employee that books appointments, quotes prices, and manages clients 24/7.",
            color = ZamaChromeMid,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Navigation Tabs: Customer View vs Business AI Engine vs Agent Personality vs Comparison
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF080B10),
            contentColor = ZamaElectricCyan,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = if (selectedTab == 2) ZamaPurple else ZamaElectricCyan,
                    height = 2.dp
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, ZamaBorder, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    hapticManager.click()
                    selectedTab = 0
                },
                text = {
                    Text(
                        text = "WHATSAPP",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    hapticManager.click()
                    selectedTab = 1
                },
                text = {
                    Text(
                        text = "AI BRAIN",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = {
                    hapticManager.click()
                    selectedTab = 2
                },
                text = {
                    Text(
                        text = "PERSONALITY",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (selectedTab == 2) ZamaPurple else Color.Unspecified,
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = {
                    hapticManager.click()
                    selectedTab = 3
                },
                text = {
                    Text(
                        text = "VS MATRIX",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                // Interactive Simulated WhatsApp Front Door
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0D1418))
                        .border(1.dp, Color(0xFF1F2C34), RoundedCornerShape(16.dp))
                ) {
                    // WhatsApp Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1F2C34))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "Zama AI Employee",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "GLAM Hair Studio • Zama AI",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAiTyping) "typing..." else "Autonomous Employee Online",
                                    color = if (isAiTyping) Color(0xFF25D366) else Color(0xFF8696A0),
                                    fontSize = 11.sp,
                                    fontWeight = if (isAiTyping) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }

                        // Room SQLite Persistence Status & Clear/Reset Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = Color(0x3300E5FF),
                                shape = RoundedCornerShape(100.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = "Room Database",
                                        tint = ZamaElectricCyan,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ROOM: $totalLogCount",
                                        color = ZamaElectricCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { chatViewModel.resetChatHistory() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("reset_chat_history_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Reset Chat History",
                                    tint = ZamaChromeMid,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Active Agent Personality Tuning Quick-Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141C24))
                            .clickable {
                                hapticManager.click()
                                selectedTab = 2
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("banner_personality_tune"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Personality Settings",
                                tint = ZamaPurple,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TONE: ${personalityConfig.formalityLabel.uppercase()} • CREATIVITY: ${(personalityConfig.creativity * 100).toInt()}%",
                                color = ZamaPurple,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "ADJUST SLIDERS ⚙️",
                            color = Color(0xFFB388FF),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Chat messages list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color(0xFF0B141A))
                            .verticalScroll(chatScrollState)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chatMessages.forEach { msg ->
                            val isCustomer = msg.isFromCustomer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isCustomer) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isCustomer) Color(0xFF005C4B) else Color(0xFF202C33),
                                    shape = RoundedCornerShape(
                                        topStart = 10.dp,
                                        topEnd = 10.dp,
                                        bottomStart = if (isCustomer) 10.dp else 2.dp,
                                        bottomEnd = if (isCustomer) 2.dp else 10.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        if (msg.isVoiceNote) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .padding(bottom = 4.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0x3300E5FF))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    .testTag("voice_note_badge")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Voice Input Note",
                                                    tint = Color(0xFF00E5FF),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "VOICE NOTE • ${msg.audioModelUsed ?: "GEMINI 3.5 FLASH"}",
                                                    color = Color(0xFF00E5FF),
                                                    fontSize = 8.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Text(
                                            text = msg.text,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.align(Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = msg.timestamp,
                                                color = Color(0x99FFFFFF),
                                                fontSize = 9.sp
                                            )
                                            if (!isCustomer) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = msg.statusTicks,
                                                    color = Color(0xFF53BDEB),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Animated WhatsApp Typing Indicator Bubble
                        AnimatedVisibility(
                            visible = isAiTyping,
                            enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f),
                            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_typing_indicator"),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                WhatsAppTypingIndicator(
                                    statusText = aiTypingStage
                                )
                            }
                        }
                    }

                    // Voice Transcription Loading Indicator
                    AnimatedVisibility(
                        visible = voiceState is VoiceRecordingState.Transcribing,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f)
                    ) {
                        Surface(
                            color = Color(0xFF091B24),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x6600E5FF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("transcribing_status_banner")
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
                                    text = "Transcribing voice via Gemini API (gemini-3.5-flash)...",
                                    color = ZamaElectricCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Error feedback banner if microphone or Gemini transcription fails
                    AnimatedVisibility(
                        visible = voiceState is VoiceRecordingState.Error,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val errorState = voiceState as? VoiceRecordingState.Error
                        if (errorState != null) {
                            Surface(
                                color = Color(0xFF2B1216),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("voice_error_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Audio Error",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = errorState.message,
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 10.5.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { voiceState = VoiceRecordingState.Idle },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss error",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Scenario Prompts & Voice Simulation Chips
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111B21))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick Voice Note Preset 1
                        Surface(
                            color = Color(0xFF0F2B33),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(0.5.dp, Color(0x6600E5FF)),
                            modifier = Modifier
                                .clickable {
                                    handleCustomerSend(
                                        userText = "Can I book Knotless Braids this Saturday at 2pm?",
                                        isVoiceNote = true,
                                        audioModelUsed = "gemini-3.5-flash"
                                    )
                                }
                                .testTag("chip_voice_braids")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice query",
                                    tint = ZamaElectricCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Voice: \"Saturday 2pm Braids\"",
                                    color = ZamaElectricCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Quick Voice Note Preset 2
                        Surface(
                            color = Color(0xFF0F2B33),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(0.5.dp, Color(0x6600E5FF)),
                            modifier = Modifier
                                .clickable {
                                    handleCustomerSend(
                                        userText = "How much for silk press and moisture treatment tomorrow?",
                                        isVoiceNote = true,
                                        audioModelUsed = "gemini-3.5-flash"
                                    )
                                }
                                .testTag("chip_voice_silkpress")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice query",
                                    tint = ZamaElectricCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Voice: \"Silk Press Price\"",
                                    color = ZamaElectricCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF1F2C34),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier
                                .clickable { handleCustomerSend("Yes, please book it!") }
                                .testTag("chip_yes_book")
                        ) {
                            Text(
                                text = "💬 \"Yes, please book it!\"",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            color = Color(0xFF1F2C34),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier
                                .clickable { handleCustomerSend("Can you send bridal hair packages?") }
                                .testTag("chip_bridal")
                        ) {
                            Text(
                                text = "🧾 \"Send bridal quote\"",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Input Bar (Seamless WhatsApp Voice / Text Bar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF202C33))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (voiceState is VoiceRecordingState.Recording) {
                            // Active Live Audio Recording UI with animated waveform & duration timer
                            val recState = voiceState as VoiceRecordingState.Recording
                            val minutes = recState.durationSeconds / 60
                            val seconds = recState.durationSeconds % 60
                            val formattedDuration = String.format("%02d:%02d", minutes, seconds)

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFF2A3942))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("active_recording_bar"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Blinking Recording Indicator Dot
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color(0xFFFF3B30), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formattedDuration,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Live Dynamic Audio Soundwave Bars responding to mic amplitude
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    val amp = recState.normalizedAmplitude
                                    val barMultipliers = listOf(0.4f, 0.7f, 1.0f, 0.5f, 0.9f, 0.6f, 0.8f)
                                    barMultipliers.forEach { mult ->
                                        val dynamicHeight = (6.dp + (22.dp * (amp * mult))).coerceIn(5.dp, 24.dp)
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(dynamicHeight)
                                                .background(ZamaElectricCyan, RoundedCornerShape(2.dp))
                                        )
                                    }
                                }

                                // Cancel / Discard Recording
                                IconButton(
                                    onClick = { cancelVoiceRecording() },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("cancel_voice_recording_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Cancel Recording",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Send & Transcribe Button with Gemini API
                            IconButton(
                                onClick = { finishAndTranscribeVoice() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0xFF00A884), CircleShape)
                                    .testTag("send_voice_note_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Voice Note to Gemini",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // Standard Text Input + Mic / Send Toggle Button
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                placeholder = {
                                    Text(
                                        "Type message or tap 🎙️ to speak...",
                                        color = Color(0xFF8696A0),
                                        fontSize = 12.sp
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00A884),
                                    unfocusedBorderColor = Color(0xFF3B4A54),
                                    focusedContainerColor = Color(0xFF2A3942),
                                    unfocusedContainerColor = Color(0xFF2A3942)
                                ),
                                shape = RoundedCornerShape(24.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("customer_msg_input")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            if (messageInput.isNotBlank()) {
                                // Text Send Button
                                IconButton(
                                    onClick = {
                                        hapticManager.click()
                                        handleCustomerSend(messageInput)
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFF00A884), CircleShape)
                                        .testTag("customer_msg_send_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Text Message",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                // WhatsApp Microphone Button (records and invokes Gemini 3.5 Flash)
                                IconButton(
                                    onClick = { startVoiceRecording() },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFF00A884), CircleShape)
                                        .testTag("voice_mic_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Record Voice Message with Gemini Transcriber",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Business Neural Dashboard & Real-Time Reasoning Log
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF080B12))
                        .border(1.dp, ZamaBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Neural Trace",
                                tint = ZamaElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE AI REASONING TRACE",
                                color = ZamaChromeLight,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "LATENCY: 0.18s",
                            color = ZamaNeonGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFF04060A),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x3300E5FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastAiTrace,
                            color = ZamaElectricCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "EXECUTIVE SALON METRICS (LIVE)",
                        color = ZamaGraphite,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4 Stat Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "AUTOMATED CHATS",
                            value = "148",
                            sub = "100% replied",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "APPOINTMENTS",
                            value = "42",
                            sub = "+28% vs manual",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "REVENUE UNLOCKED",
                            value = "R27,300",
                            sub = "ZAR this week",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "HOURS SAVED",
                            value = "28.5 hrs",
                            sub = "zero manual typing",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFF131024),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55B388FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hapticManager.click()
                                selectedTab = 2
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ACTIVE NEURAL PROMPT PARAMETERS",
                                    color = ZamaPurple,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tone: ${personalityConfig.formalityLabel} • Creativity: ${(personalityConfig.creativity * 100).toInt()}% • Verbosity: ${personalityConfig.verbosityLabel}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Tuning",
                                tint = ZamaPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            2 -> {
                // WhatsApp Agent Personality & Tone Settings Card
                AgentPersonalitySettingsCard(
                    personality = personalityConfig,
                    onPersonalityChange = { personalityConfig = it },
                    hapticManager = hapticManager
                )
            }

            3 -> {
                // WhatsApp Business vs Zama AI Employee Comparison Table
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF07090F))
                        .border(1.dp, ZamaBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "THE ARCHITECTURAL DIFFERENCE",
                        color = ZamaChromeLight,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    WHATSAPP_COMPARISONS.forEach { item ->
                        Surface(
                            color = Color(0xFF0D121B),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x22FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = item.dimension.uppercase(),
                                    color = ZamaElectricCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "WA Business:",
                                            color = ZamaGraphite,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = item.whatsAppBusiness,
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Zama AI Employee:",
                                            color = ZamaNeonGreen,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = item.zamaAiEmployee,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
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

@Composable
private fun MetricCard(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F1522),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                color = ZamaGraphite,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = ZamaChromeLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                color = ZamaElectricCyan,
                fontSize = 9.sp
            )
        }
    }
}

/**
 * WhatsApp-authentic animated typing bubble with bouncing dots and pulsating status label.
 */
@Composable
private fun WhatsAppTypingIndicator(
    modifier: Modifier = Modifier,
    statusText: String = "Zama AI is thinking..."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots_transition")

    // 3 Staggered bouncing animations for the dots
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    // Alpha pulse for the dots
    val dotPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Surface(
        color = Color(0xFF202C33),
        shape = RoundedCornerShape(
            topStart = 10.dp,
            topEnd = 10.dp,
            bottomStart = 2.dp,
            bottomEnd = 10.dp
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Three bouncing dots bubble
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = dot1Offset.dp)
                        .size(6.dp)
                        .background(Color(0xFF25D366).copy(alpha = dotPulseAlpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot2Offset.dp)
                        .size(6.dp)
                        .background(Color(0xFF25D366).copy(alpha = dotPulseAlpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot3Offset.dp)
                        .size(6.dp)
                        .background(Color(0xFF25D366).copy(alpha = dotPulseAlpha), CircleShape)
                )
            }

            // Status message
            Text(
                text = statusText,
                color = Color(0xFF25D366),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

