package com.example.ui.components

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaBorderGlow
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaVoid
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D Mesh Representation for the Zama 3D Z Object.
 * Includes vertex position coordinates, surface normal vectors, colors, and line indices.
 */
data class ZamaMeshGeometry(
    val vertexBuffer: FloatBuffer,
    val normalBuffer: FloatBuffer,
    val colorBuffer: FloatBuffer,
    val lineIndexBuffer: ShortBuffer,
    val triangleCount: Int,
    val lineCount: Int,
    val rawTriangles: List<MeshTriangle>
)

data class Vec3D(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3D) = Vec3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3D) = Vec3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = Vec3D(x * scale, y * scale, z * scale)

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3D {
        val len = length()
        return if (len > 0.0001f) Vec3D(x / len, y / len, z / len) else Vec3D(0f, 1f, 0f)
    }

    fun cross(other: Vec3D): Vec3D = Vec3D(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun dot(other: Vec3D): Float = x * other.x + y * other.y + z * other.z
}

data class MeshTriangle(
    val v1: Vec3D,
    val v2: Vec3D,
    val v3: Vec3D,
    val normal: Vec3D,
    val baseColor: Color
)

/**
 * Procedural Generator for the Watertight 3D Z Object Mesh.
 */
object ZamaMeshFactory {
    fun createZObjectMesh(
        w: Float = 0.72f,
        h: Float = 0.90f,
        barH: Float = 0.30f,
        diagW: Float = 0.36f,
        depth: Float = 0.24f
    ): ZamaMeshGeometry {
        val triangles = mutableListOf<MeshTriangle>()

        // Colors
        val frontColor = Color(0xFF1B202A)
        val backColor = Color(0xFF0D1016)
        val topSideColor = Color(0xFFC0C7D5)
        val sideColor = Color(0xFF4A5261)
        val diagSideColor = Color(0xFF232B38)

        // 12 Front Boundary Vertices (Z = +depth)
        val fV = listOf(
            Vec3D(-w, h, depth),                   // 0: top-left
            Vec3D(w - diagW, h, depth),            // 1: top-bar seam
            Vec3D(w, h, depth),                    // 2: top-right
            Vec3D(w, h - barH, depth),             // 3: top-bar bottom-right / diag top-right
            Vec3D(w - diagW, h - barH, depth),     // 4: top-bar bottom seam / diag top-left
            Vec3D(-w, h - barH, depth),            // 5: top-bar bottom-left shelf
            Vec3D(-w, -h + barH, depth),           // 6: bottom-bar top-left / diag bottom-left
            Vec3D(-w + diagW, -h + barH, depth),   // 7: bottom-bar top seam / diag bottom-right
            Vec3D(w, -h + barH, depth),            // 8: bottom-bar top-right shelf
            Vec3D(w, -h, depth),                   // 9: bottom-right
            Vec3D(-w + diagW, -h, depth),          // 10: bottom-bar seam
            Vec3D(-w, -h, depth)                   // 11: bottom-left
        )

        // 12 Back Boundary Vertices (Z = -depth)
        val bV = fV.map { Vec3D(it.x, it.y, -depth) }

        fun addQuad(v0: Vec3D, v1: Vec3D, v2: Vec3D, v3: Vec3D, color: Color, customNormal: Vec3D? = null) {
            val normal = customNormal ?: run {
                val edge1 = v1 - v0
                val edge2 = v2 - v0
                edge1.cross(edge2).normalized()
            }
            triangles.add(MeshTriangle(v0, v1, v2, normal, color))
            triangles.add(MeshTriangle(v0, v2, v3, normal, color))
        }

        // --- FRONT FACE (Z = +depth, Normal = 0, 0, 1) ---
        val frontNormal = Vec3D(0f, 0f, 1f)
        // Top bar: left quad (0, 1, 4, 5) & right quad (1, 2, 3, 4)
        addQuad(fV[0], fV[1], fV[4], fV[5], frontColor, frontNormal)
        addQuad(fV[1], fV[2], fV[3], fV[4], frontColor, frontNormal)
        // Diagonal stem parallelogram: (4, 3, 7, 6)
        addQuad(fV[4], fV[3], fV[7], fV[6], frontColor, frontNormal)
        // Bottom bar: left quad (6, 7, 10, 11) & right quad (7, 8, 9, 10)
        addQuad(fV[6], fV[7], fV[10], fV[11], frontColor, frontNormal)
        addQuad(fV[7], fV[8], fV[9], fV[10], frontColor, frontNormal)

        // --- BACK FACE (Z = -depth, Normal = 0, 0, -1) ---
        val backNormal = Vec3D(0f, 0f, -1f)
        addQuad(bV[0], bV[5], bV[4], bV[1], backColor, backNormal)
        addQuad(bV[1], bV[4], bV[3], bV[2], backColor, backNormal)
        addQuad(bV[4], bV[6], bV[7], bV[3], backColor, backNormal)
        addQuad(bV[6], bV[11], bV[10], bV[7], backColor, backNormal)
        addQuad(bV[7], bV[10], bV[9], bV[8], backColor, backNormal)

        // --- EXTRUDED PERIMETER WALLS (12 side quads) ---
        val perimeterIndices = listOf(
            Pair(0, 1) to topSideColor,
            Pair(1, 2) to topSideColor,
            Pair(2, 3) to sideColor,
            Pair(3, 7) to diagSideColor,
            Pair(7, 8) to sideColor,
            Pair(8, 9) to sideColor,
            Pair(9, 10) to sideColor,
            Pair(10, 11) to sideColor,
            Pair(11, 6) to sideColor,
            Pair(6, 4) to diagSideColor,
            Pair(4, 5) to sideColor,
            Pair(5, 0) to sideColor
        )

        for ((edge, col) in perimeterIndices) {
            val i0 = edge.first
            val i1 = edge.second
            // Side quad connecting Front(i0, i1) to Back(i1, i0)
            addQuad(fV[i0], fV[i1], bV[i1], bV[i0], col)
        }

        // Convert to NIO Direct Buffers for OpenGL ES 2.0
        val totalVertices = triangles.size * 3
        val vBuffer = ByteBuffer.allocateDirect(totalVertices * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        val nBuffer = ByteBuffer.allocateDirect(totalVertices * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        val cBuffer = ByteBuffer.allocateDirect(totalVertices * 4 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()

        // Wireframe Line Indices (3 line segments per triangle)
        val lineIndices = mutableListOf<Short>()
        var vertexIndex: Short = 0

        for (tri in triangles) {
            val pts = listOf(tri.v1, tri.v2, tri.v3)
            val baseIdx = vertexIndex

            for (p in pts) {
                vBuffer.put(p.x).put(p.y).put(p.z)
                nBuffer.put(tri.normal.x).put(tri.normal.y).put(tri.normal.z)
                cBuffer.put(tri.baseColor.red)
                    .put(tri.baseColor.green)
                    .put(tri.baseColor.blue)
                    .put(tri.baseColor.alpha)
            }

            // Lines: (v0-v1), (v1-v2), (v2-v0)
            lineIndices.add(baseIdx)
            lineIndices.add((baseIdx + 1).toShort())
            lineIndices.add((baseIdx + 1).toShort())
            lineIndices.add((baseIdx + 2).toShort())
            lineIndices.add((baseIdx + 2).toShort())
            lineIndices.add(baseIdx)

            vertexIndex = (vertexIndex + 3).toShort()
        }

        vBuffer.position(0)
        nBuffer.position(0)
        cBuffer.position(0)

        val lBuffer = ByteBuffer.allocateDirect(lineIndices.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        lineIndices.forEach { lBuffer.put(it) }
        lBuffer.position(0)

        return ZamaMeshGeometry(
            vertexBuffer = vBuffer,
            normalBuffer = nBuffer,
            colorBuffer = cBuffer,
            lineIndexBuffer = lBuffer,
            triangleCount = triangles.size,
            lineCount = lineIndices.size / 2,
            rawTriangles = triangles
        )
    }
}

/**
 * OpenGL ES 2.0 Renderer implementation for the 3D Z Object.
 */
class ZamaGlRenderer(
    private val mesh: ZamaMeshGeometry
) : GLSurfaceView.Renderer {

    // Dynamic state controlled by UI
    var rotX: Float = 15f
    var rotY: Float = -25f
    var zoom: Float = 1.0f
    var renderMode: Int = 0 // 0: Obsidian, 1: Cyan, 2: Normal Map, 3: Prism
    var wireframeOverlay: Boolean = false
    var explodeAmount: Float = 0.0f
    var lightAngle: Float = 45f

    // Matrices
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val normalMatrix = FloatArray(16)

    // GL Program & handles
    private var programId: Int = 0
    private var aPositionLoc = -1
    private var aNormalLoc = -1
    private var aColorLoc = -1
    private var uMVPLoc = -1
    private var uModelLoc = -1
    private var uNormalMatrixLoc = -1
    private var uLightPosLoc = -1
    private var uRenderModeLoc = -1
    private var uSpecularPowerLoc = -1
    private var uRimGlowLoc = -1
    private var uExplodeLoc = -1

    var isGlInitialized = false
        private set

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.025f, 0.035f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        // Compile Shader Pipeline
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform mat4 uNormalMatrix;
            uniform float uExplode;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec4 aColor;
            varying vec3 vNormal;
            varying vec3 vWorldPos;
            varying vec4 vColor;

            void main() {
                // Explode along face normal vector
                vec3 explodedPos = aPosition + aNormal * uExplode;
                gl_Position = uMVPMatrix * vec4(explodedPos, 1.0);
                vNormal = normalize(vec3(uNormalMatrix * vec4(aNormal, 0.0)));
                vWorldPos = vec3(uModelMatrix * vec4(explodedPos, 1.0));
                vColor = aColor;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            varying vec3 vNormal;
            varying vec3 vWorldPos;
            varying vec4 vColor;

            uniform vec3 uLightPos;
            uniform int uRenderMode;
            uniform float uSpecularPower;
            uniform float uRimGlow;

            void main() {
                vec3 N = normalize(vNormal);
                vec3 L = normalize(uLightPos - vWorldPos);
                vec3 V = normalize(-vWorldPos);

                if (uRenderMode == 2) {
                    // Normal diagnostic visualization (RGB = XYZ normal)
                    gl_FragColor = vec4(N * 0.5 + 0.5, 1.0);
                    return;
                }

                float diff = max(dot(N, L), 0.0);
                vec3 H = normalize(L + V);
                float spec = pow(max(dot(N, H), 0.0), uSpecularPower);
                float rim = pow(1.0 - max(dot(N, V), 0.0), 2.2) * uRimGlow;

                if (uRenderMode == 0) {
                    // Obsidian Chrome with Electric Cyan rim and specular shine
                    vec3 ambient = vec3(0.04, 0.06, 0.09);
                    vec3 diffuse = vec3(0.18, 0.22, 0.28) * diff;
                    vec3 specular = vec3(0.95, 0.98, 1.0) * spec * 1.5;
                    vec3 rimColor = vec3(0.0, 0.9, 1.0) * rim * 1.2;
                    gl_FragColor = vec4(ambient + diffuse + specular + rimColor, 1.0);
                } else if (uRenderMode == 1) {
                    // Cyber Electric Cyan Laser
                    vec3 cyan = vec3(0.0, 0.9, 1.0);
                    vec3 ambient = cyan * 0.22;
                    vec3 diffuse = cyan * (diff * 0.7);
                    vec3 specular = vec3(1.0) * spec;
                    vec3 rimColor = cyan * rim * 1.4;
                    gl_FragColor = vec4(ambient + diffuse + specular + rimColor, 0.95);
                } else {
                    // Prism Holographic
                    vec3 prism = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + N.y * 3.0 + diff * 3.14);
                    vec3 specular = vec3(1.0) * spec;
                    gl_FragColor = vec4(prism * (diff * 0.65 + 0.35) + specular, 1.0);
                }
            }
        """.trimIndent()

        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES20.glCreateProgram().also { p ->
            GLES20.glAttachShader(p, vShader)
            GLES20.glAttachShader(p, fShader)
            GLES20.glLinkProgram(p)
        }

        aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalLoc = GLES20.glGetAttribLocation(programId, "aNormal")
        aColorLoc = GLES20.glGetAttribLocation(programId, "aColor")

        uMVPLoc = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uModelLoc = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uNormalMatrixLoc = GLES20.glGetUniformLocation(programId, "uNormalMatrix")
        uLightPosLoc = GLES20.glGetUniformLocation(programId, "uLightPos")
        uRenderModeLoc = GLES20.glGetUniformLocation(programId, "uRenderMode")
        uSpecularPowerLoc = GLES20.glGetUniformLocation(programId, "uSpecularPower")
        uRimGlowLoc = GLES20.glGetUniformLocation(programId, "uRimGlow")
        uExplodeLoc = GLES20.glGetUniformLocation(programId, "uExplode")

        isGlInitialized = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projectionMatrix, 0, 42f, aspect, 0.5f, 50f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (programId == 0) return

        GLES20.glUseProgram(programId)

        // Set View Matrix (Camera distance adjusted by zoom)
        val cameraDist = 4.2f / zoom.coerceIn(0.5f, 2.5f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, cameraDist, 0f, 0f, 0f, 0f, 1f, 0f)

        // Set Model Matrix (Euler Rotations)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)

        // Normal Matrix (inverse transpose of model matrix)
        val inverted = FloatArray(16)
        Matrix.invertM(inverted, 0, modelMatrix, 0)
        Matrix.transposeM(normalMatrix, 0, inverted, 0)

        // Model-View-Projection Matrix
        val mvMatrix = FloatArray(16)
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        // Uniforms
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uNormalMatrixLoc, 1, false, normalMatrix, 0)

        // Lighting position calculated from lightAngle
        val lightRad = (lightAngle * PI / 180f).toFloat()
        val lx = cos(lightRad) * 3.5f
        val ly = 3.0f
        val lz = sin(lightRad) * 3.5f
        GLES20.glUniform3f(uLightPosLoc, lx, ly, lz)

        GLES20.glUniform1i(uRenderModeLoc, renderMode)
        GLES20.glUniform1f(uSpecularPowerLoc, 36.0f)
        GLES20.glUniform1f(uRimGlowLoc, 1.2f)
        GLES20.glUniform1f(uExplodeLoc, explodeAmount)

        // Attributes
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, mesh.vertexBuffer)

        GLES20.glEnableVertexAttribArray(aNormalLoc)
        GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, 0, mesh.normalBuffer)

        GLES20.glEnableVertexAttribArray(aColorLoc)
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, mesh.colorBuffer)

        // Render Solid Mesh Triangles
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.triangleCount * 3)

        // Optional Wireframe Overlay
        if (wireframeOverlay) {
            GLES20.glUniform1i(uRenderModeLoc, 1) // Cyan color for wireframe
            GLES20.glLineWidth(2.0f)
            GLES20.glDrawElements(
                GLES20.GL_LINES,
                mesh.lineCount * 2,
                GLES20.GL_UNSIGNED_SHORT,
                mesh.lineIndexBuffer
            )
        }

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aNormalLoc)
        GLES20.glDisableVertexAttribArray(aColorLoc)
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}

/**
 * High-performance Composable component implementing OpenGL ES 2.0 & 3D Mesh Framework
 * to render the iconic sculpted 3D Z object with full interactive orbit, shader modes,
 * exploding mesh views, and real-time telemetry.
 */
@Composable
fun ZamaGlMeshVisualizer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Geometry Mesh (Generated once and cached)
    val mesh = remember { ZamaMeshFactory.createZObjectMesh() }

    // Interactive Viewport State
    var rotX by remember { mutableFloatStateOf(16f) }
    var rotY by remember { mutableFloatStateOf(-30f) }
    var zoomFactor by remember { mutableFloatStateOf(1.0f) }
    var autoOrbit by remember { mutableStateOf(true) }
    var renderModeIndex by remember { mutableIntStateOf(0) } // 0: Obsidian Chrome, 1: Cyan Laser, 2: Normal Vectors, 3: Prism Hologram
    var wireframeOverlay by remember { mutableStateOf(false) }
    var explodeAmount by remember { mutableFloatStateOf(0.0f) } // 0.0 to 0.45f
    var lightAngle by remember { mutableFloatStateOf(45f) }
    var engineMode by remember { mutableIntStateOf(0) } // 0: OpenGL ES 2.0 (GLES), 1: 3D Mesh Software Rasterizer

    // Continuous auto-orbit rotation
    val infiniteTransition = rememberInfiniteTransition(label = "gl_orbit_transition")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_orbit"
    )

    val activeRotY = if (autoOrbit) (rotY + orbitAngle) % 360f else rotY

    // GL Renderer instance
    val glRenderer = remember(mesh) {
        ZamaGlRenderer(mesh)
    }

    // Keep GL Renderer synchronized with Compose state
    LaunchedEffect(rotX, activeRotY, zoomFactor, renderModeIndex, wireframeOverlay, explodeAmount, lightAngle) {
        glRenderer.rotX = rotX
        glRenderer.rotY = activeRotY
        glRenderer.zoom = zoomFactor
        glRenderer.renderMode = renderModeIndex
        glRenderer.wireframeOverlay = wireframeOverlay
        glRenderer.explodeAmount = explodeAmount
        glRenderer.lightAngle = lightAngle
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
            .testTag("zama_gl_mesh_visualizer")
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
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OPENGL ES 2.0 / 3D MESH FRAMEWORK",
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
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ZamaNeonGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (engineMode == 0) "GPU ACCELERATED" else "MESH RASTERIZER",
                        color = ZamaNeonGreen,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title & Description
        Text(
            text = "THE 3D Z SCULPTURE",
            color = ZamaChromeLight,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Real-time hardware-accelerated polygonal mesh of the monolithic Zama Z. Sculpted with 44 indexed planar faces, specular chrome reflections, and normal-extruded geometry.",
            color = ZamaChromeMid,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pipeline Mode Switcher (OpenGL ES vs 3D Mesh Engine)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF10141D))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val engines = listOf("OpenGL ES 2.0 (GLES)", "3D Mesh Rasterizer")
            engines.forEachIndexed { index, label ->
                val isSelected = engineMode == index
                Surface(
                    onClick = { engineMode = index },
                    color = if (isSelected) Color(0x3300E5FF) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ZamaElectricCyan) else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("engine_mode_tab_$index")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) ZamaElectricCyan else ZamaChromeMid,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3D Viewport Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF06080D))
                .border(1.dp, ZamaBorderGlow, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        autoOrbit = false
                        rotY = (rotY + dragAmount.x * 0.55f) % 360f
                        rotX = (rotX - dragAmount.y * 0.45f).coerceIn(-85f, 85f)
                    }
                }
                .testTag("gl_viewport_box")
        ) {
            // Background Subtle Tech Grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 32f
                val w = size.width
                val h = size.height
                var x = 0f
                while (x < w) {
                    drawLine(
                        color = Color(0x0C00E5FF),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                    x += step
                }
                var y = 0f
                while (y < h) {
                    drawLine(
                        color = Color(0x0C00E5FF),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                    y += step
                }

                // Ambient Radial Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x3300E5FF),
                            Color(0x112979FF),
                            Color.Transparent
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f,
                    center = Offset(w / 2f, h / 2f)
                )
            }

            // Engine Rendering View
            val isInspectionMode = LocalInspectionMode.current
            if (engineMode == 0 && !isInspectionMode) {
                // HARDWARE OPENGL ES 2.0 (GLSurfaceView)
                AndroidView(
                    factory = { ctx ->
                        GLSurfaceView(ctx).apply {
                            setEGLContextClientVersion(2)
                            setRenderer(glRenderer)
                            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        // Triggers continuous redraw
                        view.requestRender()
                    }
                )
            } else {
                // SOFTWARE 3D MESH RASTERIZER (Fallback & Vector Mesh Engine)
                SoftwareMeshCanvas(
                    mesh = mesh,
                    rotX = rotX,
                    rotY = activeRotY,
                    zoom = zoomFactor,
                    renderMode = renderModeIndex,
                    wireframeOverlay = wireframeOverlay,
                    explodeAmount = explodeAmount,
                    lightAngle = lightAngle,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Live Telemetry HUD Overlay (Top-Left)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(Color(0xD9080A0E), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "FACES: ${mesh.triangleCount} TRIANGLES",
                    color = ZamaChromeLight,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "VERTICES: ${mesh.triangleCount * 3}",
                    color = ZamaElectricCyan,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "YAW: ${activeRotY.toInt()}° | PITCH: ${rotX.toInt()}°",
                    color = ZamaChromeMid,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Quick Viewport Reset / Orbit Button (Top-Right)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = {
                        autoOrbit = !autoOrbit
                    },
                    color = if (autoOrbit) Color(0x3300E5FF) else Color(0xD9080A0E),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (autoOrbit) ZamaElectricCyan else Color(0x33FFFFFF)),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("toggle_auto_orbit_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Auto Orbit",
                            tint = if (autoOrbit) ZamaElectricCyan else ZamaChromeMid,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        rotX = 16f
                        rotY = -30f
                        zoomFactor = 1.0f
                        explodeAmount = 0.0f
                    },
                    color = Color(0xD9080A0E),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("reset_gl_camera_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Camera",
                            tint = ZamaChromeMid,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shader Material Mode Selector
        Text(
            text = "SHADER MATERIAL MODE",
            color = ZamaChromeMid,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val modes = listOf(
                "Obsidian",
                "Cyan Laser",
                "Normal Map",
                "Prism"
            )
            modes.forEachIndexed { idx, title ->
                val isSelected = renderModeIndex == idx
                Surface(
                    onClick = { renderModeIndex = idx },
                    color = if (isSelected) Color(0x3300E5FF) else Color(0xFF121620),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ZamaElectricCyan else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shader_mode_pill_$idx")
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) ZamaElectricCyan else ZamaChromeMid,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 7.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Explode Mesh Disassembly Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = ZamaElectricCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EXPLODE MESH FACES (NORMAL OFFSET)",
                    color = ZamaChromeLight,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${(explodeAmount * 100).toInt()}%",
                color = ZamaElectricCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = explodeAmount,
            onValueChange = { explodeAmount = it },
            valueRange = 0.0f..0.40f,
            colors = SliderDefaults.colors(
                thumbColor = ZamaElectricCyan,
                activeTrackColor = ZamaElectricCyan,
                inactiveTrackColor = Color(0xFF1E2433)
            ),
            modifier = Modifier.testTag("explode_mesh_slider")
        )

        // Mesh Toggles Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { wireframeOverlay = !wireframeOverlay }
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = null,
                    tint = if (wireframeOverlay) ZamaElectricCyan else ZamaChromeMid,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wireframe Lattice Overlay",
                    color = if (wireframeOverlay) ZamaElectricCyan else ZamaChromeMid,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Switch(
                checked = wireframeOverlay,
                onCheckedChange = { wireframeOverlay = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ZamaVoid,
                    checkedTrackColor = ZamaElectricCyan,
                    uncheckedThumbColor = ZamaChromeMid,
                    uncheckedTrackColor = Color(0xFF1A202C)
                ),
                modifier = Modifier.testTag("wireframe_switch")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lighting Direction Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIGHT SOURCE ANGLE",
                color = ZamaGraphite,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${lightAngle.toInt()}°",
                color = ZamaChromeMid,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Slider(
            value = lightAngle,
            onValueChange = { lightAngle = it },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = ZamaChromeLight,
                activeTrackColor = ZamaChromeMid,
                inactiveTrackColor = Color(0xFF1E2433)
            ),
            modifier = Modifier.testTag("light_angle_slider")
        )
    }
}

/**
 * Software 3D Mesh Engine for immediate rendering, fallback mode,
 * and environments where OpenGL ES hardware context might not be available.
 */
@Composable
private fun SoftwareMeshCanvas(
    mesh: ZamaMeshGeometry,
    rotX: Float,
    rotY: Float,
    zoom: Float,
    renderMode: Int,
    wireframeOverlay: Boolean,
    explodeAmount: Float,
    lightAngle: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = (size.width * 0.42f) * zoom

        val radX = (rotX * PI / 180f).toFloat()
        val radY = (rotY * PI / 180f).toFloat()
        val cosX = cos(radX)
        val sinX = sin(radX)
        val cosY = cos(radY)
        val sinY = sin(radY)

        val lightRad = (lightAngle * PI / 180f).toFloat()
        val lightDir = Vec3D(cos(lightRad) * 0.8f, 0.9f, sin(lightRad) * 0.8f).normalized()
        val viewDir = Vec3D(0f, 0f, 1f)

        // Project and depth-sort all 44 triangles
        data class ProjectedTriangle(
            val p1: Offset,
            val p2: Offset,
            val p3: Offset,
            val depthZ: Float,
            val color: Color,
            val normal: Vec3D,
            val isBackface: Boolean
        )

        fun transformVec(v: Vec3D): Vec3D {
            // Rotate Y
            val x1 = v.x * cosY + v.z * sinY
            val z1 = -v.x * sinY + v.z * cosY
            // Rotate X
            val y2 = v.y * cosX - z1 * sinX
            val z2 = v.y * sinX + z1 * cosX
            return Vec3D(x1, y2, z2)
        }

        fun projectPoint(v: Vec3D): Offset {
            val cameraDist = 3.6f
            val projDist = cameraDist + v.z
            val factor = if (projDist > 0.1f) 3.6f / projDist else 1f
            val px = cx + v.x * scale * factor
            val py = cy - v.y * scale * factor
            return Offset(px, py)
        }

        val projectedList = mesh.rawTriangles.map { tri ->
            // Displace along face normal if explode is active
            val displacedV1 = tri.v1 + tri.normal * explodeAmount
            val displacedV2 = tri.v2 + tri.normal * explodeAmount
            val displacedV3 = tri.v3 + tri.normal * explodeAmount

            val tV1 = transformVec(displacedV1)
            val tV2 = transformVec(displacedV2)
            val tV3 = transformVec(displacedV3)
            val tNormal = transformVec(tri.normal).normalized()

            val p1 = projectPoint(tV1)
            val p2 = projectPoint(tV2)
            val p3 = projectPoint(tV3)

            val avgZ = (tV1.z + tV2.z + tV3.z) / 3f

            // Lighting computation
            val diff = tNormal.dot(lightDir).coerceAtLeast(0f)
            val halfDir = (lightDir + viewDir).normalized()
            val spec = Math.pow(tNormal.dot(halfDir).coerceAtLeast(0f).toDouble(), 24.0).toFloat()
            val rim = Math.pow((1f - tNormal.dot(viewDir).coerceAtLeast(0f)).toDouble(), 2.0).toFloat()

            val faceColor = when (renderMode) {
                2 -> {
                    // Normal diagnostic colors (RGB)
                    Color(
                        red = (tNormal.x * 0.5f + 0.5f).coerceIn(0f, 1f),
                        green = (tNormal.y * 0.5f + 0.5f).coerceIn(0f, 1f),
                        blue = (tNormal.z * 0.5f + 0.5f).coerceIn(0f, 1f)
                    )
                }
                1 -> {
                    // Cyber Cyan Laser
                    val cyan = Color(0xFF00E5FF)
                    val r = (cyan.red * (diff * 0.6f + 0.3f) + spec * 0.6f).coerceIn(0f, 1f)
                    val g = (cyan.green * (diff * 0.6f + 0.3f) + spec * 0.6f).coerceIn(0f, 1f)
                    val b = (cyan.blue * (diff * 0.6f + 0.3f) + spec * 0.6f).coerceIn(0f, 1f)
                    Color(r, g, b)
                }
                3 -> {
                    // Holographic Prism
                    val r = (0.5f + 0.5f * cos(tNormal.y * 3f) * diff + spec).coerceIn(0f, 1f)
                    val g = (0.5f + 0.5f * cos(tNormal.y * 3f + 2f) * diff + spec).coerceIn(0f, 1f)
                    val b = (0.5f + 0.5f * cos(tNormal.y * 3f + 4f) * diff + spec).coerceIn(0f, 1f)
                    Color(r, g, b)
                }
                else -> {
                    // Obsidian Chrome with Cyan rim
                    val base = tri.baseColor
                    val r = (base.red * (diff * 0.7f + 0.2f) + spec * 0.8f).coerceIn(0f, 1f)
                    val g = (base.green * (diff * 0.7f + 0.2f) + spec * 0.8f + rim * 0.35f).coerceIn(0f, 1f)
                    val b = (base.blue * (diff * 0.7f + 0.2f) + spec * 0.8f + rim * 0.65f).coerceIn(0f, 1f)
                    Color(r, g, b)
                }
            }

            ProjectedTriangle(
                p1 = p1,
                p2 = p2,
                p3 = p3,
                depthZ = avgZ,
                color = faceColor,
                normal = tNormal,
                isBackface = tNormal.z < 0f
            )
        }

        // Sort triangles from farthest back to nearest front (Painters Algorithm)
        val sortedTriangles = projectedList.sortedBy { it.depthZ }

        // Render sorted triangles
        for (tri in sortedTriangles) {
            val path = Path().apply {
                moveTo(tri.p1.x, tri.p1.y)
                lineTo(tri.p2.x, tri.p2.y)
                lineTo(tri.p3.x, tri.p3.y)
                close()
            }

            drawPath(
                path = path,
                color = tri.color
            )

            // Wireframe outline
            if (wireframeOverlay) {
                drawPath(
                    path = path,
                    color = ZamaElectricCyan.copy(alpha = 0.45f),
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
