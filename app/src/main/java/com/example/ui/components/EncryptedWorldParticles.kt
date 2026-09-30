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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaVoid
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class CosmicParticle(
    var x: Float,
    var y: Float,
    val originX: Float,
    val originY: Float,
    val size: Float,
    val baseAlpha: Float,
    val speed: Float,
    val phase: Float,
    val isElectric: Boolean
)

@Composable
fun EncryptedWorldParticles(
    modifier: Modifier = Modifier
) {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var isRepelling by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_field")
    val cosmicTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cosmic_time"
    )

    // Grid of particles forming latent constellation
    val particles = remember {
        val list = mutableListOf<CosmicParticle>()
        val rnd = java.util.Random(77)
        for (i in 0 until 110) {
            val ox = rnd.nextFloat()
            val oy = rnd.nextFloat()
            list.add(
                CosmicParticle(
                    x = ox,
                    y = oy,
                    originX = ox,
                    originY = oy,
                    size = 1.8f + rnd.nextFloat() * 3.5f,
                    baseAlpha = 0.3f + rnd.nextFloat() * 0.7f,
                    speed = 0.5f + rnd.nextFloat() * 1.5f,
                    phase = rnd.nextFloat() * 6.28f,
                    isElectric = rnd.nextFloat() > 0.65f
                )
            )
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF04060A),
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
                    text = "SECTION 05 // ENCRYPTED WORLD",
                    color = ZamaElectricCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "MAGNETIC QUANTUM UNIVERSE",
                    color = ZamaChromeLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Surface(
                color = if (isRepelling) Color(0x33FF5252) else Color(0x3300E5FF),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isRepelling) Color(0x66FF5252) else Color(0x6600E5FF)
                )
            ) {
                Text(
                    text = if (isRepelling) "MODE: REPEL" else "MODE: ATTRACT",
                    color = if (isRepelling) Color(0xFFFF5252) else ZamaElectricCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Particle Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF020306))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> touchPos = offset },
                        onDragEnd = { touchPos = null },
                        onDragCancel = { touchPos = null },
                        onDrag = { change, _ ->
                            change.consume()
                            touchPos = change.position
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            touchPos = offset
                            tryAwaitRelease()
                            touchPos = null
                        }
                    )
                }
                .testTag("encrypted_particle_field"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Render dynamic magnetic particle physics
                renderMagneticParticles(
                    w = w,
                    h = h,
                    particles = particles,
                    touch = touchPos,
                    isRepel = isRepelling,
                    time = cosmicTime
                )
            }

            // Central Sacred Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "YOUR DATA.",
                    color = ZamaChromeLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "YOUR INTELLIGENCE.",
                    color = ZamaElectricCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "YOUR CONTROL.",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Interactive hint
            Text(
                text = "TOUCH TO ACTIVATE MAGNETIC FIELD",
                color = ZamaGraphite,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggle Magnet Mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { isRepelling = !isRepelling },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isRepelling) Color(0xFFFF5252) else ZamaElectricCyan
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = "Toggle Polarity",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRepelling) "POLARITY: REPEL" else "POLARITY: ATTRACT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun DrawScope.renderMagneticParticles(
    w: Float,
    h: Float,
    particles: List<CosmicParticle>,
    touch: Offset?,
    isRepel: Boolean,
    time: Float
) {
    val fieldRadius = 180f

    // Draw magnetic cursor beacon if active
    if (touch != null) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    if (isRepel) Color(0x44FF5252) else Color(0x4400E5FF),
                    Color.Transparent
                ),
                center = touch,
                radius = fieldRadius
            ),
            radius = fieldRadius,
            center = touch
        )
    }

    val computedPositions = mutableListOf<Offset>()

    particles.forEach { p ->
        // Natural ambient drift
        val driftX = cos(time * p.speed + p.phase) * 16f
        val driftY = sin(time * p.speed + p.phase) * 16f

        var px = (p.originX * w) + driftX
        var py = (p.originY * h) + driftY

        // Magnetic displacement if user touches
        if (touch != null) {
            val dx = touch.x - px
            val dy = touch.y - py
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

            if (dist < fieldRadius) {
                val force = (1f - dist / fieldRadius) * 55f
                if (isRepel) {
                    px -= (dx / dist) * force
                    py -= (dy / dist) * force
                } else {
                    px += (dx / dist) * force
                    py += (dy / dist) * force
                }
            }
        }

        val pt = Offset(px, py)
        computedPositions.add(pt)

        val col = if (p.isElectric) ZamaElectricCyan else Color.White
        drawCircle(
            color = col.copy(alpha = p.baseAlpha),
            radius = p.size,
            center = pt
        )
    }

    // Connect close neighbors
    for (i in 0 until computedPositions.size step 3) {
        if (i + 1 < computedPositions.size) {
            val p1 = computedPositions[i]
            val p2 = computedPositions[i + 1]
            val dx = p1.x - p2.x
            val dy = p1.y - p2.y
            val d = sqrt(dx * dx + dy * dy)
            if (d < 65f) {
                drawLine(
                    color = Color(0x2200E5FF),
                    start = p1,
                    end = p2,
                    strokeWidth = 0.8f
                )
            }
        }
    }
}
