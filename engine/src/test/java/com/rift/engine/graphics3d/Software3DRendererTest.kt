package com.rift.engine.graphics3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.rift.engine.graphics.TextureResource
import com.rift.engine.graphics.RenderContext
import java.lang.reflect.Proxy
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
    fun texturedCuboidSupportsRepeatedUvCoordinatesOnEveryFace() {
        val texture = TestTexture()
        val materials = CuboidMaterialSet(texture, texture, texture, texture, texture, texture)
        val mesh = texturedCuboidMesh(Vector3.ZERO, Vector3(2f, 1f, 3f), materials, 4f, 2.5f)

        assertEquals(12, mesh.triangles.size)
        val coordinates = mesh.triangles.flatMap { listOf(it.uvA, it.uvB, it.uvC) }
        assertEquals(4f, coordinates.maxOf { it.u }, 0f)
        assertEquals(2.5f, coordinates.maxOf { it.v }, 0f)
        assertTrue(coordinates.all { it.u in 0f..4f && it.v in 0f..2.5f })
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

    @Test
    fun rendererForwardsMeshUvsToTexturedPlatformPrimitive() {
        val texture = TestTexture()
        val calls = mutableListOf<Array<Any?>>()
        val context = Proxy.newProxyInstance(
            RenderContext::class.java.classLoader,
            arrayOf(RenderContext::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getWidth" -> 640
                "getHeight" -> 360
                "drawTexturedTriangle" -> { calls.add(args?.toList()?.toTypedArray() ?: emptyArray()); null }
                "drawTriangle" -> null
                else -> null
            }
        } as RenderContext
        val camera = Camera3D(
            position = Vector3(0f, 0f, 5f),
            target = Vector3.ZERO,
            aspectRatio = 16f / 9f
        )
        val triangle = MeshTriangle(
            Vector3(-1f,-1f,0f),
            Vector3(1f,-1f,0f),
            Vector3(0f,1f,0f),
            0xFFFFFFFF.toInt(),
            texture,
            TextureUv(.1f,.2f),
            TextureUv(.9f,.2f),
            TextureUv(.5f,.8f)
        )

        renderer.render(context, camera, listOf(Mesh3D(listOf(triangle))))

        assertEquals(1, calls.size)
        assertTrue(calls.single()[0] === texture)
        assertEquals(.1f, calls.single()[3] as Float, 0f)
        assertEquals(.2f, calls.single()[4] as Float, 0f)
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
    @Test
    fun clipsTriangleCrossingNearPlaneInsteadOfDroppingWholeTriangle() {
        val drawn = mutableListOf<Int>()
        val context = Proxy.newProxyInstance(
            RenderContext::class.java.classLoader,
            arrayOf(RenderContext::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getWidth" -> 640
                "getHeight" -> 360
                "drawTriangle" -> { drawn += 1; null }
                else -> null
            }
        } as RenderContext
        val camera = Camera3D(
            position = Vector3.ZERO,
            target = Vector3(0f, 0f, -1f),
            near = 0.1f,
            far = 10f
        )
        val crossing = MeshTriangle(
            Vector3(0f, 0f, -0.05f),
            Vector3(-0.5f, -0.5f, -2f),
            Vector3(0.5f, -0.5f, -2f),
            0xFFFFFFFF.toInt()
        )

        renderer.render(context, camera, listOf(Mesh3D(listOf(crossing))))

        // A triangle clipped by one plane becomes a quad, rendered as a fan.
        assertEquals(2, drawn.size)
    }

    @Test
    fun rejectsGeometryEntirelyBeyondFarPlane() {
        val drawn = mutableListOf<Int>()
        val context = Proxy.newProxyInstance(
            RenderContext::class.java.classLoader,
            arrayOf(RenderContext::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getWidth" -> 640
                "getHeight" -> 360
                "drawTriangle" -> { drawn += 1; null }
                else -> null
            }
        } as RenderContext
        val camera = Camera3D(
            position = Vector3.ZERO,
            target = Vector3(0f, 0f, -1f),
            near = 0.1f,
            far = 5f
        )
        val beyondFar = MeshTriangle(
            Vector3(-1f, -1f, -8f),
            Vector3(1f, -1f, -8f),
            Vector3(0f, 1f, -8f),
            0xFFFFFFFF.toInt()
        )

        renderer.render(context, camera, listOf(Mesh3D(listOf(beyondFar))))

        assertTrue(drawn.isEmpty())
    }

    @Test
    fun projectRejectsPointsBeyondCameraFarPlane() {
        val camera = Camera3D(
            position = Vector3.ZERO,
            target = Vector3(0f, 0f, -1f),
            near = 0.1f,
            far = 5f
        )
        assertNull(renderer.project(Vector3(0f, 0f, -6f), camera, 640, 360))
    }

    @Test
    fun rejectsTriangleEntirelyOutsideHorizontalFrustum() {
        val drawn = mutableListOf<Int>()
        val context = Proxy.newProxyInstance(
            RenderContext::class.java.classLoader,
            arrayOf(RenderContext::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getWidth" -> 640
                "getHeight" -> 360
                "drawTriangle" -> { drawn += 1; null }
                else -> null
            }
        } as RenderContext
        val camera = Camera3D(
            position = Vector3.ZERO,
            target = Vector3(0f, 0f, -1f),
            aspectRatio = 16f / 9f,
            near = 0.1f,
            far = 10f
        )
        val outside = MeshTriangle(
            Vector3(20f, -1f, -2f),
            Vector3(22f, -1f, -2f),
            Vector3(21f, 1f, -2f),
            0xFFFFFFFF.toInt()
        )

        renderer.render(context, camera, listOf(Mesh3D(listOf(outside))))

        assertTrue(drawn.isEmpty())
    }

}
