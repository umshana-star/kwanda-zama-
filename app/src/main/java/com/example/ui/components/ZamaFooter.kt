package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaVoid
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ZamaFooter(
    modifier: Modifier = Modifier,
    onNavigate: ((String) -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "distant_z_orbit")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "distant_orbit"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF040609),
                        ZamaVoid
                    )
                )
            )
            .border(1.dp, ZamaBorder, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Distant Floating 3D Z in the Void
        Box(
            modifier = Modifier
                .size(140.dp)
                .clickable(enabled = onNavigate != null) { onNavigate?.invoke("hero") }
                .testTag("distant_z_box"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Deep starfield halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ZamaElectricCyan.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = 65f
                    ),
                    radius = 65f,
                    center = Offset(cx, cy)
                )

                // Render tiny distant rotating Z
                renderDistantZ(cx, cy, orbitAngle)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Enormous Typography
        Text(
            text = "ZAMA",
            color = ZamaChromeLight,
            fontSize = 52.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 10.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "INTELLIGENCE WITHOUT COMPROMISE.",
            color = ZamaElectricCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 1.dp)

        Spacer(modifier = Modifier.height(16.dp))

        // Footer links & legal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORK",
                    color = ZamaElectricCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clickable { onNavigate?.invoke("agents") }
                        .padding(vertical = 4.dp)
                        .testTag("footer_link_work")
                )
                Text(text = "//", color = ZamaGraphite, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "ARCHITECTURE",
                    color = ZamaElectricCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clickable { onNavigate?.invoke("chat") }
                        .padding(vertical = 4.dp)
                        .testTag("footer_link_architecture")
                )
                Text(text = "//", color = ZamaGraphite, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "ENGINE",
                    color = ZamaElectricCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clickable { onNavigate?.invoke("3d") }
                        .padding(vertical = 4.dp)
                        .testTag("footer_link_engine")
                )
            }

            Text(
                text = "© ZAMA AI 2026 • TOP ↑",
                color = ZamaChromeMid,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable { onNavigate?.invoke("hero") }
                    .padding(vertical = 4.dp)
                    .testTag("footer_link_top")
            )
        }
    }
}

private fun DrawScope.renderDistantZ(cx: Float, cy: Float, angleDeg: Float) {
    val rad = (angleDeg * PI / 180f).toFloat()
    val cosA = cos(rad)
    val sinA = sin(rad)

    val scale = 22f
    val depth = 8f

    fun proj(x: Float, y: Float, z: Float): Offset {
        val xRot = x * cosA + z * sinA
        return Offset(cx + xRot, cy + y)
    }

    val p1 = proj(-scale, -scale, depth)
    val p2 = proj(scale, -scale, depth)
    val p3 = proj(-scale, scale, depth)
    val p4 = proj(scale, scale, depth)

    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
    }

    drawPath(
        path = path,
        color = Color(0xFFC0C7D5).copy(alpha = 0.8f),
        style = Stroke(width = 2.2f)
    )
}
