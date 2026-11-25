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

        val frontCamera = Camera3D(
            position = Vector3(2f, 2f, 0f),
            target = Vector3(2f, 0f, 3f)
        )
        val result = DirectionalSpriteRenderer.buildMesh(
            Vector3(2f, 0f, 3f), Vector3.FORWARD, frontCamera,
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
    fun appliesPerFrameAtlasUvRegionsWithoutCroppingTheTexture() {
        val atlas = FakeTexture()
        val animation = DirectionalSpriteAnimation(
            framesByDirection = mapOf(SpriteDirection8.FRONT to listOf(atlas,atlas)),
            frameDurationSeconds = .1f,
            uvRectsByDirection = mapOf(
                SpriteDirection8.FRONT to listOf(
                    spriteAtlasFrameUv(4,4,2),
                    spriteAtlasFrameUv(5,4,2)
                )
            )
        )

        val result = DirectionalSpriteRenderer.buildMesh(
            Vector3.ZERO, Vector3.FORWARD, camera,
            animation, .15f, 1f, 2f
        )

        assertEquals(1,result.frameIndex)
        assertSame(atlas,result.mesh.triangles.first().texture)
        assertEquals(.25f,result.mesh.triangles.first().uvA.u,0f)
        assertEquals(.5f,result.mesh.triangles.first().uvA.v,0f)
        assertEquals(.5f,result.mesh.triangles.first().uvC.u,0f)
        assertEquals(1f,result.mesh.triangles.first().uvC.v,0f)
    }

    @Test
    fun rejectsMismatchedAtlasRegionAndFrameCounts() {
        val atlas = FakeTexture()
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            DirectionalSpriteAnimation(
                framesByDirection = mapOf(SpriteDirection8.FRONT to listOf(atlas,atlas)),
                uvRectsByDirection = mapOf(
                    SpriteDirection8.FRONT to listOf(spriteAtlasFrameUv(0,4,2))
                )
            )
        }
    }

    @Test
    fun oneShotClipStopsOnItsLastFrameAndReportsCompletion() {
        val first = FakeTexture()
        val second = FakeTexture()
        val clip = DirectionalSpriteAnimation(
            framesByDirection = mapOf(SpriteDirection8.FRONT to listOf(first, second)),
            frameDurationSeconds = .1f,
            loop = false
        )
        assertEquals(0, clip.frameIndexAt(0f))
        assertEquals(1, clip.frameIndexAt(.15f))
        assertEquals(1, clip.frameIndexAt(12f))
        assertFalse(clip.isFinished(.19f))
        assertTrue(clip.isFinished(.2f))
    }

    @Test
    fun loopingClipWrapsSafelyForLongSessions() {
        val clip = DirectionalSpriteAnimation(
            framesByDirection = mapOf(SpriteDirection8.FRONT to listOf(FakeTexture(), FakeTexture())),
            frameDurationSeconds = .125f
        )
        assertEquals(0, clip.frameIndexAt(1_000_000f))
        assertFalse(clip.isFinished(100f))
    }

    @Test
    fun spriteLibraryResolvesNamedActionsAndReportsFallback() {
        val idle = DirectionalSpriteAnimation(mapOf(SpriteDirection8.FRONT to listOf(FakeTexture())))
        val attack = DirectionalSpriteAnimation(
            mapOf(SpriteDirection8.FRONT to listOf(FakeTexture())),
            loop = false
        )
        val library = DirectionalSpriteLibrary(mapOf("idle" to idle, "attack" to attack))
        val selected = library.resolve("attack")
        assertEquals("attack", selected.action)
        assertSame(attack, selected.animation)
        assertFalse(selected.usedFallback)
        val missing = library.resolve("cast")
        assertEquals("idle", missing.action)
        assertSame(idle, missing.animation)
        assertTrue(missing.usedFallback)
    }

    @Test
    fun spriteLibraryRejectsInvalidActionIdsAndMissingDefaults() {
        val idle = DirectionalSpriteAnimation(mapOf(SpriteDirection8.FRONT to listOf(FakeTexture())))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            DirectionalSpriteLibrary(mapOf("Idle" to idle))
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            DirectionalSpriteLibrary(mapOf("idle" to idle), defaultAction = "walk")
        }
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
