package com.example.autoreply

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import kotlinx.coroutines.launch

/**
 * Machine Learning-Powered Auto-Reply Bar.
 * Analyzes conversation history patterns to synthesize personalized, ready-to-dispatch
 * responses with predicted confidence metrics and one-tap actions.
 */
@Composable
fun AutoReplyBar(
    conversationHistory: List<ChatMessage>,
    onSendAutoReply: (String) -> Unit,
    onInsertIntoInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var isGeneratingAi by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 1. Extract context variables from multi-turn history
    val extractedContext = remember(conversationHistory) {
        ConversationPatternExtractor.extractContext(conversationHistory)
    }

    // 2. Synthesize pattern-based suggestions
    var suggestions by remember(conversationHistory) {
        mutableStateOf(AutoReplySuggestionEngine.generatePatternSuggestions(conversationHistory))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_brain")
    val brainPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brain_pulse"
    )

    Surface(
        color = Color(0xFF070E18),
        border = BorderStroke(1.dp, Color(0x3D00E5FF)),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("auto_reply_bar")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            // Header Bar with ML status, context summary, and controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing Brain / Robot Icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x2E00E5FF))
                        .border(1.dp, ZamaElectricCyan.copy(alpha = brainPulseAlpha), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "ML Auto-Reply Engine",
                        tint = ZamaElectricCyan,
                        modifier = Modifier
                            .size(15.dp)
                            .alpha(brainPulseAlpha)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Title + Active Pattern Stage
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ML AUTO-REPLY",
                            color = ZamaElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Confidence pill
                        val avgConfidence = remember(suggestions) {
                            if (suggestions.isNotEmpty()) suggestions.map { it.confidenceScore }.average().toFloat() else 0.95f
                        }
                        Surface(
                            color = ZamaNeonGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(0.5.dp, ZamaNeonGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${(avgConfidence * 100).toInt()}% CONFIDENCE",
                                color = ZamaNeonGreen,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Context clues: Name, Service, Time
                    val contextClue = buildString {
                        if (!extractedContext.customerName.isNullOrBlank()) {
                            append("👤 ${extractedContext.customerName} • ")
                        }
                        if (!extractedContext.detectedService.isNullOrBlank()) {
                            append("✂️ ${extractedContext.detectedService} • ")
                        }
                        if (!extractedContext.detectedDayOrTime.isNullOrBlank()) {
                            append("📅 ${extractedContext.detectedDayOrTime} • ")
                        }
                        append("🏷️ ${extractedContext.currentStage.displayName}")
                    }
                    Text(
                        text = contextClue,
                        color = ZamaChromeMid,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // AI Neural Regeneration Button
                Surface(
                    color = if (isGeneratingAi) Color(0x3300E5FF) else Color(0x1A00E5FF),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color(0x6600E5FF)),
                    modifier = Modifier
                        .clickable(enabled = !isGeneratingAi) {
                            isGeneratingAi = true
                            coroutineScope.launch {
                                val aiResults = AutoReplySuggestionEngine.generateAiPersonalizedReplies(conversationHistory)
                                suggestions = aiResults
                                isGeneratingAi = false
                            }
                        }
                        .testTag("btn_regenerate_ml_replies")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isGeneratingAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                color = ZamaElectricCyan,
                                strokeWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Regenerate",
                                tint = ZamaElectricCyan,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isGeneratingAi) "Synthesizing..." else "Gemini Neural",
                            color = ZamaElectricCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Expand / Collapse Toggle
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse Auto-Reply" else "Expand Auto-Reply",
                        tint = ZamaChromeLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expandable suggestions carousel
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions, key = { it.id }) { suggestion ->
                        AutoReplyCard(
                            suggestion = suggestion,
                            onSend = { onSendAutoReply(suggestion.suggestedText) },
                            onInsert = { onInsertIntoInput(suggestion.suggestedText) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Card for an individual Auto-Reply recommendation with one-tap SEND or INSERT.
 */
@Composable
private fun AutoReplyCard(
    suggestion: AutoReplySuggestion,
    onSend: () -> Unit,
    onInsert: () -> Unit
) {
    val borderColor = remember(suggestion.confidenceScore) {
        when {
            suggestion.confidenceScore >= 0.95f -> ZamaNeonGreen.copy(alpha = 0.7f)
            suggestion.confidenceScore >= 0.90f -> ZamaElectricCyan.copy(alpha = 0.6f)
            else -> ZamaBorder
        }
    }

    Surface(
        color = Color(0xFF0F1722),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .widthIn(min = 230.dp, max = 290.dp)
            .testTag("auto_reply_card_${suggestion.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Top Row: Category label & Match Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = suggestion.shortLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    color = if (suggestion.confidenceScore >= 0.95f) Color(0x3325D366) else Color(0x3300E5FF),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(0.5.dp, if (suggestion.confidenceScore >= 0.95f) ZamaNeonGreen else ZamaElectricCyan)
                ) {
                    Text(
                        text = "${suggestion.confidencePercentage}% MATCH",
                        color = if (suggestion.confidenceScore >= 0.95f) ZamaNeonGreen else ZamaElectricCyan,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Pattern rationale subtitle
            Text(
                text = "💡 ${suggestion.patternRationale}",
                color = ZamaChromeMid,
                fontSize = 8.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp, bottom = 4.dp)
            )

            // Message text preview
            Surface(
                color = Color(0xFF0B1017),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, Color(0x26FFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = suggestion.suggestedText,
                    color = ZamaChromeLight,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(7.dp)
                )
            }

            // Action Buttons: Send Now vs Insert / Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Insert into text box
                Surface(
                    color = Color(0xFF1B2433),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, ZamaBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onInsert() }
                        .testTag("btn_insert_reply_${suggestion.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit or customize",
                            tint = ZamaChromeLight,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Insert",
                            color = ZamaChromeLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // One-tap Instant Send
                Surface(
                    color = Color(0xFF003B46),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, ZamaElectricCyan),
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable { onSend() }
                        .testTag("btn_send_reply_${suggestion.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Reply Now",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Send Now",
                            color = ZamaElectricCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
