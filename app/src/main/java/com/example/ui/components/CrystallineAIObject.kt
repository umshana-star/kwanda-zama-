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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricBlue
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaVoid
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class CrystallineVertex(val x: Float, val y: Float, val z: Float)
private data class InnerComputationNode(
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val yOffset: Float,
    val color: Color
)

@Composable
fun CrystallineAIObject(
    modifier: Modifier = Modifier
) {
    var rotX by remember { mutableFloatStateOf(18f) }
    var rotY by remember { mutableFloatStateOf(45f) }
    var zoomFactor by remember { mutableFloatStateOf(1.0f) } // 0.8 to 1.6
    var isUserInteracting by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "crystal_spin")
    val autoRotY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_rot_y"
    )
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val currentRotY = if (isUserInteracting) rotY else (rotY + autoRotY)

    // Inner computation particles
    val innerNodes = remember {
        val list = mutableListOf<InnerComputationNode>()
        val rnd = java.util.Random(101)
        for (i in 0 until 40) {
            list.add(
                InnerComputationNode(
                    radius = 20f + rnd.nextFloat() * 55f,
                    speed = 0.4f + rnd.nextFloat() * 1.2f,
                    phase = rnd.nextFloat() * 2f * PI.toFloat(),
                    yOffset = (rnd.nextFloat() - 0.5f) * 70f,
                    color = if (i % 2 == 0) ZamaElectricCyan else ZamaElectricBlue
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
                        Color(0xFF0B0E14),
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
                    text = "SECTION 03 // THE AI OBJECT",
                    color = ZamaElectricCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "CRYSTALLINE INTELLIGENCE",
                    color = ZamaChromeLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Surface(
                color = Color(0x2238BDF8),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4438BDF8))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "FHE Encrypted",
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FHE STATE MESH",
                        color = ZamaElectricCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Crystalline 3D Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF06080C))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isUserInteracting = true },
                        onDragEnd = { isUserInteracting = false },
                        onDragCancel = { isUserInteracting = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            rotY += dragAmount.x * 0.5f
                            rotX = (rotX - dragAmount.y * 0.5f).coerceIn(-75f, 75f)
                        }
                    )
                }
                .testTag("crystalline_canvas"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Draw central quantum aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ZamaElectricCyan.copy(alpha = 0.2f * corePulse),
                            Color(0x1138BDF8).copy(alpha = 0.1f * corePulse),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = 180f * zoomFactor
                    ),
                    radius = 180f * zoomFactor,
                    center = Offset(cx, cy)
                )

                // Render 3D crystalline polytope
                renderCrystalPolytope(
                    cx = cx,
                    cy = cy,
                    rotX = rotX,
                    rotY = currentRotY,
                    zoom = zoomFactor,
                    pulse = corePulse,
                    innerNodes = innerNodes
                )
            }

            // HUD telemetry badges
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(ZamaElectricCyan, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LATTICE: 20 FACETS • 12 VERTICES",
                    color = ZamaGraphite,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Camera proximity (zoom) slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CAMERA PROXIMITY",
                color = ZamaGraphite,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = String.format("%.1fx ZOOM", zoomFactor),
                color = ZamaChromeLight,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = zoomFactor,
            onValueChange = { zoomFactor = it },
            valueRange = 0.75f..1.45f,
            colors = SliderDefaults.colors(
                thumbColor = ZamaChromeLight,
                activeTrackColor = ZamaChromeMid,
                inactiveTrackColor = Color(0xFF1E2430)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "An abstract crystalline intelligence system constructed from optical glass, chrome bevels, and micro-particles representing encrypted computation and zero-knowledge neural workflows.",
            color = ZamaChromeMid,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

private fun DrawScope.renderCrystalPolytope(
    cx: Float,
    cy: Float,
    rotX: Float,
    rotY: Float,
    zoom: Float,
    pulse: Float,
    innerNodes: List<InnerComputationNode>
) {
    val radX = (rotX * PI / 180f).toFloat()
    val radY = (rotY * PI / 180f).toFloat()
    val cosX = cos(radX)
    val sinX = sin(radX)
    val cosY = cos(radY)
    val sinY = sin(radY)
    val cameraDist = 500f

    fun project(p: CrystallineVertex): Offset? {
        val x1 = p.x * cosY + p.z * sinY
        val y1 = p.y
        val z1 = -p.x * sinY + p.z * cosY

        val x2 = x1
        val y2 = y1 * cosX - z1 * sinX
        val z2 = y1 * sinX + z1 * cosX

        val depth = cameraDist + z2
        if (depth <= 10f) return null

        val scale = (cameraDist / depth) * zoom
        return Offset(cx + x2 * scale, cy + y2 * scale)
    }

    // 12 vertices of regular icosahedron scaled to base 100
    val phi = (1f + sqrt(5f)) / 2f
    val scale = 80f
    val rawVertices = listOf(
        CrystallineVertex(-1f * scale, phi * scale, 0f),
        CrystallineVertex(1f * scale, phi * scale, 0f),
        CrystallineVertex(-1f * scale, -phi * scale, 0f),
        CrystallineVertex(1f * scale, -phi * scale, 0f),
        CrystallineVertex(0f, -1f * scale, phi * scale),
        CrystallineVertex(0f, 1f * scale, phi * scale),
        CrystallineVertex(0f, -1f * scale, -phi * scale),
        CrystallineVertex(0f, 1f * scale, -phi * scale),
        CrystallineVertex(phi * scale, 0f, -1f * scale),
        CrystallineVertex(phi * scale, 0f, 1f * scale),
        CrystallineVertex(-phi * scale, 0f, -1f * scale),
        CrystallineVertex(-phi * scale, 0f, 1f * scale)
    )

    // Triangular faces
    val faces = listOf(
        Triple(0, 11, 5), Triple(0, 5, 1), Triple(0, 1, 7), Triple(0, 7, 10), Triple(0, 10, 11),
        Triple(1, 5, 9), Triple(5, 11, 4), Triple(11, 10, 2), Triple(10, 7, 6), Triple(7, 1, 8),
        Triple(3, 9, 4), Triple(3, 4, 2), Triple(3, 2, 6), Triple(3, 6, 8), Triple(3, 8, 9),
        Triple(4, 9, 5), Triple(2, 4, 11), Triple(6, 2, 10), Triple(8, 6, 7), Triple(9, 8, 1)
    )

    val projected = rawVertices.map { project(it) }

    // Draw transparent glass facets with soft chrome borders
    faces.forEachIndexed { idx, (i1, i2, i3) ->
        val p1 = projected[i1]
        val p2 = projected[i2]
        val p3 = projected[i3]

        if (p1 != null && p2 != null && p3 != null) {
            val path = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }

            // Glass facet fill
            val facetAlpha = if (idx % 2 == 0) 0.12f else 0.06f
            val tint = if (idx % 3 == 0) ZamaElectricCyan else Color(0xFF60A5FA)
            drawPath(path, tint.copy(alpha = facetAlpha))

            // Glass facet edge line
            drawPath(
                path = path,
                color = ZamaChromeLight.copy(alpha = 0.45f),
                style = Stroke(width = 1.1f)
            )
        }
    }

    // Draw internal encrypted computation particles flowing in orbits
    innerNodes.forEachIndexed { i, node ->
        val angle = (radY * node.speed) + node.phase
        val px = cos(angle) * node.radius
        val py = node.yOffset + sin(angle * 2f) * 15f
        val pz = sin(angle) * node.radius

        val proj = project(CrystallineVertex(px, py, pz))
        if (proj != null) {
            drawCircle(
                color = node.color.copy(alpha = 0.85f * pulse),
                radius = 2.5f * zoom,
                center = proj
            )

            // Sparkle halo
            drawCircle(
                color = Color.White.copy(alpha = 0.35f * pulse),
                radius = 4.5f * zoom,
                center = proj
            )
        }
    }

    // Outer vertex chrome nodes
    projected.filterNotNull().forEach { pt ->
        drawCircle(
            color = ZamaChromeLight,
            radius = 2.8f * zoom,
            center = pt
        )
    }
}
