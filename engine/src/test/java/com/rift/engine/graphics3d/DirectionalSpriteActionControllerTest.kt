package com.rift.engine.graphics3d

import com.rift.engine.graphics.TextureResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionalSpriteActionControllerTest {
    private class FakeTexture : TextureResource {
        override val width = 16
        override val height = 16
        override var isDisposed = false
        override fun close() { isDisposed = true }
    }

    private fun clip(frames: Int, loop: Boolean = true) = DirectionalSpriteAnimation(
        mapOf(SpriteDirection8.FRONT to List(frames) { FakeTexture() }),
        frameDurationSeconds = .1f,
        loop = loop
    )

    @Test
    fun movementCanBeInterruptedByAttackAndReturnsToIdle() {
        val library = DirectionalSpriteLibrary(mapOf(
            "idle" to clip(2),
            "walk" to clip(4),
            "attack" to clip(3, loop = false)
        ))
        val controller = DirectionalSpriteActionController(library)

        assertTrue(controller.play("walk", 10))
        controller.update(.2f)
        assertEquals("walk", controller.currentAction)
        assertTrue(controller.play("attack", 50))
        assertEquals(0f, controller.elapsedSeconds, 0f)
        controller.update(.31f)
        assertEquals("idle", controller.currentAction)
        assertEquals(0f, controller.elapsedSeconds, 0f)
    }

    @Test
    fun lowerPriorityMovementCannotCancelAnActiveOneShot() {
        val library = DirectionalSpriteLibrary(mapOf(
            "idle" to clip(1),
            "walk" to clip(2),
            "dodge" to clip(4, loop = false)
        ))
        val controller = DirectionalSpriteActionController(library)

        assertTrue(controller.play("dodge", 60))
        assertFalse(controller.play("walk", 10))
        assertEquals("dodge", controller.currentAction)
        controller.update(.4f)
        assertEquals("idle", controller.currentAction)
    }

    @Test
    fun missingActionsUseLibraryFallbackWithoutInventingFrames() {
        val idle = clip(1)
        val library = DirectionalSpriteLibrary(mapOf("idle" to idle))
        val controller = DirectionalSpriteActionController(library)

        assertTrue(controller.play("hit", 70))
        assertEquals("idle", controller.currentAction)
        assertTrue(controller.usedFallback)
        assertTrue(controller.currentAnimation === idle)
    }

    @Test
    fun sameActionDoesNotRestartItsAnimationClock() {
        val library = DirectionalSpriteLibrary(mapOf(
            "idle" to clip(1),
            "walk" to clip(3)
        ))
        val controller = DirectionalSpriteActionController(library)
        controller.play("walk", 10)
        controller.update(.15f)

        assertFalse(controller.play("walk", 10))
        assertEquals(.15f, controller.elapsedSeconds, .0001f)
    }

    @Test
    fun resetReturnsToFallbackAndRejectsInvalidDelta() {
        val library = DirectionalSpriteLibrary(mapOf(
            "idle" to clip(1),
            "walk" to clip(2)
        ))
        val controller = DirectionalSpriteActionController(library)
        controller.play("walk", 10)
        controller.reset()

        assertEquals("idle", controller.currentAction)
        assertEquals(0f, controller.elapsedSeconds, 0f)
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            controller.update(Float.NaN)
        }
    }
}
