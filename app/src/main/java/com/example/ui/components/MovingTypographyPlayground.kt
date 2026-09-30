package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaVoid
import kotlin.math.roundToInt

@Composable
fun MovingTypographyPlayground(
    modifier: Modifier = Modifier
) {
    // Stage from 0.0 (discrete words) to 1.0 (merged into ZAMA)
    var morphProgress by remember { mutableFloatStateOf(0.0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = morphProgress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "typography_morph"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF07090E),
                        ZamaVoid
                    )
                )
            )
            .border(1.dp, ZamaBorder, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SECTION 04 // TYPOGRAPHY AS ARCHITECTURE",
                    color = ZamaElectricCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "MOVING TYPOGRAPHY",
                    color = ZamaChromeLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Surface(
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = if (animatedProgress > 0.8f) "STATE: MERGED ZAMA" else "STATE: DECONSTRUCTING",
                    color = if (animatedProgress > 0.8f) ZamaElectricCyan else ZamaChromeMid,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Kinetic Typography Display Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF040507))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(16.dp)
                .testTag("typography_playground_box"),
            contentAlignment = Alignment.Center
        ) {
            if (animatedProgress > 0.75f) {
                // Merged MONOLITH "ZAMA"
                val mergeAlpha = ((animatedProgress - 0.75f) / 0.25f).coerceIn(0f, 1f)
                val mergeScale = 0.8f + (mergeAlpha * 0.2f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        alpha = mergeAlpha
                        scaleX = mergeScale
                        scaleY = mergeScale
                    }
                ) {
                    Text(
                        text = "ZAMA",
                        color = ZamaChromeLight,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AUTONOMOUS INTELLIGENCE CORE",
                        color = ZamaElectricCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp
                    )
                }
            } else {
                // 3 Separate Transforming Words: PRIVATE, POWERFUL, OPEN
                val remainingAlpha = (1f - (animatedProgress / 0.75f)).coerceIn(0f, 1f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = remainingAlpha },
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. PRIVATE → stretches horizontally
                    val stretchFactor = 1.0f + (animatedProgress * 0.9f)
                    Text(
                        text = "PRIVATE",
                        color = ZamaChromeLight,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (2 + animatedProgress * 12).sp,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = stretchFactor
                            }
                    )

                    // 2. POWERFUL → rotates in 3D
                    val rot3D = animatedProgress * 65f
                    Text(
                        text = "POWERFUL",
                        color = Color(0xFFC0C7D5),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        modifier = Modifier
                            .graphicsLayer {
                                rotationX = rot3D
                                rotationY = -rot3D * 0.7f
                                cameraDistance = 12f
                            }
                    )

                    // 3. OPEN → splits apart into individual characters
                    val splitOffset = animatedProgress * 30f
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "O",
                            color = ZamaElectricCyan,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.offset { IntOffset((-splitOffset * 2.5f).roundToInt(), 0) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "P",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.offset { IntOffset((-splitOffset * 0.8f).roundToInt(), 0) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "E",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.offset { IntOffset((splitOffset * 0.8f).roundToInt(), 0) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "N",
                            color = ZamaElectricCyan,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.offset { IntOffset((splitOffset * 2.5f).roundToInt(), 0) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive scrub & trigger buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KINETIC MORPHING",
                color = ZamaGraphite,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "${(animatedProgress * 100).toInt()}% KINETIC FLOW",
                color = ZamaChromeMid,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Slider(
            value = morphProgress,
            onValueChange = { morphProgress = it },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = ZamaElectricCyan,
                activeTrackColor = ZamaElectricCyan,
                inactiveTrackColor = Color(0xFF1E2430)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { morphProgress = 0.0f },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "DECONSTRUCT",
                    color = ZamaChromeMid,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            ElevatedButton(
                onClick = { morphProgress = 1.0f },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color(0xFF162030),
                    contentColor = ZamaElectricCyan
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "MERGE INTO ZAMA",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
