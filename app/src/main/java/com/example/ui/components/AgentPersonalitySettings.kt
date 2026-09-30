package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkGrey
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaPurple
import com.example.ui.theme.ZamaVoid
import com.example.util.HapticFeedbackManager
import kotlin.math.roundToInt

/**
 * Model representing the personality and prompt tuning parameters
 * sent to Gemini for the WhatsApp business agent.
 */
data class AgentPersonalityConfig(
    val creativity: Float = 0.65f, // 0.0: strict/deterministic, 1.0: highly creative
    val formality: Float = 0.40f,  // 0.0: casual/street, 1.0: corporate/luxury formal
    val verbosity: Float = 0.50f   // 0.0: ultra-concise, 1.0: descriptive/detailed
) {
    fun toSystemInstruction(): String {
        val creativityText = when {
            creativity < 0.35f -> "Temperature 0.2: Provide factual, deterministic salon slot confirmations without embellishment."
            creativity < 0.70f -> "Temperature 0.7: Maintain warm, natural conversational flow with polite styling suggestions."
            else -> "Temperature 0.95: Be exuberant, creative, and enthusiastic with emojis and stylish salon flair."
        }

        val formalityText = when {
            formality < 0.35f -> "Tone: Casual, upbeat, modern South African WhatsApp slang & high-vibe salon culture."
            formality < 0.70f -> "Tone: Warm, courteous, and highly professional concierge."
            else -> "Tone: Ultra-luxury, executive concierge styling with refined etiquette."
        }

        val verbosityText = when {
            verbosity < 0.35f -> "Length: Ultra-concise, 1-2 sentence quick confirmations optimized for fast mobile reading."
            verbosity < 0.70f -> "Length: Balanced 2-3 sentences with clear price, timing, and next action."
            else -> "Length: Comprehensive details with breakdown of hair care treatments, aftercare tips, and timing."
        }

        return """
            [ZAMA AGENT PERSONALITY MATRIX]
            $creativityText
            $formalityText
            $verbosityText
            Business Context: GLAM Hair & Beauty Studio, Johannesburg.
        """.trimIndent()
    }

    val creativityLabel: String
        get() = when {
            creativity < 0.35f -> "Precise / Direct"
            creativity < 0.70f -> "Natural / Balanced"
            else -> "Creative & Expressive"
        }

    val formalityLabel: String
        get() = when {
            formality < 0.35f -> "Casual & Upbeat"
            formality < 0.70f -> "Warm & Courteous"
            else -> "Luxury VIP Formal"
        }

    val verbosityLabel: String
        get() = when {
            verbosity < 0.35f -> "Quick Bullet"
            verbosity < 0.70f -> "Balanced"
            else -> "Detailed & Elaborate"
        }
}

enum class PersonalityPreset(
    val title: String,
    val subtitle: String,
    val config: AgentPersonalityConfig
) {
    BALANCED_CONCIERGE(
        title = "Balanced Concierge",
        subtitle = "Optimal default for day-to-day salon bookings",
        config = AgentPersonalityConfig(creativity = 0.50f, formality = 0.50f, verbosity = 0.50f)
    ),
    HIGH_ENERGY_SALES(
        title = "High-Energy Sales",
        subtitle = "Enthusiastic, upsells treatments with flair",
        config = AgentPersonalityConfig(creativity = 0.85f, formality = 0.20f, verbosity = 0.70f)
    ),
    FAST_BOOKER(
        title = "Concise Fast Booker",
        subtitle = "Quick, direct confirmations for busy customers",
        config = AgentPersonalityConfig(creativity = 0.20f, formality = 0.60f, verbosity = 0.25f)
    ),
    LUXURY_VIP(
        title = "Luxury VIP Host",
        subtitle = "Polished, elite tone for premium clientele",
        config = AgentPersonalityConfig(creativity = 0.60f, formality = 0.90f, verbosity = 0.80f)
    )
}

/**
 * Settings card with interactive sliders, tactile haptic feedback,
 * preset buttons, and real-time Gemini prompt reflection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentPersonalitySettingsCard(
    personality: AgentPersonalityConfig,
    onPersonalityChange: (AgentPersonalityConfig) -> Unit,
    hapticManager: HapticFeedbackManager,
    modifier: Modifier = Modifier
) {
    var showPromptPreview by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0E17))
            .border(1.dp, ZamaBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("personality_settings_card")
    ) {
        // Card Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x227C4DFF), CircleShape)
                        .border(1.dp, Color(0x447C4DFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Agent Personality Settings",
                        tint = ZamaPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AGENT PERSONALITY & TONE",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Real-time Gemini prompt tuning with haptics",
                        color = ZamaChromeMid,
                        fontSize = 11.sp
                    )
                }
            }

            // Haptic Feedback Indicator badge
            Surface(
                color = Color(0x2200E676),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, Color(0x4400E676))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Haptic feedback enabled",
                        tint = ZamaNeonGreen,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HAPTIC ON",
                        color = ZamaNeonGreen,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Chips
        Text(
            text = "ONE-TAP PERSONALITY PRESETS",
            color = ZamaElectricCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PersonalityPreset.entries.forEach { preset ->
                val isSelected = (personality.creativity - preset.config.creativity).let { kotlin.math.abs(it) < 0.06f } &&
                        (personality.formality - preset.config.formality).let { kotlin.math.abs(it) < 0.06f } &&
                        (personality.verbosity - preset.config.verbosity).let { kotlin.math.abs(it) < 0.06f }

                Surface(
                    color = if (isSelected) Color(0x3300E5FF) else Color(0xFF141923),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) ZamaElectricCyan else Color(0xFF2A3442)
                    ),
                    modifier = Modifier
                        .clickable {
                            hapticManager.presetSelected()
                            onPersonalityChange(preset.config)
                        }
                        .testTag("preset_${preset.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ZamaElectricCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = preset.title,
                            color = if (isSelected) Color.White else ZamaChromeLight,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SLIDER 1: Creativity / Temperature
        PersonalitySliderRow(
            title = "CREATIVITY / TEMPERATURE",
            currentValue = personality.creativity,
            valueLabel = personality.creativityLabel,
            percentage = "${(personality.creativity * 100).roundToInt()}%",
            accentColor = ZamaElectricCyan,
            icon = Icons.Default.AutoAwesome,
            testTag = "slider_creativity",
            hapticManager = hapticManager,
            onValueChange = { newVal ->
                onPersonalityChange(personality.copy(creativity = newVal))
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // SLIDER 2: Tone / Formality
        PersonalitySliderRow(
            title = "TONE / FORMALITY",
            currentValue = personality.formality,
            valueLabel = personality.formalityLabel,
            percentage = "${(personality.formality * 100).roundToInt()}%",
            accentColor = ZamaPurple,
            icon = Icons.Default.Psychology,
            testTag = "slider_formality",
            hapticManager = hapticManager,
            onValueChange = { newVal ->
                onPersonalityChange(personality.copy(formality = newVal))
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // SLIDER 3: Response Length / Verbosity
        PersonalitySliderRow(
            title = "RESPONSE VERBOSITY",
            currentValue = personality.verbosity,
            valueLabel = personality.verbosityLabel,
            percentage = "${(personality.verbosity * 100).roundToInt()}%",
            accentColor = ZamaNeonGreen,
            icon = Icons.Default.GraphicEq,
            testTag = "slider_verbosity",
            hapticManager = hapticManager,
            onValueChange = { newVal ->
                onPersonalityChange(personality.copy(verbosity = newVal))
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Toggle Gemini System Prompt Inspector
        Surface(
            color = Color(0xFF10141E),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF202735)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    hapticManager.click()
                    showPromptPreview = !showPromptPreview
                }
                .testTag("btn_toggle_prompt_preview")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Prompt Inspection",
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showPromptPreview) "HIDE GEMINI SYSTEM PROMPT" else "VIEW ACTIVE GEMINI SYSTEM PROMPT",
                        color = ZamaChromeLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (showPromptPreview) "▲" else "▼",
                    color = ZamaChromeMid,
                    fontSize = 10.sp
                )
            }
        }

        AnimatedVisibility(visible = showPromptPreview) {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF05070B), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = personality.toSystemInstruction(),
                    color = Color(0xFF80D8FF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * Reusable personality slider row with step-crossing tactile haptic feedback.
 */
@Composable
private fun PersonalitySliderRow(
    title: String,
    currentValue: Float,
    valueLabel: String,
    percentage: String,
    accentColor: Color,
    icon: ImageVector,
    testTag: String,
    hapticManager: HapticFeedbackManager,
    onValueChange: (Float) -> Unit
) {
    // Keep track of the last haptic tick step (in 5% increments)
    var lastTickStep by remember { mutableFloatStateOf((currentValue * 20).roundToInt().toFloat()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = ZamaChromeMid,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = valueLabel,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = percentage,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Slider(
            value = currentValue,
            onValueChange = { newValue ->
                val step = (newValue * 20).roundToInt().toFloat()
                if (step != lastTickStep) {
                    lastTickStep = step
                    hapticManager.sliderTick()
                }
                onValueChange(newValue)
            },
            onValueChangeFinished = {
                hapticManager.sliderValueConfirmed()
            },
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF1E2633)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
