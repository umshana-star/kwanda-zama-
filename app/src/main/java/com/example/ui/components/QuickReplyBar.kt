package com.example.ui.components

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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.triage.MessageTriageEngine
import com.example.triage.QuickReplySuggestion
import com.example.triage.TriageAnalysis
import com.example.triage.TriageIntent
import com.example.triage.TriageUrgency
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen

/**
 * Quick Reply Bar positioned directly above the text input field in ChatScreen.
 * Suggests common responses tailored to the incoming customer message's triage category
 * (e.g., Booking, Inquiry, Complaint, Human Escalation), enabling instantaneous one-tap replies.
 */
@Composable
fun QuickReplyBar(
    latestCustomerMessage: String?,
    onSendQuickReply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Perform AI triage analysis on the latest customer message
    val autoTriageAnalysis = remember(latestCustomerMessage) {
        if (!latestCustomerMessage.isNullOrBlank()) {
            MessageTriageEngine.analyze(latestCustomerMessage, isCustomer = true)
        } else {
            // Default initial triage state
            MessageTriageEngine.analyze("Hello, I want to book an appointment", isCustomer = true)
        }
    }

    // Allow business owner to manually override/switch categories, or stay in "Auto" mode
    var selectedCategoryOverride by remember { mutableStateOf<TriageIntent?>(null) }
    val activeIntent = selectedCategoryOverride ?: autoTriageAnalysis.intent

    // Suggested replies based on the active triage intent
    val activeSuggestions = remember(activeIntent, autoTriageAnalysis) {
        if (selectedCategoryOverride == null) {
            autoTriageAnalysis.suggestedReplies
        } else {
            MessageTriageEngine.getSuggestionsForIntent(activeIntent)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_triage")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = Color(0xFF0B111A),
        border = BorderStroke(1.dp, Color(0x2E00E5FF)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_reply_bar")
    ) {
        Column(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        ) {
            // Human Attention Escalation Alert Banner (if flagged by triage engine)
            AnimatedVisibility(
                visible = autoTriageAnalysis.requiresImmediateHumanAttention,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    color = Color(0xFF2A0D12),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF1744)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("human_attention_flag_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(pulseAlpha)
                                .background(Color(0xFFFF1744), CircleShape)
                        )
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Urgent Escalation",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(15.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HUMAN ATTENTION FLAGGED • ${autoTriageAnalysis.urgency.label}",
                                color = Color(0xFFFF5252),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (autoTriageAnalysis.escalationReason != null) {
                                Text(
                                    text = autoTriageAnalysis.escalationReason,
                                    color = Color(0xFFFFCDD2),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Top Status & Triage Category Selector Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Triage Category Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "QUICK REPLY",
                        color = ZamaElectricCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    // Detected Intent Badge
                    Surface(
                        color = Color(activeIntent.colorHex).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, Color(activeIntent.colorHex).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(text = activeIntent.iconEmoji, fontSize = 9.sp)
                            Text(
                                text = activeIntent.displayName.uppercase(),
                                color = Color(activeIntent.colorHex),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Urgency Tag
                    if (selectedCategoryOverride == null) {
                        Surface(
                            color = Color(autoTriageAnalysis.urgency.colorHex).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = autoTriageAnalysis.urgency.label,
                                color = Color(autoTriageAnalysis.urgency.colorHex),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Instruction hint
                Text(
                    text = "Tap to send",
                    color = ZamaChromeMid,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Pills (Allows 1-tap switching between Booking, Inquiry, Complaint, etc.)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto (AI Detected) Pill
                val isAutoSelected = selectedCategoryOverride == null
                Surface(
                    color = if (isAutoSelected) Color(0xFF0F3142) else Color(0xFF141B24),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(
                        0.5.dp,
                        if (isAutoSelected) ZamaElectricCyan else Color(0x338696A0)
                    ),
                    modifier = Modifier
                        .clickable { selectedCategoryOverride = null }
                        .testTag("triage_filter_auto")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isAutoSelected) ZamaElectricCyan else ZamaChromeMid,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "Auto (${autoTriageAnalysis.intent.displayName})",
                            color = if (isAutoSelected) ZamaElectricCyan else ZamaChromeLight,
                            fontSize = 10.sp,
                            fontWeight = if (isAutoSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Explicit intent pills
                listOf(
                    TriageIntent.BOOKING,
                    TriageIntent.INQUIRY,
                    TriageIntent.COMPLAINT,
                    TriageIntent.HUMAN_ESCALATION,
                    TriageIntent.FEEDBACK
                ).forEach { intent ->
                    val isSelected = selectedCategoryOverride == intent
                    Surface(
                        color = if (isSelected) Color(intent.colorHex).copy(alpha = 0.25f) else Color(0xFF141B24),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(
                            0.5.dp,
                            if (isSelected) Color(intent.colorHex) else Color(0x338696A0)
                        ),
                        modifier = Modifier
                            .clickable { selectedCategoryOverride = intent }
                            .testTag("triage_filter_${intent.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = intent.iconEmoji, fontSize = 9.sp)
                            Text(
                                text = intent.displayName,
                                color = if (isSelected) Color(intent.colorHex) else ZamaChromeLight,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Contextual Suggested Response Chips (1-tap replies)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_reply_chips_row"),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = activeSuggestions,
                    key = { it.id }
                ) { suggestion ->
                    QuickReplyChip(
                        suggestion = suggestion,
                        onSend = { onSendQuickReply(suggestion.fullText) }
                    )
                }
            }
        }
    }
}

/**
 * Individual Quick Reply Chip.
 * Formatted with accessible 48dp touch target, high-contrast typography,
 * intent color accents, and instant one-tap dispatch.
 */
@Composable
private fun QuickReplyChip(
    suggestion: QuickReplySuggestion,
    onSend: () -> Unit
) {
    val chipBorderColor = when {
        suggestion.isPrimaryAction -> Color(suggestion.intent.colorHex)
        suggestion.intent == TriageIntent.BOOKING -> ZamaNeonGreen.copy(alpha = 0.6f)
        suggestion.intent == TriageIntent.COMPLAINT -> Color(0xFFFF5252).copy(alpha = 0.7f)
        suggestion.intent == TriageIntent.HUMAN_ESCALATION -> Color(0xFFFF1744).copy(alpha = 0.8f)
        else -> Color(0x4400E5FF)
    }

    val chipBackground = when {
        suggestion.isPrimaryAction -> Color(suggestion.intent.colorHex).copy(alpha = 0.16f)
        else -> Color(0xFF131D28)
    }

    Surface(
        color = chipBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, chipBorderColor),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable { onSend() }
            .semantics {
                contentDescription = "One tap reply: ${suggestion.title}. Content: ${suggestion.fullText}"
            }
            .testTag("quick_reply_chip_${suggestion.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon
            Text(
                text = suggestion.iconEmoji,
                fontSize = 15.sp
            )

            // Title and Snippet Column
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = suggestion.title,
                        color = Color(0xFFFFFFFF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                    if (suggestion.isPrimaryAction) {
                        Surface(
                            color = Color(suggestion.intent.colorHex).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                color = Color(suggestion.intent.colorHex),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = suggestion.fullText,
                    color = ZamaChromeMid,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 240.dp)
                )
            }

            // Quick Send Arrow Icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0x3300E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = ZamaElectricCyan,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
