package com.example.ui.components

import androidx.compose.animation.Crossfade
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MaterialType
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaVoid
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveTypographicArt(
    modifier: Modifier = Modifier
) {
    var selectedMaterial by remember { mutableStateOf(MaterialType.TRUST) }

    val infiniteTransition = rememberInfiniteTransition(label = "material_sweep")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090C12),
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
                    text = "SECTION 06 // MATERIAL TYPOGRAPHY",
                    color = ZamaElectricCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "FOUR SCULPTURAL ARCHETYPES",
                    color = ZamaChromeLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Surface(
                color = Color(selectedMaterial.primaryColorHex).copy(alpha = 0.4f),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(selectedMaterial.accentColorHex).copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = selectedMaterial.materialName.uppercase(),
                    color = Color(selectedMaterial.accentColorHex),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4 Word Tabs: TRUST, PRIVACY, COMPUTE, FREEDOM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaterialType.entries.forEach { material ->
                val isSelected = material == selectedMaterial
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedMaterial = material }
                        .testTag("material_tab_${material.name.lowercase()}"),
                    color = if (isSelected) Color(0xFF161C26) else Color(0xFF0C0E14),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(material.accentColorHex) else Color(0x33FFFFFF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = material.title,
                            color = if (isSelected) Color(material.accentColorHex) else ZamaChromeMid,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Material Visualizer Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF040608))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = selectedMaterial,
                animationSpec = tween(500),
                label = "material_canvas_crossfade"
            ) { target ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    renderMaterialCanvas(
                        target = target,
                        sweep = sweepProgress
                    )
                }
            }

            // Word Title Displayed with sculptural presence
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = selectedMaterial.title,
                    color = Color(selectedMaterial.accentColorHex),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = selectedMaterial.subtitle.uppercase(),
                    color = ZamaChromeLight.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Material Architecture Spec Card
        Surface(
            color = Color(0xFF0C0F17),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ZamaBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MATERIAL SHADER",
                        color = ZamaGraphite,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = selectedMaterial.reflectionType,
                        color = Color(selectedMaterial.accentColorHex),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = selectedMaterial.description,
                    color = ZamaChromeMid,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun DrawScope.renderMaterialCanvas(
    target: MaterialType,
    sweep: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    when (target) {
        MaterialType.TRUST -> {
            // Polished chrome mirror reflections
            val sweepX = size.width * sweep
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFE2E8F0).copy(alpha = 0.6f),
                        Color.White,
                        Color.Transparent
                    ),
                    start = Offset(sweepX - 80f, 0f),
                    end = Offset(sweepX + 80f, size.height)
                ),
                start = Offset(sweepX - 80f, 0f),
                end = Offset(sweepX + 80f, size.height),
                strokeWidth = 40f
            )

            // Metallic isometric grid lines
            for (i in -4..4) {
                drawLine(
                    color = Color(0x18FFFFFF),
                    start = Offset(cx + i * 45f, 0f),
                    end = Offset(cx + i * 45f + 60f, size.height),
                    strokeWidth = 1f
                )
            }
        }

        MaterialType.PRIVACY -> {
            // Refractive transparent glass caustics
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3338BDF8),
                        Color(0x110284C7),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = 160f
                ),
                radius = 160f,
                center = Offset(cx, cy)
            )

            // Prismatic refractive rings
            for (r in listOf(60f, 100f, 140f)) {
                drawCircle(
                    color = Color(0x3338BDF8),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.2f)
                )
            }
        }

        MaterialType.COMPUTE -> {
            // Dark metallic micro-circuitry
            for (y in 20..size.height.toInt() step 30) {
                drawLine(
                    color = Color(0x1A818CF8),
                    start = Offset(0f, y.toFloat()),
                    end = Offset(size.width, y.toFloat()),
                    strokeWidth = 1f
                )
            }

            // Logic nodes pulsing
            val sweepOffset = (sweep * size.width) % size.width
            drawCircle(
                color = Color(0xFF818CF8),
                radius = 5f,
                center = Offset(sweepOffset, cy)
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 4f,
                center = Offset(size.width - sweepOffset, cy + 30f)
            )
        }

        MaterialType.FREEDOM -> {
            // Volumetric light diffusion & green/white particle rays
            for (i in 0 until 12) {
                val angle = (i * 30f) * (3.14159f / 180f)
                val len = 120f + (sin(sweep * 3f + i) * 30f)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x8834D399),
                            Color.Transparent
                        ),
                        start = Offset(cx, cy),
                        end = Offset(cx + cos(angle) * len, cy + sin(angle) * len)
                    ),
                    start = Offset(cx, cy),
                    end = Offset(cx + cos(angle) * len, cy + sin(angle) * len),
                    strokeWidth = 2f
                )
            }
        }
    }
}
