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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransformationPhase
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaVoid
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// 3D Point representation
private data class Point3D(val x: Float, val y: Float, val z: Float)

// Polygon face for the 3D Z
private data class Polygon3D(
    val indices: List<Int>,
    val baseColor: Color,
    val isGlassEdge: Boolean = false
)

// Particulate fragment
private data class ParticleFragment(
    val basePos: Point3D,
    val velocityVector: Point3D,
    val targetWordPos: Point3D,
    val size: Float,
    val color: Color
)

@Composable
fun Zama3DVisualizer(
    modifier: Modifier = Modifier,
    initialPhase: TransformationPhase = TransformationPhase.PHASE_01,
    onPhaseChanged: (TransformationPhase) -> Unit = {}
) {
    var currentPhase by remember { mutableStateOf(initialPhase) }
    var scrubProgress by remember { mutableFloatStateOf(0.0f) } // 0.0f to 5.0f
    var bloomEnabled by remember { mutableStateOf(true) }

    // 3D Rotation Angles (Euler)
    var rotX by remember { mutableFloatStateOf(-12f) }
    var rotY by remember { mutableFloatStateOf(24f) }
    var isUserInteracting by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_rotation")
    val ambientRotY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_y"
    )
    val ambientFloatZ by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_z"
    )

    // Compute effective rotation
    val effectiveRotY = if (isUserInteracting) rotY else (rotY + ambientRotY)
    val effectiveRotX = rotX

    // 60 dynamic particulate fragments
    val fragments = remember {
        val list = mutableListOf<ParticleFragment>()
        val rnd = java.util.Random(42)
        for (i in 0 until 90) {
            // Distribute on Z shape bounds
            val t = rnd.nextFloat()
            val px: Float
            val py: Float
            if (t < 0.33f) {
                // Top bar of Z
                px = -120f + rnd.nextFloat() * 240f
                py = -110f + rnd.nextFloat() * 40f
            } else if (t < 0.66f) {
                // Diagonal of Z
                val diagT = rnd.nextFloat()
                px = 100f - diagT * 200f
                py = -80f + diagT * 160f
            } else {
                // Bottom bar of Z
                px = -120f + rnd.nextFloat() * 240f
                py = 80f + rnd.nextFloat() * 40f
            }
            val pz = -30f + rnd.nextFloat() * 60f

            // Vector pointing outward from center
            val dist = sqrt(px * px + py * py + pz * pz).coerceAtLeast(1f)
            val vx = (px / dist) * (180f + rnd.nextFloat() * 140f)
            val vy = (py / dist) * (180f + rnd.nextFloat() * 140f)
            val vz = (pz / dist) * (180f + rnd.nextFloat() * 140f)

            // Letter target position for Phase 04 ("Z A M A")
            val letterIndex = i % 6
            val tx = -180f + (letterIndex * 65f) + (rnd.nextFloat() - 0.5f) * 20f
            val ty = (rnd.nextFloat() - 0.5f) * 45f
            val tz = (rnd.nextFloat() - 0.5f) * 30f

            val color = if (rnd.nextFloat() > 0.4f) {
                ZamaChromeLight
            } else if (rnd.nextFloat() > 0.5f) {
                ZamaElectricCyan
            } else {
                Color(0xFFE2E8F0)
            }

            list.add(
                ParticleFragment(
                    basePos = Point3D(px, py, pz),
                    velocityVector = Point3D(vx, vy, vz),
                    targetWordPos = Point3D(tx, ty, tz),
                    size = 2.5f + rnd.nextFloat() * 4f,
                    color = color
                )
            )
        }
        list
    }

    // Map scrubProgress (0..5) to phase
    LaunchedEffect(scrubProgress) {
        val index = scrubProgress.toInt().coerceIn(0, TransformationPhase.entries.size - 1)
        val selected = TransformationPhase.entries[index]
        if (selected != currentPhase) {
            currentPhase = selected
            onPhaseChanged(selected)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F131C),
                        ZamaVoid
                    ),
                    radius = 800f
                )
            )
            .border(1.dp, ZamaBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top HUD Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(ZamaElectricCyan, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ZAMA // 3D CORE ENGINE",
                    color = ZamaChromeLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Post-Processing Bloom Toggle Pill
                Surface(
                    onClick = { bloomEnabled = !bloomEnabled },
                    color = if (bloomEnabled) Color(0x3300E5FF) else Color(0x1AFFFFFF),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (bloomEnabled) Color(0x9900E5FF) else Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.testTag("toggle_bloom_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flare,
                            contentDescription = "Bloom Filter",
                            tint = if (bloomEnabled) ZamaElectricCyan else ZamaChromeMid,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (bloomEnabled) "BLOOM: ON" else "BLOOM: OFF",
                            color = if (bloomEnabled) ZamaElectricCyan else ZamaChromeMid,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = Color(0x3300E5FF),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF))
                ) {
                    Text(
                        text = "PHASE 0${currentPhase.phaseNumber} : ${currentPhase.title}",
                        color = ZamaElectricCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main 3D Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF07080B))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isUserInteracting = true },
                        onDragEnd = { isUserInteracting = false },
                        onDragCancel = { isUserInteracting = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            rotY += dragAmount.x * 0.45f
                            rotX = (rotX - dragAmount.y * 0.45f).coerceIn(-65f, 65f)
                        }
                    )
                }
                .testTag("canvas_3d_z_sculpture"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Draw background cybernetic coordinate grid & deep focal glow with post-processing bloom
                drawRadialGlow(cx, cy, currentPhase.glowIntensity, bloomEnabled = bloomEnabled)

                // Render 3D Z object & particle fragmentation with optional post-processing bloom
                renderZama3DWorld(
                    cx = cx,
                    cy = cy,
                    rotX = effectiveRotX,
                    rotY = effectiveRotY,
                    ambientZ = ambientFloatZ,
                    phase = currentPhase,
                    progress = scrubProgress,
                    fragments = fragments,
                    bloomEnabled = bloomEnabled
                )
            }

            // Interactive gesture overlay hint
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Surface(
                    color = Color(0x99000000),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate in 3D",
                            tint = ZamaChromeMid,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DRAG TO ROTATE 360°",
                            color = ZamaChromeMid,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Phase Description and scrub bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = currentPhase.description,
                color = ZamaChromeMid,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Transformation Timeline Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TRANSFORMATION SCRUBBER",
                    color = ZamaGraphite,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${((scrubProgress / 5f) * 100).toInt()}% DISPERSION",
                    color = ZamaElectricCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = scrubProgress,
                onValueChange = { scrubProgress = it },
                valueRange = 0f..5f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = ZamaElectricCyan,
                    activeTrackColor = ZamaElectricCyan,
                    inactiveTrackColor = Color(0xFF1E2430)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scrubber_transformation")
            )

            // Phase Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TransformationPhase.entries.forEach { phase ->
                    val isSelected = currentPhase == phase
                    TextButton(
                        onClick = {
                            currentPhase = phase
                            scrubProgress = (phase.phaseNumber - 1).toFloat()
                            onPhaseChanged(phase)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isSelected) ZamaElectricCyan else ZamaGraphite
                        ),
                        modifier = Modifier.testTag("phase_btn_${phase.phaseNumber}")
                    ) {
                        Text(
                            text = "0${phase.phaseNumber}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// 3D Canvas Rendering Helpers
private fun DrawScope.drawRadialGlow(cx: Float, cy: Float, intensity: Float, bloomEnabled: Boolean = true) {
    val bloomScale = if (bloomEnabled) 1.5f else 1.0f
    val bloomAlpha = if (bloomEnabled) 1.6f else 1.0f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x5500E5FF).copy(alpha = (0.32f * intensity * bloomAlpha).coerceIn(0f, 0.6f)),
                Color(0x222979FF).copy(alpha = (0.20f * intensity * bloomAlpha).coerceIn(0f, 0.4f)),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = 280f * bloomScale
        ),
        radius = 280f * bloomScale,
        center = Offset(cx, cy)
    )

    // Secondary wide ambient soft atmospheric scatter
    if (bloomEnabled) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x1800E5FF).copy(alpha = (0.15f * intensity).coerceIn(0f, 0.3f)),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = 380f
            ),
            radius = 380f,
            center = Offset(cx, cy)
        )
    }

    // Subtle coordinate lines
    drawLine(
        color = Color(0x15FFFFFF),
        start = Offset(cx - 160f, cy),
        end = Offset(cx + 160f, cy),
        strokeWidth = 1f
    )
    drawLine(
        color = Color(0x15FFFFFF),
        start = Offset(cx, cy - 140f),
        end = Offset(cx, cy + 140f),
        strokeWidth = 1f
    )
}

private fun DrawScope.renderZama3DWorld(
    cx: Float,
    cy: Float,
    rotX: Float,
    rotY: Float,
    ambientZ: Float,
    phase: TransformationPhase,
    progress: Float,
    fragments: List<ParticleFragment>,
    bloomEnabled: Boolean = true
) {
    val radX = (rotX * PI / 180f).toFloat()
    val radY = (rotY * PI / 180f).toFloat()
    val cosX = cos(radX)
    val sinX = sin(radX)
    val cosY = cos(radY)
    val sinY = sin(radY)

    // Camera perspective distance
    val cameraDist = 550f

    // 3D Rotation Function
    fun project(p: Point3D): Offset? {
        // Rotate around Y
        val x1 = p.x * cosY + p.z * sinY
        val y1 = p.y
        val z1 = -p.x * sinY + p.z * cosY + ambientZ

        // Rotate around X
        val x2 = x1
        val y2 = y1 * cosX - z1 * sinX
        val z2 = y1 * sinX + z1 * cosX

        val depth = cameraDist + z2
        if (depth <= 20f) return null

        val scale = cameraDist / depth
        val screenX = cx + x2 * scale
        val screenY = cy + y2 * scale
        return Offset(screenX, screenY)
    }

    // Check phase dispersion factor
    val dispersion = (progress / 5.0f).coerceIn(0f, 1f)

    // If Phase 1, 2, or early 3, draw the solid polygonal 3D Z sculpture
    val solidAlpha = (1f - (dispersion * 1.6f)).coerceIn(0f, 1f)
    if (solidAlpha > 0.05f) {
        drawSolid3DZ(
            project = ::project,
            solidAlpha = solidAlpha,
            cosX = cosX,
            sinX = sinX,
            cosY = cosY,
            sinY = sinY,
            bloomEnabled = bloomEnabled
        )
    }

    // Draw particulate fragments
    fragments.forEachIndexed { idx, frag ->
        // Compute position based on phase
        val curPos: Point3D = when {
            dispersion < 0.2f -> {
                // Near base position
                val jitter = (dispersion / 0.2f) * 12f
                Point3D(
                    frag.basePos.x + if (idx % 2 == 0) jitter else -jitter,
                    frag.basePos.y + if (idx % 3 == 0) jitter else -jitter,
                    frag.basePos.z
                )
            }
            dispersion < 0.65f -> {
                // Expanding outward
                val t = (dispersion - 0.2f) / 0.45f
                Point3D(
                    frag.basePos.x + frag.velocityVector.x * t,
                    frag.basePos.y + frag.velocityVector.y * t,
                    frag.basePos.z + frag.velocityVector.z * t
                )
            }
            dispersion < 0.85f -> {
                // Assembling into ZAMA AI letters
                val t = (dispersion - 0.65f) / 0.2f
                val startX = frag.basePos.x + frag.velocityVector.x * 0.8f
                val startY = frag.basePos.y + frag.velocityVector.y * 0.8f
                val startZ = frag.basePos.z + frag.velocityVector.z * 0.8f
                Point3D(
                    startX + (frag.targetWordPos.x - startX) * t,
                    startY + (frag.targetWordPos.y - startY) * t,
                    startZ + (frag.targetWordPos.z - startZ) * t
                )
            }
            else -> {
                // Dissolving into quantum stream
                val t = (dispersion - 0.85f) / 0.15f
                Point3D(
                    frag.targetWordPos.x + (idx * 3.5f - 150f) * t * 1.5f,
                    frag.targetWordPos.y - t * 160f,
                    frag.targetWordPos.z + t * 90f
                )
            }
        }

        val proj = project(curPos)
        if (proj != null) {
            val particleAlpha = when {
                dispersion < 0.1f -> 0.4f
                dispersion in 0.1f..0.85f -> 0.95f
                else -> (1f - (dispersion - 0.85f) / 0.15f).coerceIn(0.1f, 1f)
            }

            // Soft bloom glow halo around metallic particles
            if (bloomEnabled) {
                drawCircle(
                    color = frag.color.copy(alpha = particleAlpha * 0.28f),
                    radius = frag.size * (0.8f + dispersion * 0.5f) * 2.8f,
                    center = proj
                )
            }

            drawCircle(
                color = frag.color.copy(alpha = particleAlpha),
                radius = frag.size * (0.8f + dispersion * 0.5f),
                center = proj
            )

            // Connect nearest nodes with laser vectors in phase 04 & 05
            if (dispersion in 0.55f..0.9f && idx % 4 == 0 && idx + 1 < fragments.size) {
                val nextProj = project(fragments[idx + 1].targetWordPos)
                if (nextProj != null) {
                    drawLine(
                        color = ZamaElectricCyan.copy(alpha = 0.35f * phase.glowIntensity),
                        start = proj,
                        end = nextProj,
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}

// Draw the solid 3D Z Polytope with specular chrome reflections and glass bevels
private fun DrawScope.drawSolid3DZ(
    project: (Point3D) -> Offset?,
    solidAlpha: Float,
    cosX: Float,
    sinX: Float,
    cosY: Float,
    sinY: Float,
    bloomEnabled: Boolean = true
) {
    // 3D vertices of a sculpted, faceted futuristic Z letter
    // Z top bar, diagonal, bottom bar with depth
    val w = 95f
    val h = 115f
    val barH = 34f
    val depth = 28f

    // 16 key vertices for front and back faces of Z
    // Front face (Z = +depth)
    val v0 = Point3D(-w, -h, depth)
    val v1 = Point3D(w, -h, depth)
    val v2 = Point3D(w, -h + barH, depth)
    val v3 = Point3D(-w + 50f, h - barH, depth)
    val v4 = Point3D(-w, h - barH, depth)
    val v5 = Point3D(-w, h, depth)
    val v6 = Point3D(w, h, depth)
    val v7 = Point3D(w, h - barH, depth)
    val v8 = Point3D(-w + 55f, -h + barH, depth)

    // Back face (Z = -depth)
    val b0 = Point3D(-w, -h, -depth)
    val b1 = Point3D(w, -h, -depth)
    val b2 = Point3D(w, -h + barH, -depth)
    val b3 = Point3D(-w + 50f, h - barH, -depth)
    val b4 = Point3D(-w, h - barH, -depth)
    val b5 = Point3D(-w, h, -depth)
    val b6 = Point3D(w, h, -depth)
    val b7 = Point3D(w, h - barH, -depth)
    val b8 = Point3D(-w + 55f, -h + barH, -depth)

    // Project points
    val pV0 = project(v0) ?: return
    val pV1 = project(v1) ?: return
    val pV2 = project(v2) ?: return
    val pV3 = project(v3) ?: return
    val pV4 = project(v4) ?: return
    val pV5 = project(v5) ?: return
    val pV6 = project(v6) ?: return
    val pV7 = project(v7) ?: return
    val pV8 = project(v8) ?: return

    val pB0 = project(b0) ?: return
    val pB1 = project(b1) ?: return
    val pB2 = project(b2) ?: return
    val pB3 = project(b3) ?: return
    val pB6 = project(b6) ?: return
    val pB7 = project(b7) ?: return

    // Draw back depth facets (metallic chrome shadow)
    val backPath = Path().apply {
        moveTo(pB0.x, pB0.y)
        lineTo(pB1.x, pB1.y)
        lineTo(pB2.x, pB2.y)
        lineTo(pB3.x, pB3.y)
        lineTo(pB7.x, pB7.y)
        lineTo(pB6.x, pB6.y)
        close()
    }
    drawPath(
        path = backPath,
        color = Color(0xFF090B0E).copy(alpha = 0.85f * solidAlpha)
    )

    // Side extruded chrome bevels
    fun drawSideQuad(p1: Offset, p2: Offset, p3: Offset, p4: Offset, color: Color) {
        val quad = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(p4.x, p4.y)
            close()
        }
        drawPath(quad, color.copy(alpha = 0.9f * solidAlpha))
    }

    // Top surface bevel
    drawSideQuad(pV0, pV1, pB1, pB0, Color(0xFFC0C7D5))
    // Right bevel of top bar
    drawSideQuad(pV1, pV2, pB2, pB1, Color(0xFF6B7280))
    // Diagonal side bevel
    drawSideQuad(pV2, pV7, pB7, pB2, Color(0xFF1E2430))
    // Bottom right bevel
    drawSideQuad(pV7, pV6, pB6, pB7, Color(0xFF4B5563))

    // Front main face path of Z
    // Composed of top bar, diagonal, and bottom bar
    val frontZPath = Path().apply {
        moveTo(pV0.x, pV0.y)
        lineTo(pV1.x, pV1.y)
        lineTo(pV2.x, pV2.y)
        lineTo(pV3.x, pV3.y)
        lineTo(pV4.x, pV4.y)
        lineTo(pV5.x, pV5.y)
        lineTo(pV6.x, pV6.y)
        lineTo(pV7.x, pV7.y)
        lineTo(pV8.x, pV8.y)
        close()
    }

    // Polished obsidian chrome gradient with specular highlight
    drawPath(
        path = frontZPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF262C38),
                Color(0xFF0F1218),
                Color(0xFF181C26),
                Color(0xFF2D3545)
            ),
            start = pV0,
            end = pV6
        ),
        alpha = solidAlpha
    )

    // Post-Processing Bloom Pass 1: Soft diffuse glow behind chrome contours
    if (bloomEnabled) {
        // Broad outer diffuse atmospheric bloom
        drawPath(
            path = frontZPath,
            color = ZamaElectricCyan.copy(alpha = 0.12f * solidAlpha),
            style = Stroke(width = 18f, cap = StrokeCap.Round)
        )
        // Mid-range specular dispersion halo
        drawPath(
            path = frontZPath,
            color = Color(0xFF64D2FF).copy(alpha = 0.22f * solidAlpha),
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
        // Tight high-intensity core radiance
        drawPath(
            path = frontZPath,
            color = Color.White.copy(alpha = 0.35f * solidAlpha),
            style = Stroke(width = 3.8f, cap = StrokeCap.Round)
        )
    }

    // Glass-like sharp chrome wireframe edges
    drawPath(
        path = frontZPath,
        color = ZamaChromeLight.copy(alpha = 0.85f * solidAlpha),
        style = Stroke(width = 1.8f, cap = StrokeCap.Round)
    )

    // Specular silver reflection slash
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                ZamaElectricCyan.copy(alpha = 0.8f * solidAlpha),
                Color.White.copy(alpha = 0.95f * solidAlpha),
                Color.Transparent
            )
        ),
        start = pV1,
        end = pV4,
        strokeWidth = 2.2f
    )

    // Post-Processing Bloom Pass 2: Specular Reflection Flare & Anamorphic Lens Bloom Streak
    if (bloomEnabled) {
        // Specular glow along the diagonal reflection slash
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    ZamaElectricCyan.copy(alpha = 0.35f * solidAlpha),
                    Color.White.copy(alpha = 0.5f * solidAlpha),
                    Color.Transparent
                )
            ),
            start = pV1,
            end = pV4,
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )

        // Anamorphic horizontal bloom streaks centered on high-reflectance corner vertices
        val flareVertices = listOf(pV1, pV0, pV6, pV4)
        flareVertices.forEachIndexed { i, pt ->
            val flareIntensity = if (i == 0 || i == 3) 1.0f else 0.6f
            // Wide anamorphic horizontal streak (cinematic sci-fi optical flare)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ZamaElectricCyan.copy(alpha = 0.45f * solidAlpha * flareIntensity),
                        Color.White.copy(alpha = 0.9f * solidAlpha * flareIntensity),
                        ZamaElectricCyan.copy(alpha = 0.45f * solidAlpha * flareIntensity),
                        Color.Transparent
                    ),
                    startX = pt.x - 45f,
                    endX = pt.x + 45f
                ),
                start = Offset(pt.x - 45f, pt.y),
                end = Offset(pt.x + 45f, pt.y),
                strokeWidth = 2.5f
            )

            // Radial soft bloom star / core halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f * solidAlpha * flareIntensity),
                        ZamaElectricCyan.copy(alpha = 0.4f * solidAlpha * flareIntensity),
                        Color.Transparent
                    ),
                    center = pt,
                    radius = 16f
                ),
                radius = 16f,
                center = pt
            )
        }
    }
}
