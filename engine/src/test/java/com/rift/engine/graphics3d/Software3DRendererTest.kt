package com.rift.engine.graphics3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.rift.engine.graphics.TextureResource
import org.junit.Test

class Software3DRendererTest {
    private val renderer = Software3DRenderer()
    private val camera = Camera3D(
        position = Vector3(0f, 2f, 6f),
        target = Vector3.ZERO,
        aspectRatio = 16f / 9f
    )

    @Test
    fun projectsVisibleWorldPointIntoViewport() {
        val projected = renderer.project(Vector3.ZERO, camera, 960, 540)
        assertNotNull(projected)
        assertEquals(480f, projected!!.first, 0.01f)
        org.junit.Assert.assertTrue(projected.second in 0f..540f)
    }

    @Test
    fun rejectsPointsBehindNearPlane() {
        assertNull(renderer.project(Vector3(0f, 2f, 6f), camera, 960, 540))
    }

    @Test
    fun cuboidContainsTwelveTrianglesForSixFaces() {
        val mesh = cuboidMesh(Vector3.ZERO, Vector3(2f, 1f, 3f), top = 0xFFFFFFFF.toInt())
        assertEquals(12, mesh.triangles.size)
    }

    @Test
    fun texturedCuboidAssignsIndependentMaterialsToAllSixFaces() {
        val textures = List(6) { TestTexture() }
        val materials = CuboidMaterialSet(
            textures[0], textures[1], textures[2],
            textures[3], textures[4], textures[5]
        )
        val mesh = texturedCuboidMesh(Vector3.ZERO, Vector3(2f, 1f, 3f), materials)

        assertEquals(12, mesh.triangles.size)
        assertEquals(6, mesh.triangles.mapNotNull { it.texture }.distinctBy { System.identityHashCode(it) }.size)
        assertTrue(mesh.triangles.all { it.uvA.u.isFinite() && it.uvB.v.isFinite() && it.uvC.u.isFinite() })
    }

    @Test
    fun texturedGroundPlaneSupportsRepeatedUvCoordinates() {
        val texture = TestTexture()
        val mesh = texturedGroundPlaneMesh(Vector3.ZERO, 10f, 8f, texture, 4f, 3f)

        assertEquals(2, mesh.triangles.size)
        assertTrue(mesh.triangles.all { it.texture === texture })
        assertEquals(4f, mesh.triangles[0].uvB.u, 0f)
        assertEquals(3f, mesh.triangles[0].uvC.v, 0f)
    }

    private class TestTexture : TextureResource {
        override val width = 64
        override val height = 64
        override var isDisposed = false
        override fun close() { isDisposed = true }
    }

    @Test
    fun groundPlaneContainsTwoTriangles() {
        val mesh = groundPlaneMesh(Vector3.ZERO, 10f, 10f, 0xFFFFFFFF.toInt())
        assertEquals(2, mesh.triangles.size)
    }
}
