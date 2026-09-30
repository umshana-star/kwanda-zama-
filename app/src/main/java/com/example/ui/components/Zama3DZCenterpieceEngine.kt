package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZamaAmberPulse
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaCardSurface
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricBlue
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaPurple
import com.example.ui.theme.ZamaVoid
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================================
// 1. 3D VECTOR & MATRIX ENGINE
// ============================================================================

/**
 * 3D Coordinate in Euclidean Space.
 */
data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 0.00001f) Vector3(x / len, y / len, z / len) else Vector3(0f, 0f, 1f)
    }
}

/**
 * 2D Projected Screen Coordinate with depth info.
 */
data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val isVisible: Boolean
)

/**
 * Triangulated Surface Facet for the 3D Z Mesh.
 */
data class FacetTriangle(
    val v0: Int,
    val v1: Int,
    val v2: Int,
    val facetCategory: FacetCategory,
    val metallicFactor: Float = 1.0f
)

enum class FacetCategory {
    FRONT_CAP,
    BACK_CAP,
    OUTER_PERIMETER_WALL,
    INNER_DIAGONAL_SLOPE,
    CHAMFER_BEVEL
}

/**
 * Centerpiece Shading Themes.
 */
enum class ZCenterpieceTheme(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val specularTint: Color,
    val wireframeColor: Color
) {
    PRISMATIC_CHROME(
        displayName = "PRISMATIC CHROME",
        primaryColor = Color(0xFF1E2838),
        secondaryColor = Color(0xFF0D1420),
        accentColor = ZamaElectricCyan,
        specularTint = Color(0xFFFFFFFF),
        wireframeColor = Color(0xFF00E5FF)
    ),
    NEON_CYBER_MESH(
        displayName = "NEON CYBERPUNK",
        primaryColor = Color(0xCC05101A),
        secondaryColor = Color(0x99002533),
        accentColor = Color(0xFF00FFCC),
        specularTint = Color(0xFF00E5FF),
        wireframeColor = Color(0xFF00FF99)
    ),
    QUANTUM_HOLO(
        displayName = "QUANTUM FLUX",
        primaryColor = Color(0x88110D28),
        secondaryColor = Color(0x66080516),
        accentColor = ZamaPurple,
        specularTint = Color(0xFFE1BEE7),
        wireframeColor = Color(0xFFD500F9)
    ),
    SOLAR_AMBER(
        displayName = "SOLAR AMBER",
        primaryColor = Color(0xFF261805),
        secondaryColor = Color(0xFF120B02),
        accentColor = ZamaAmberPulse,
        specularTint = Color(0xFFFFE082),
        wireframeColor = Color(0xFFFF9100)
    )
}

// ============================================================================
// 2. THE 3D "Z" OBJECT MESH GENERATOR
// ============================================================================

/**
 * High-precision mathematical definition of the 3D Zama "Z" geometry.
 */
object Zama3DMeshGenerator {

    /**
     * Generates all 3D vertices for the extruded "Z" emblem.
     * Front face at +depth, Back face at -depth.
     */
    fun createZVertices(thickness: Float = 0.38f): List<Vector3> {
        // 10 Outer boundary vertices tracing the iconic Z contour:
        val zContour2D = listOf(
            Vector3(-0.95f, -1.05f, 0f), // 0: Top-left outer
            Vector3(0.95f, -1.05f, 0f),  // 1: Top-right outer
            Vector3(0.95f, -0.58f, 0f),  // 2: Top-right shelf lower drop
            Vector3(-0.25f, 0.58f, 0f),  // 3: Diagonal slope down-left inner corner
            Vector3(0.95f, 0.58f, 0f),   // 4: Bottom-right shelf top
            Vector3(0.95f, 1.05f, 0f),   // 5: Bottom-right outer
            Vector3(-0.95f, 1.05f, 0f),  // 6: Bottom-left outer
            Vector3(-0.95f, 0.58f, 0f),  // 7: Bottom-left shelf upper rise
            Vector3(0.25f, -0.58f, 0f),  // 8: Diagonal slope up-right inner corner
            Vector3(-0.95f, -0.58f, 0f)  // 9: Top-left shelf bottom
        )

        val halfDepth = thickness * 0.5f
        val vertices = mutableListOf<Vector3>()

        // 0..9: Front face vertices (z = +halfDepth)
        for (v in zContour2D) {
            vertices.add(Vector3(v.x, v.y, halfDepth))
        }

        // 10..19: Back face vertices (z = -halfDepth)
        for (v in zContour2D) {
            vertices.add(Vector3(v.x, v.y, -halfDepth))
        }

        return vertices
    }

    /**
     * Builds all triangulated facets (polygons) with consistent outward normals.
     */
    fun createZFacets(): List<FacetTriangle> {
        val facets = mutableListOf<FacetTriangle>()

        // 1. FRONT FACE TRIANGLES (Counter-Clockwise winding, normal facing +Z)
        // Top bar
        facets.add(FacetTriangle(0, 1, 2, FacetCategory.FRONT_CAP, 1.1f))
        facets.add(FacetTriangle(0, 2, 9, FacetCategory.FRONT_CAP, 1.0f))
        facets.add(FacetTriangle(9, 2, 8, FacetCategory.FRONT_CAP, 1.05f))
        // Diagonal crossbar
        facets.add(FacetTriangle(8, 2, 3, FacetCategory.FRONT_CAP, 1.15f))
        facets.add(FacetTriangle(8, 3, 7, FacetCategory.FRONT_CAP, 1.1f))
        // Bottom bar
        facets.add(FacetTriangle(7, 3, 4, FacetCategory.FRONT_CAP, 1.0f))
        facets.add(FacetTriangle(7, 4, 6, FacetCategory.FRONT_CAP, 1.05f))
        facets.add(FacetTriangle(6, 4, 5, FacetCategory.FRONT_CAP, 1.1f))

        // 2. BACK FACE TRIANGLES (Clockwise winding from front, normal facing -Z)
        val b = 10
        // Top bar
        facets.add(FacetTriangle(b + 0, b + 2, b + 1, FacetCategory.BACK_CAP, 0.85f))
        facets.add(FacetTriangle(b + 0, b + 9, b + 2, FacetCategory.BACK_CAP, 0.85f))
        facets.add(FacetTriangle(b + 9, b + 8, b + 2, FacetCategory.BACK_CAP, 0.85f))
        // Diagonal crossbar
        facets.add(FacetTriangle(b + 8, b + 3, b + 2, FacetCategory.BACK_CAP, 0.9f))
        facets.add(FacetTriangle(b + 8, b + 7, b + 3, FacetCategory.BACK_CAP, 0.9f))
        // Bottom bar
        facets.add(FacetTriangle(b + 7, b + 4, b + 3, FacetCategory.BACK_CAP, 0.85f))
        facets.add(FacetTriangle(b + 7, b + 6, b + 4, FacetCategory.BACK_CAP, 0.85f))
        facets.add(FacetTriangle(b + 6, b + 5, b + 4, FacetCategory.BACK_CAP, 0.85f))

        // 3. PERIMETER EXTRUDED SIDE WALLS (Connecting front 0..9 with back 10..19)
        for (i in 0 until 10) {
            val j = (i + 1) % 10
            val fi = i
            val fj = j
            val bi = b + i
            val bj = b + j

            val category = when (i) {
                2, 7 -> FacetCategory.INNER_DIAGONAL_SLOPE
                3, 8 -> FacetCategory.CHAMFER_BEVEL
                else -> FacetCategory.OUTER_PERIMETER_WALL
            }

            val metallic = when (i) {
                0, 5 -> 1.25f // Top and bottom horizontal reflective shelves
                2, 7 -> 1.35f // Diagonal high-gleam facets
                else -> 1.0f
            }

            // Quad split into 2 triangles
            facets.add(FacetTriangle(fi, fj, bj, category, metallic))
            facets.add(FacetTriangle(fi, bj, bi, category, metallic))
        }

        return facets
    }

    /**
     * Floating Central Quantum Octahedron inside the Z core.
     */
    fun createCoreOctahedronVertices(scale: Float = 0.28f): List<Vector3> {
        return listOf(
            Vector3(0f, -scale * 1.3f, 0f), // 0: Top
            Vector3(0f, scale * 1.3f, 0f),  // 1: Bottom
            Vector3(-scale, 0f, 0f),        // 2: Left
            Vector3(scale, 0f, 0f),         // 3: Right
            Vector3(0f, 0f, scale),         // 4: Front
            Vector3(0f, 0f, -scale)         // 5: Back
        )
    }

    fun createCoreOctahedronFacets(): List<Triple<Int, Int, Int>> {
        return listOf(
            Triple(0, 4, 3), Triple(0, 3, 5), Triple(0, 5, 2), Triple(0, 2, 4), // Top 4 pyramids
            Triple(1, 3, 4), Triple(1, 5, 3), Triple(1, 2, 5), Triple(1, 4, 2)  // Bottom 4 pyramids
        )
    }
}

// ============================================================================
// 3. 3D MATH PROJECTION & LIGHTING ENGINE
// ============================================================================

object Zama3DProjectionEngine {

    /**
     * Rotates point by Euler angles in radians (Yaw = Y, Pitch = X, Roll = Z).
     */
    fun rotatePoint(p: Vector3, rx: Float, ry: Float, rz: Float): Vector3 {
        // 1. Rotate Y (Yaw)
        val cosY = cos(ry)
        val sinY = sin(ry)
        val x1 = p.x * cosY + p.z * sinY
        val y1 = p.y
        val z1 = -p.x * sinY + p.z * cosY

        // 2. Rotate X (Pitch)
        val cosX = cos(rx)
        val sinX = sin(rx)
        val x2 = x1
        val y2 = y1 * cosX - z1 * sinX
        val z2 = y1 * sinX + z1 * cosX

        // 3. Rotate Z (Roll)
        val cosZ = cos(rz)
        val sinZ = sin(rz)
        val x3 = x2 * cosZ - y2 * sinZ
        val y3 = x2 * sinZ + y2 * cosZ
        val z3 = z2

        return Vector3(x3, y3, z3)
    }

    /**
     * Projects 3D camera-space vector onto 2D screen Canvas.
     */
    fun project(
        point: Vector3,
        cameraDistance: Float,
        focalLength: Float,
        center: Offset,
        viewScale: Float
    ): ProjectedPoint {
        val zCamera = point.z + cameraDistance
        if (zCamera <= 0.15f) {
            return ProjectedPoint(0f, 0f, zCamera, isVisible = false)
        }

        val perspectiveRatio = (focalLength / zCamera) * viewScale
        val screenX = center.x + point.x * perspectiveRatio
        val screenY = center.y + point.y * perspectiveRatio

        return ProjectedPoint(screenX, screenY, zCamera, isVisible = true)
    }
}

// ============================================================================
// 4. MAIN JETPACK COMPOSE 3D Z CENTERPIECE
// ============================================================================

/**
 * Central visual centerpiece for Zama AI:
 * Features a real-time, interactive 3D Z object rendering engine with:
 * - Full 360° touch rotation with momentum inertia
 * - Mathematical Blinn-Phong specular & Lambertian diffuse lighting
 * - Dynamic depth-sorting (Painter's algorithm)
 * - 4 Visual Shading Themes (Prismatic Chrome, Neon Cyber-Mesh, Quantum Flux, Solar Amber)
 * - Concentric Gyroscopic Quantum Orbital Rings
 * - Pulsing Central Octahedral AI Core
 * - Interactive Quantum Shockwave Burst
 * - Live Cybernetic Telemetry HUD
 */
@Composable
fun Zama3DZCenterpieceEngine(
    modifier: Modifier = Modifier
) {
    var rotX by remember { mutableFloatStateOf(16f) }
    var rotY by remember { mutableFloatStateOf(35f) }
    var rotZ by remember { mutableFloatStateOf(0f) }

    var velX by remember { mutableFloatStateOf(0f) }
    var velY by remember { mutableFloatStateOf(0f) }

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var autoSpinEnabled by remember { mutableStateOf(true) }
    var spinSpeed by remember { mutableFloatStateOf(1.0f) }
    var activeTheme by remember { mutableStateOf(ZCenterpieceTheme.PRISMATIC_CHROME) }
    var showWireframeOnly by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val shockwaveAnim = remember { Animatable(0f) }

    // Infinite ambient animations: orbital rings, core pulse, quantum particle drift
    val infiniteTransition = rememberInfiniteTransition(label = "zama_3d_centerpiece")

    val autoYaw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_yaw"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val ringRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot_1"
    )

    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot_2"
    )

    // Apply auto-spin when enabled
    val effectiveRotY = remember(rotY, autoYaw, autoSpinEnabled, spinSpeed) {
        if (autoSpinEnabled) {
            (rotY + autoYaw * spinSpeed) % 360f
        } else {
            rotY
        }
    }

    // Prepare 3D Mesh Data
    val meshVertices = remember { Zama3DMeshGenerator.createZVertices(thickness = 0.42f) }
    val meshFacets = remember { Zama3DMeshGenerator.createZFacets() }
    val coreVertices = remember { Zama3DMeshGenerator.createCoreOctahedronVertices(scale = 0.28f) }
    val coreFacets = remember { Zama3DMeshGenerator.createCoreOctahedronFacets() }

    Surface(
        color = ZamaDarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ZamaBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("zama_3d_z_centerpiece")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Bar with Telemetry
            CenterpieceHeader(
                activeTheme = activeTheme,
                rotX = rotX,
                rotY = effectiveRotY,
                onReset = {
                    rotX = 16f
                    rotY = 35f
                    rotZ = 0f
                    zoomScale = 1.0f
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // The Interactive 3D Viewport Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeTheme.accentColor.copy(alpha = 0.12f),
                                Color(0xFF070B12),
                                ZamaVoid
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                activeTheme.accentColor.copy(alpha = 0.45f),
                                Color(0x1100E5FF),
                                activeTheme.accentColor.copy(alpha = 0.25f)
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                autoSpinEnabled = false
                                velX = 0f
                                velY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                rotY = (rotY + dragAmount.x * 0.45f) % 360f
                                rotX = (rotX - dragAmount.y * 0.45f).coerceIn(-85f, 85f)
                                velX = dragAmount.x
                                velY = dragAmount.y
                            }
                        )
                    }
                    .testTag("zama_3d_canvas_box"),
                contentAlignment = Alignment.Center
            ) {
                // Compose 3D Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("zama_3d_canvas")
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val center = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f)
                    val baseScale = min(canvasWidth, canvasHeight) * 0.38f * zoomScale

                    val rxRad = (rotX * PI / 180f).toFloat()
                    val ryRad = (effectiveRotY * PI / 180f).toFloat()
                    val rzRad = (rotZ * PI / 180f).toFloat()

                    val cameraDistance = 3.6f
                    val focalLength = 2.4f

                    // 1. Draw Background Cybernetic Radial Grid
                    drawCyberneticGrid(center, canvasWidth, canvasHeight, activeTheme.accentColor)

                    // 2. Draw Gyroscopic Orbital Rings in 3D
                    drawOrbitalRing(
                        center = center,
                        radius = baseScale * 1.55f,
                        rotX = rxRad + 0.35f,
                        rotY = ryRad + (ringRotation1 * PI / 180f).toFloat(),
                        cameraDistance = cameraDistance,
                        focalLength = focalLength,
                        viewScale = baseScale,
                        theme = activeTheme
                    )

                    drawOrbitalRing(
                        center = center,
                        radius = baseScale * 1.85f,
                        rotX = rxRad - 0.45f,
                        rotY = ryRad + (ringRotation2 * PI / 180f).toFloat(),
                        cameraDistance = cameraDistance,
                        focalLength = focalLength,
                        viewScale = baseScale,
                        theme = activeTheme
                    )

                    // 3. Transform All Z-Mesh Vertices to Camera Space
                    val rotatedVertices = meshVertices.map { v ->
                        Zama3DProjectionEngine.rotatePoint(v, rxRad, ryRad, rzRad)
                    }

                    val projectedPoints = rotatedVertices.map { v ->
                        Zama3DProjectionEngine.project(v, cameraDistance, focalLength, center, baseScale)
                    }

                    // 4. Transform Facets & Sort by Depth (Painter's Algorithm)
                    val lightDir = Vector3(0.55f, -0.75f, 1.25f).normalized()
                    val rimLightDir = Vector3(-0.85f, 0.45f, -0.6f).normalized()

                    data class RenderableFacet(
                        val p0: ProjectedPoint,
                        val p1: ProjectedPoint,
                        val p2: ProjectedPoint,
                        val normal: Vector3,
                        val depth: Float,
                        val facet: FacetTriangle
                    )

                    val renderableFacets = mutableListOf<RenderableFacet>()

                    for (facet in meshFacets) {
                        val p0 = projectedPoints[facet.v0]
                        val p1 = projectedPoints[facet.v1]
                        val p2 = projectedPoints[facet.v2]

                        if (!p0.isVisible || !p1.isVisible || !p2.isVisible) continue

                        val v0Rot = rotatedVertices[facet.v0]
                        val v1Rot = rotatedVertices[facet.v1]
                        val v2Rot = rotatedVertices[facet.v2]

                        // Compute transformed surface normal
                        val edge1 = v1Rot - v0Rot
                        val edge2 = v2Rot - v0Rot
                        val normal = edge1.cross(edge2).normalized()

                        val avgDepth = (v0Rot.z + v1Rot.z + v2Rot.z) / 3f

                        renderableFacets.add(
                            RenderableFacet(p0, p1, p2, normal, avgDepth, facet)
                        )
                    }

                    // Sort back to front (lowest Z in view space first, or highest distance)
                    renderableFacets.sortBy { it.depth }

                    // 5. Draw the 3D Z Facets with Shading & Wireframes
                    for (item in renderableFacets) {
                        val path = Path().apply {
                            moveTo(item.p0.screenX, item.p0.screenY)
                            lineTo(item.p1.screenX, item.p1.screenY)
                            lineTo(item.p2.screenX, item.p2.screenY)
                            close()
                        }

                        // Lighting calculation
                        val diffuse = max(0f, item.normal.dot(lightDir))
                        val rim = max(0f, item.normal.dot(rimLightDir)) * 0.4f

                        // Blinn-Phong specular gleam
                        val viewDir = Vector3(0f, 0f, 1f)
                        val halfVector = (lightDir + viewDir).normalized()
                        val specAngle = max(0f, item.normal.dot(halfVector))
                        val specular = specAngle.pow(14f) * item.facet.metallicFactor

                        val baseFill = if (showWireframeOnly) {
                            Color(0x0A00E5FF)
                        } else {
                            computeFacetColor(
                                theme = activeTheme,
                                category = item.facet.facetCategory,
                                diffuse = diffuse,
                                rim = rim,
                                specular = specular
                            )
                        }

                        // Fill polygon
                        drawPath(path = path, color = baseFill, style = Fill)

                        // Outline / Edge glow
                        val strokeColor = if (showWireframeOnly) {
                            activeTheme.wireframeColor
                        } else {
                            activeTheme.wireframeColor.copy(
                                alpha = (0.25f + diffuse * 0.65f).coerceIn(0.15f, 0.95f)
                            )
                        }

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = if (showWireframeOnly) 1.5.dp.toPx() else 1.0.dp.toPx(),
                                join = StrokeJoin.Round,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // 6. Draw the Internal Pulsing Quantum AI Core (Octahedron)
                    drawQuantumCore(
                        center = center,
                        coreVertices = coreVertices,
                        coreFacets = coreFacets,
                        pulseScale = corePulse,
                        rxRad = rxRad,
                        ryRad = ryRad * 1.5f,
                        cameraDistance = cameraDistance,
                        focalLength = focalLength,
                        viewScale = baseScale,
                        theme = activeTheme
                    )

                    // 7. Draw Expanding Shockwave Ripple when Burst is Triggered
                    val burstProgress = shockwaveAnim.value
                    if (burstProgress > 0f) {
                        val maxRadius = baseScale * 2.4f
                        val currentRadius = burstProgress * maxRadius
                        val alpha = (1f - burstProgress) * 0.75f

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    activeTheme.accentColor.copy(alpha = alpha),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = currentRadius + 8.dp.toPx()
                            ),
                            radius = currentRadius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                // HUD Overlay Tags on Top Right
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Surface(
                        color = Color(0xCC050A10),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0x3300E5FF))
                    ) {
                        Text(
                            text = "MESH: 36 FACETS // 20 VTX",
                            color = activeTheme.accentColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        color = Color(0xCC050A10),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Text(
                            text = "STATUS: NEURAL SYNC",
                            color = ZamaNeonGreen,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Floating Action Trigger: "Quantum Burst"
                Surface(
                    color = Color(0xDD0D1624),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, activeTheme.accentColor.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clickable {
                            coroutineScope.launch {
                                shockwaveAnim.snapTo(0f)
                                shockwaveAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(900, easing = LinearEasing)
                                )
                            }
                        }
                        .testTag("btn_quantum_burst")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Trigger Quantum Burst",
                            tint = activeTheme.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "TRIGGER QUANTUM BURST",
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Control Deck & Theme Switcher
            CenterpieceControlDeck(
                activeTheme = activeTheme,
                onSelectTheme = { activeTheme = it },
                autoSpinEnabled = autoSpinEnabled,
                onToggleAutoSpin = { autoSpinEnabled = !autoSpinEnabled },
                spinSpeed = spinSpeed,
                onSpeedChange = { spinSpeed = it },
                zoomScale = zoomScale,
                onZoomChange = { zoomScale = it },
                showWireframe = showWireframeOnly,
                onToggleWireframe = { showWireframeOnly = !showWireframeOnly }
            )
        }
    }
}

// ============================================================================
// 5. DRAWING & RENDERING PIPELINE HELPERS
// ============================================================================

/**
 * Computes the shaded surface color for a facet based on theme and Blinn-Phong lighting.
 */
private fun computeFacetColor(
    theme: ZCenterpieceTheme,
    category: FacetCategory,
    diffuse: Float,
    rim: Float,
    specular: Float
): Color {
    val base = when (category) {
        FacetCategory.FRONT_CAP -> theme.primaryColor
        FacetCategory.BACK_CAP -> theme.secondaryColor
        FacetCategory.OUTER_PERIMETER_WALL -> theme.secondaryColor
        FacetCategory.INNER_DIAGONAL_SLOPE -> theme.primaryColor
        FacetCategory.CHAMFER_BEVEL -> theme.primaryColor
    }

    val litR = (base.red * (0.35f + diffuse * 0.65f) + theme.accentColor.red * rim + theme.specularTint.red * specular).coerceIn(0f, 1f)
    val litG = (base.green * (0.35f + diffuse * 0.65f) + theme.accentColor.green * rim + theme.specularTint.green * specular).coerceIn(0f, 1f)
    val litB = (base.blue * (0.35f + diffuse * 0.65f) + theme.accentColor.blue * rim + theme.specularTint.blue * specular).coerceIn(0f, 1f)

    val alpha = if (theme == ZCenterpieceTheme.QUANTUM_HOLO) 0.78f else 0.95f
    return Color(litR, litG, litB, alpha)
}

/**
 * Draws the cybernetic grid background on the canvas.
 */
private fun DrawScope.drawCyberneticGrid(
    center: Offset,
    width: Float,
    height: Float,
    accentColor: Color
) {
    // Concentric radial distance rings
    listOf(0.35f, 0.65f, 0.95f).forEach { fraction ->
        drawCircle(
            color = accentColor.copy(alpha = 0.04f),
            radius = min(width, height) * 0.5f * fraction,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // Crosshair reference lines
    drawLine(
        color = accentColor.copy(alpha = 0.08f),
        start = Offset(center.x - 30.dp.toPx(), center.y),
        end = Offset(center.x + 30.dp.toPx(), center.y),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = accentColor.copy(alpha = 0.08f),
        start = Offset(center.x, center.y - 30.dp.toPx()),
        end = Offset(center.x, center.y + 30.dp.toPx()),
        strokeWidth = 1.dp.toPx()
    )
}

/**
 * Draws a 3D gyroscopic orbital ring projected to screen space.
 */
private fun DrawScope.drawOrbitalRing(
    center: Offset,
    radius: Float,
    rotX: Float,
    rotY: Float,
    cameraDistance: Float,
    focalLength: Float,
    viewScale: Float,
    theme: ZCenterpieceTheme
) {
    val segments = 24
    val projectedNodes = mutableListOf<ProjectedPoint>()

    for (i in 0 until segments) {
        val angle = (i * 2f * PI / segments).toFloat()
        val localPoint = Vector3(cos(angle) * 1.55f, 0f, sin(angle) * 1.55f)
        val rotated = Zama3DProjectionEngine.rotatePoint(localPoint, rotX, rotY, 0f)
        val proj = Zama3DProjectionEngine.project(rotated, cameraDistance, focalLength, center, viewScale)
        projectedNodes.add(proj)
    }

    for (i in 0 until segments) {
        val next = (i + 1) % segments
        val p1 = projectedNodes[i]
        val p2 = projectedNodes[next]

        if (p1.isVisible && p2.isVisible) {
            val depthAlpha = (1.5f / (p1.depth + 0.5f)).coerceIn(0.1f, 0.65f)
            drawLine(
                color = theme.accentColor.copy(alpha = depthAlpha),
                start = Offset(p1.screenX, p1.screenY),
                end = Offset(p2.screenX, p2.screenY),
                strokeWidth = 1.2.dp.toPx()
            )

            // Draw glowing node marker every 6 nodes
            if (i % 6 == 0) {
                drawCircle(
                    color = Color.White.copy(alpha = depthAlpha),
                    radius = 2.dp.toPx(),
                    center = Offset(p1.screenX, p1.screenY)
                )
            }
        }
    }
}

/**
 * Draws the internal pulsing quantum AI core (floating octahedron).
 */
private fun DrawScope.drawQuantumCore(
    center: Offset,
    coreVertices: List<Vector3>,
    coreFacets: List<Triple<Int, Int, Int>>,
    pulseScale: Float,
    rxRad: Float,
    ryRad: Float,
    cameraDistance: Float,
    focalLength: Float,
    viewScale: Float,
    theme: ZCenterpieceTheme
) {
    val scaledVertices = coreVertices.map { v ->
        Vector3(v.x * pulseScale, v.y * pulseScale, v.z * pulseScale)
    }

    val rotated = scaledVertices.map { v ->
        Zama3DProjectionEngine.rotatePoint(v, rxRad, ryRad, 0f)
    }

    val projected = rotated.map { v ->
        Zama3DProjectionEngine.project(v, cameraDistance, focalLength, center, viewScale)
    }

    for (facet in coreFacets) {
        val p0 = projected[facet.first]
        val p1 = projected[facet.second]
        val p2 = projected[facet.third]

        if (p0.isVisible && p1.isVisible && p2.isVisible) {
            val path = Path().apply {
                moveTo(p0.screenX, p0.screenY)
                lineTo(p1.screenX, p1.screenY)
                lineTo(p2.screenX, p2.screenY)
                close()
            }

            drawPath(
                path = path,
                color = theme.accentColor.copy(alpha = 0.35f),
                style = Fill
            )

            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.85f),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

// ============================================================================
// 6. UI HEADER & CONTROL DECK
// ============================================================================

@Composable
private fun CenterpieceHeader(
    activeTheme: ZCenterpieceTheme,
    rotX: Float,
    rotY: Float,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x1F00E5FF))
                    .border(1.dp, activeTheme.accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = "3D Z Engine",
                    tint = activeTheme.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = "3D Z VISUAL CENTERPIECE",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "YAW: ${rotY.toInt()}° • PITCH: ${rotX.toInt()}° // RAY SHADED",
                    color = activeTheme.accentColor,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }

        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0x1AFFFFFF))
                .testTag("btn_reset_centerpiece_cam")
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Reset Camera View",
                tint = ZamaChromeLight,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CenterpieceControlDeck(
    activeTheme: ZCenterpieceTheme,
    onSelectTheme: (ZCenterpieceTheme) -> Unit,
    autoSpinEnabled: Boolean,
    onToggleAutoSpin: () -> Unit,
    spinSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    zoomScale: Float,
    onZoomChange: (Float) -> Unit,
    showWireframe: Boolean,
    onToggleWireframe: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Theme Pills Row
        Text(
            text = "SHADING & MATERIAL THEME",
            color = ZamaChromeMid,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.6.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ZCenterpieceTheme.values().forEach { theme ->
                val isSelected = activeTheme == theme
                Surface(
                    color = if (isSelected) theme.accentColor.copy(alpha = 0.2f) else Color(0xFF0F1520),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) theme.accentColor else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable { onSelectTheme(theme) }
                        .testTag("theme_chip_${theme.name}")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = theme.displayName.take(8),
                            color = if (isSelected) theme.accentColor else ZamaChromeLight,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Toggles & Fine Adjustment Sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Auto Orbit Toggle
            Surface(
                color = if (autoSpinEnabled) Color(0x2200E5FF) else Color(0xFF0D121B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (autoSpinEnabled) ZamaElectricCyan else ZamaBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable { onToggleAutoSpin() }
                    .testTag("btn_toggle_auto_spin")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = if (autoSpinEnabled) ZamaElectricCyan else ZamaChromeMid,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (autoSpinEnabled) "AUTO-ORBIT" else "MANUAL",
                        color = if (autoSpinEnabled) ZamaElectricCyan else ZamaChromeLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Wireframe Toggle
            Surface(
                color = if (showWireframe) Color(0x2200E676) else Color(0xFF0D121B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (showWireframe) ZamaNeonGreen else ZamaBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable { onToggleWireframe() }
                    .testTag("btn_toggle_wireframe")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = if (showWireframe) ZamaNeonGreen else ZamaChromeMid,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showWireframe) "WIREFRAME" else "SOLID SHADED",
                        color = if (showWireframe) ZamaNeonGreen else ZamaChromeLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Zoom Scale Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ZOOM: ${(zoomScale * 100).toInt()}%",
                color = ZamaChromeMid,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(76.dp)
            )

            Slider(
                value = zoomScale,
                onValueChange = onZoomChange,
                valueRange = 0.75f..1.65f,
                colors = SliderDefaults.colors(
                    thumbColor = activeTheme.accentColor,
                    activeTrackColor = activeTheme.accentColor,
                    inactiveTrackColor = Color(0xFF1E2838)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("slider_centerpiece_zoom")
            )
        }
    }
}
