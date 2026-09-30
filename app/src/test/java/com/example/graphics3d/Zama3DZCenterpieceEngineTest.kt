package com.example.graphics3d

import com.example.ui.components.FacetCategory
import com.example.ui.components.Vector3
import com.example.ui.components.Zama3DMeshGenerator
import com.example.ui.components.Zama3DProjectionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class Zama3DZCenterpieceEngineTest {

    @Test
    fun vector3_operationsComputeAccurately() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(4f, 5f, 6f)

        val sum = a + b
        assertEquals(5f, sum.x, 0.001f)
        assertEquals(7f, sum.y, 0.001f)
        assertEquals(9f, sum.z, 0.001f)

        val dot = a.dot(b) // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        assertEquals(32f, dot, 0.001f)

        val cross = a.cross(b)
        // (2*6 - 3*5, 3*4 - 1*6, 1*5 - 2*4) = (-3, 6, -3)
        assertEquals(-3f, cross.x, 0.001f)
        assertEquals(6f, cross.y, 0.001f)
        assertEquals(-3f, cross.z, 0.001f)

        val norm = cross.normalized()
        assertEquals(1f, norm.length(), 0.001f)
    }

    @Test
    fun zama3DMeshGenerator_createsCompleteZGeometry() {
        val vertices = Zama3DMeshGenerator.createZVertices(thickness = 0.4f)
        // 10 front vertices + 10 back vertices
        assertEquals(20, vertices.size)

        // Front vertices should have positive Z, back should have negative Z
        for (i in 0 until 10) {
            assertTrue("Front vertex $i should have positive z", vertices[i].z > 0f)
            assertTrue("Back vertex ${i + 10} should have negative z", vertices[i + 10].z < 0f)
            // X and Y coordinates must match for extrusion alignment
            assertEquals("X alignment at index $i", vertices[i].x, vertices[i + 10].x, 0.001f)
            assertEquals("Y alignment at index $i", vertices[i].y, vertices[i + 10].y, 0.001f)
        }

        val facets = Zama3DMeshGenerator.createZFacets()
        // 8 front + 8 back + 20 perimeter = 36 triangulated facets
        assertEquals(36, facets.size)

        // Verify categories exist
        val frontFacets = facets.filter { it.facetCategory == FacetCategory.FRONT_CAP }
        val backFacets = facets.filter { it.facetCategory == FacetCategory.BACK_CAP }
        val sideFacets = facets.filter { it.facetCategory != FacetCategory.FRONT_CAP && it.facetCategory != FacetCategory.BACK_CAP }

        assertEquals(8, frontFacets.size)
        assertEquals(8, backFacets.size)
        assertEquals(20, sideFacets.size)
    }

    @Test
    fun zama3DProjectionEngine_rotatesAndProjectsAccurately() {
        val original = Vector3(1f, 0f, 0f)
        // Rotate 90 degrees around Y axis (pi/2)
        val rotated = Zama3DProjectionEngine.rotatePoint(original, rx = 0f, ry = (PI / 2.0).toFloat(), rz = 0f)

        // cos(pi/2) ~ 0, -sin(pi/2) ~ -1
        assertEquals(0f, rotated.x, 0.001f)
        assertEquals(0f, rotated.y, 0.001f)
        assertEquals(-1f, rotated.z, 0.001f)

        // Projection
        val proj = Zama3DProjectionEngine.project(
            point = Vector3(0f, 0f, 0f),
            cameraDistance = 4f,
            focalLength = 2f,
            center = androidx.compose.ui.geometry.Offset(200f, 300f),
            viewScale = 100f
        )

        assertTrue("Projected point should be visible in front of camera", proj.isVisible)
        assertEquals(200f, proj.screenX, 0.001f)
        assertEquals(300f, proj.screenY, 0.001f)
        assertEquals(4f, proj.depth, 0.001f)
    }

    @Test
    fun coreOctahedron_generatesSymmetricGeometry() {
        val coreVertices = Zama3DMeshGenerator.createCoreOctahedronVertices(scale = 0.3f)
        assertEquals(6, coreVertices.size)

        val coreFacets = Zama3DMeshGenerator.createCoreOctahedronFacets()
        assertEquals(8, coreFacets.size)
    }
}
