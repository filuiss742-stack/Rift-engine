package com.rift.engine.graphics3d

import com.rift.engine.graphics.TextureResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionalSpriteRendererTest {
    private class FakeTexture : TextureResource {
        override val width = 32
        override val height = 64
        override var isDisposed = false
        override fun close() { isDisposed = true }
    }

    private val camera = Camera3D(
        position = Vector3(0f, 2f, 6f),
        target = Vector3.ZERO
    )

    @Test
    fun buildsTwoTexturedTrianglesAnchoredAtFeet() {
        val texture = FakeTexture()
        val animation = DirectionalSpriteAnimation(
            mapOf(SpriteDirection8.FRONT to listOf(texture))
        )

        val result = DirectionalSpriteRenderer.buildMesh(
            Vector3(2f, 0f, 3f), Vector3.FORWARD, camera,
            animation, 0f, 1.2f, 2.4f
        )

        assertEquals(2, result.mesh.triangles.size)
        assertSame(texture, result.mesh.triangles[0].texture)
        val vertices = result.mesh.triangles.flatMap { listOf(it.a, it.b, it.c) }
        assertEquals(0f, vertices.minOf { it.y }, 0f)
        assertEquals(2.4f, vertices.maxOf { it.y }, 0f)
        assertFalse(result.usedFallback)
    }

    @Test
    fun selectsAuthoredCameraDirectionInsteadOfRotatingFrontFrame() {
        val front = FakeTexture()
        val right = FakeTexture()
        val animation = DirectionalSpriteAnimation(
            mapOf(
                SpriteDirection8.FRONT to listOf(front),
                SpriteDirection8.RIGHT to listOf(right)
            )
        )
        val sideCamera = Camera3D(
            position = Vector3(6f, 2f, 0f),
            target = Vector3.ZERO
        )

        val result = DirectionalSpriteRenderer.buildMesh(
            Vector3.ZERO, Vector3.FORWARD, sideCamera,
            animation, 0f, 1f, 2f
        )

        assertEquals(SpriteDirection8.RIGHT, result.selectedDirection)
        assertSame(right, result.mesh.triangles.first().texture)
        assertFalse(result.usedFallback)
    }

    @Test
    fun reportsAndUsesExplicitFallbackWhenDirectionIsMissing() {
        val front = FakeTexture()
        val animation = DirectionalSpriteAnimation(
            mapOf(SpriteDirection8.FRONT to listOf(front))
        )
        val sideCamera = Camera3D(
            position = Vector3(6f, 2f, 0f),
            target = Vector3.ZERO
        )

        val result = DirectionalSpriteRenderer.buildMesh(
            Vector3.ZERO, Vector3.FORWARD, sideCamera,
            animation, 0f, 1f, 2f
        )

        assertTrue(result.usedFallback)
        assertEquals(SpriteDirection8.FRONT, result.selectedDirection)
        assertSame(front, result.mesh.triangles.first().texture)
    }

    @Test
    fun advancesFramesUsingConfiguredDuration() {
        val first = FakeTexture()
        val second = FakeTexture()
        val animation = DirectionalSpriteAnimation(
            mapOf(SpriteDirection8.FRONT to listOf(first, second)),
            frameDurationSeconds = 0.1f
        )

        val result = DirectionalSpriteRenderer.buildMesh(
            Vector3.ZERO, Vector3.FORWARD, camera,
            animation, 0.15f, 1f, 2f
        )

        assertEquals(1, result.frameIndex)
        assertSame(second, result.mesh.triangles.first().texture)
    }
}
