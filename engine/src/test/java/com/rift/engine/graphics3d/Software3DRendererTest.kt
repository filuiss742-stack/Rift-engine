package com.rift.engine.graphics3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun groundPlaneContainsTwoTriangles() {
        val mesh = groundPlaneMesh(Vector3.ZERO, 10f, 10f, 0xFFFFFFFF.toInt())
        assertEquals(2, mesh.triangles.size)
    }
}
