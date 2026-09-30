package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaBorderGlow
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaVoid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Types of typography animation effects supported by the futuristic modifier.
 */
enum class TypographyEffectType {
    CYBER_GLITCH,       // RGB chromatic aberration, horizontal slice jitter, high-tech scanlines
    LIQUID_MERCURY,     // Sinusoidal wave ribbons, flowing fluid refraction, metallic specular sheen
    QUANTUM_CORRUPTION  // High-frequency kinetic signal decay, data tear, stochastic strobe
}

/**
 * Custom Modifier for applying Cyberpunk Glitch distortion to any typography or layout.
 * Displaces horizontal bands of text and renders RGB chromatic aberration splitting.
 */
fun Modifier.glitchTypography(
    progress: Float,
    intensity: Float = 1.0f,
    isBurstActive: Boolean = false,
    sliceCount: Int = 16,
    cyanColor: Color = Color(0xFF00E5FF),
    magentaColor: Color = Color(0xFFFF0055),
    enableScanlines: Boolean = true,
    enabled: Boolean = true
): Modifier = this.drawWithContent {
    if (!enabled || intensity <= 0f) {
        drawContent()
        return@drawWithContent
    }

    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) {
        drawContent()
        return@drawWithContent
    }

    val effectiveIntensity = if (isBurstActive) intensity * 2.2f else intensity
    val sliceHeight = height / sliceCount.coerceAtLeast(1)

    // Pseudo-random deterministic noise based on progress
    val seed = (progress * 1000).toInt()
    val rng = Random(seed)

    // Base subtle chromatic aberration shifts
    val baseShiftX = (sin(progress * 2f * PI) * 3.5f * effectiveIntensity).toFloat()
    val baseShiftY = (cos(progress * 3f * PI) * 1.2f * effectiveIntensity).toFloat()

    // 1. CYAN ABERRATION PASS (Shifted Left/Up)
    if (effectiveIntensity > 0.3f) {
        translate(left = -baseShiftX - 2.5f * effectiveIntensity, top = -baseShiftY) {
            this@drawWithContent.drawContent()
        }
        // Cyan tint blend
        drawRect(
            color = cyanColor.copy(alpha = (0.28f * effectiveIntensity).coerceIn(0f, 0.6f)),
            blendMode = BlendMode.SrcAtop
        )
    }

    // 2. MAGENTA ABERRATION PASS (Shifted Right/Down)
    if (effectiveIntensity > 0.3f) {
        translate(left = baseShiftX + 2.5f * effectiveIntensity, top = baseShiftY) {
            this@drawWithContent.drawContent()
        }
        // Magenta tint blend
        drawRect(
            color = magentaColor.copy(alpha = (0.28f * effectiveIntensity).coerceIn(0f, 0.6f)),
            blendMode = BlendMode.SrcAtop
        )
    }

    // 3. SLICE JITTER DISPLACEMENT PASS
    for (i in 0 until sliceCount) {
        val top = i * sliceHeight
        val bottom = ((i + 1) * sliceHeight).coerceAtMost(height)

        // Determine if this particular slice experiences glitch displacement
        val sliceRand = rng.nextFloat()
        val shouldGlitch = isBurstActive || (sliceRand < (0.25f * effectiveIntensity.coerceIn(0f, 1f)))

        val sliceOffsetX = if (shouldGlitch) {
            val direction = if (rng.nextBoolean()) 1f else -1f
            val magnitude = (4f + rng.nextFloat() * 18f) * effectiveIntensity
            direction * magnitude
        } else {
            0f
        }

        clipRect(left = -100f, top = top, right = width + 100f, bottom = bottom) {
            translate(left = sliceOffsetX, top = 0f) {
                this@drawWithContent.drawContent()
            }
        }
    }

    // 4. HIGH-TECH SCANLINE RASTER OVERLAY
    if (enableScanlines && effectiveIntensity > 0.2f) {
        val scanlineStep = 3.5f
        var y = 0f
        while (y < height) {
            drawLine(
                color = Color.Black.copy(alpha = (0.35f * effectiveIntensity).coerceIn(0f, 0.7f)),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += scanlineStep
        }
    }

    // 5. LASER BEAM GLITCH STRIPE
    if (isBurstActive) {
        val stripeY = (height * rng.nextFloat()).coerceIn(0f, height - 4f)
        drawRect(
            color = cyanColor.copy(alpha = 0.85f),
            topLeft = Offset(0f, stripeY),
            size = Size(width, 2.5f),
            blendMode = BlendMode.Screen
        )
    }
}

/**
 * Custom Modifier for applying Continuous Liquid Mercury ripple waves and fluid sheer
 * to any typography or layout.
 */
fun Modifier.liquidTypography(
    progress: Float,
    amplitude: Float = 6.0f,
    frequency: Float = 2.5f,
    sheenOffset: Float = 0f,
    sliceCount: Int = 24,
    sheenColor: Color = Color(0xFF00E5FF),
    enabled: Boolean = true
): Modifier = this.drawWithContent {
    if (!enabled) {
        drawContent()
        return@drawWithContent
    }

    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) {
        drawContent()
        return@drawWithContent
    }

    val sliceHeight = height / sliceCount.coerceAtLeast(1)

    // Render sinusoidal fluid wave ribbons
    for (i in 0 until sliceCount) {
        val top = i * sliceHeight
        val bottom = ((i + 1) * sliceHeight).coerceAtMost(height)
        val normalizedY = top / height

        // Dual harmonic liquid wave formula
        val wave1 = sin((normalizedY * frequency * 2f * PI + progress * 2f * PI).toFloat())
        val wave2 = cos((normalizedY * frequency * 1.5f * PI - progress * 1.2f * PI).toFloat())
        val dx = (wave1 * 0.7f + wave2 * 0.3f) * amplitude
        val dy = (wave2 * 0.4f) * (amplitude * 0.3f)

        clipRect(left = -60f, top = top, right = width + 60f, bottom = bottom) {
            translate(left = dx, top = dy) {
                this@drawWithContent.drawContent()
            }
        }
    }

    // Sweeping Liquid Specular Sheen
    val sheenX = (sheenOffset % 1.5f - 0.25f) * width
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                sheenColor.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.75f),
                sheenColor.copy(alpha = 0.45f),
                Color.Transparent
            ),
            startX = sheenX - width * 0.2f,
            endX = sheenX + width * 0.2f
        ),
        blendMode = BlendMode.SrcAtop
    )
}

/**
 * Unified Custom Modifier applying selected futuristic typography animation.
 */
fun Modifier.futuristicTypographyEffect(
    effectType: TypographyEffectType,
    progress: Float,
    intensity: Float = 1.0f,
    isBurstActive: Boolean = false,
    sheenOffset: Float = 0f,
    enabled: Boolean = true
): Modifier = when (effectType) {
    TypographyEffectType.CYBER_GLITCH -> this.glitchTypography(
        progress = progress,
        intensity = intensity,
        isBurstActive = isBurstActive,
        enabled = enabled
    )
    TypographyEffectType.LIQUID_MERCURY -> this.liquidTypography(
        progress = progress,
        amplitude = 7f * intensity,
        sheenOffset = sheenOffset,
        enabled = enabled
    )
    TypographyEffectType.QUANTUM_CORRUPTION -> this.glitchTypography(
        progress = progress,
        intensity = intensity * 1.6f,
        isBurstActive = isBurstActive,
        sliceCount = 28,
        cyanColor = Color(0xFF00E676), // Matrix Green
        magentaColor = Color(0xFF2979FF),
        enableScanlines = true,
        enabled = enabled
    )
}

/**
 * Production-ready Futuristic Text component integrating the custom glitch and liquid modifiers.
 */
@Composable
fun FuturisticGlitchText(
    text: String,
    modifier: Modifier = Modifier,
    effectType: TypographyEffectType = TypographyEffectType.CYBER_GLITCH,
    intensity: Float = 1.0f,
    interactive: Boolean = true,
    style: TextStyle = TextStyle.Default,
    color: Color = ZamaChromeLight,
    fontSize: TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = FontFamily.Monospace,
    letterSpacing: TextUnit = 1.sp,
    textAlign: TextAlign = TextAlign.Start,
    lineHeight: TextUnit = TextUnit.Unspecified,
    onClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var isManualBurst by remember { mutableStateOf(false) }

    // Continuous Animation Loops
    val infiniteTransition = rememberInfiniteTransition(label = "futuristic_typo_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_progress"
    )
    val liquidSheen by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_sheen"
    )

    // Autonomous Periodic Glitch Spikes
    var periodicSpike by remember { mutableStateOf(false) }
    LaunchedEffect(effectType) {
        while (true) {
            delay((2000L..4500L).random())
            periodicSpike = true
            delay((120L..280L).random())
            periodicSpike = false
        }
    }

    val isBursting = isManualBurst || periodicSpike

    Box(
        modifier = modifier
            .then(
                if (interactive) {
                    Modifier.clickable {
                        onClick?.invoke()
                        coroutineScope.launch {
                            isManualBurst = true
                            delay(350)
                            isManualBurst = false
                        }
                    }
                } else Modifier
            )
            .futuristicTypographyEffect(
                effectType = effectType,
                progress = animProgress,
                intensity = intensity,
                isBurstActive = isBursting,
                sheenOffset = liquidSheen,
                enabled = true
            )
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textAlign = textAlign,
            lineHeight = lineHeight,
            style = style
        )
    }
}

/**
 * Interactive Lab Playground Component showcasing the custom Glitch & Liquid Typography modifiers.
 */
@Composable
fun FuturisticTypographyShowcase(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var activeEffect by remember { mutableStateOf(TypographyEffectType.CYBER_GLITCH) }
    var effectIntensity by remember { mutableFloatStateOf(1.0f) }
    var customText by remember { mutableStateOf("ZAMA AI / NEURAL SOVEREIGNTY") }
    var isManualBursting by remember { mutableStateOf(false) }

    val presetPhrases = listOf(
        "ZAMA AI / NEURAL SOVEREIGNTY",
        "AUTONOMOUS WHATSAPP AGENT",
        "HOMOMORPHIC ENCRYPTED DATA",
        "LIQUID MERCURY INTELLIGENCE"
    )

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
            .padding(20.dp)
            .testTag("futuristic_typography_showcase")
    ) {
        // Section Header Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0x3300E5FF),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CUSTOM MODIFIER / TYPOGRAPHY LAB",
                        color = ZamaElectricCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Surface(
                color = Color(0x2200E676),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E676))
            ) {
                Text(
                    text = "REAL-TIME SHADER",
                    color = ZamaNeonGreen,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "GLITCH & LIQUID TYPOGRAPHY",
            color = ZamaChromeLight,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Custom Compose Modifier utilizing canvas clipping ribbons, RGB chromatic split passes, scanlines, and sinusoidal fluid wave sheer.",
            color = ZamaChromeMid,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Large Typography Display Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF07090F))
                .border(1.dp, ZamaBorderGlow, RoundedCornerShape(16.dp))
                .padding(vertical = 36.dp, horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            FuturisticGlitchText(
                text = customText,
                effectType = activeEffect,
                intensity = effectIntensity,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("futuristic_glitch_text_target")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Effect Mode Switcher Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF10141D))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val effects = listOf(
                Triple("Cyber Glitch", TypographyEffectType.CYBER_GLITCH, Icons.Default.Bolt),
                Triple("Liquid Mercury", TypographyEffectType.LIQUID_MERCURY, Icons.Default.WaterDrop),
                Triple("Quantum Decay", TypographyEffectType.QUANTUM_CORRUPTION, Icons.Default.InvertColors)
            )

            effects.forEach { (title, effect, icon) ->
                val isSelected = activeEffect == effect
                Surface(
                    onClick = { activeEffect = effect },
                    color = if (isSelected) Color(0x3300E5FF) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ZamaElectricCyan) else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("effect_tab_${effect.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) ZamaElectricCyan else ZamaChromeMid,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = title,
                            color = if (isSelected) ZamaElectricCyan else ZamaChromeMid,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Glitch Intensity Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EFFECT INTENSITY",
                color = ZamaChromeLight,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${(effectIntensity * 100).toInt()}%",
                color = ZamaElectricCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = effectIntensity,
            onValueChange = { effectIntensity = it },
            valueRange = 0.2f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = ZamaElectricCyan,
                activeTrackColor = ZamaElectricCyan,
                inactiveTrackColor = Color(0xFF1E2433)
            ),
            modifier = Modifier.testTag("typo_intensity_slider")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Preset Words Quick Selector Chips
        Text(
            text = "PRESET SLOGANS",
            color = ZamaGraphite,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetPhrases.take(2).forEach { phrase ->
                val isSelected = customText == phrase
                Surface(
                    onClick = { customText = phrase },
                    color = if (isSelected) Color(0x2200E5FF) else Color(0xFF121622),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ZamaElectricCyan else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = phrase.take(15) + "...",
                        color = if (isSelected) ZamaElectricCyan else ZamaChromeMid,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetPhrases.drop(2).forEach { phrase ->
                val isSelected = customText == phrase
                Surface(
                    onClick = { customText = phrase },
                    color = if (isSelected) Color(0x2200E5FF) else Color(0xFF121622),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ZamaElectricCyan else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = phrase.take(15) + "...",
                        color = if (isSelected) ZamaElectricCyan else ZamaChromeMid,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High-Voltage Burst Trigger Button
        ElevatedButton(
            onClick = {
                coroutineScope.launch {
                    isManualBursting = true
                    delay(450)
                    isManualBursting = false
                }
            },
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFF1A2233),
                contentColor = ZamaElectricCyan
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ZamaElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .testTag("trigger_burst_glitch_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = ZamaElectricCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TRIGGER HIGH-VOLTAGE GLITCH BURST",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
