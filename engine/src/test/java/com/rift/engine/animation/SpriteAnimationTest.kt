package com.rift.engine.animation

import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.Sprite
import com.rift.engine.graphics.SpriteSheet
import com.rift.engine.graphics.TextureResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpriteAnimationTest {
    private class FakeTexture(
        override val width: Int,
        override val height: Int
    ) : TextureResource {
        override var isDisposed: Boolean = false
            private set

        override fun close() {
            isDisposed = true
        }
    }

    @Test
    fun spriteSheetMapsFramesToGridCoordinates() {
        val texture = FakeTexture(64, 32)
        val sheet = SpriteSheet(texture, frameWidth = 16, frameHeight = 16)

        val frame = sheet.sprite(5)

        assertEquals(4, sheet.columns)
        assertEquals(2, sheet.rows)
        assertEquals(8, sheet.frameCount)
        assertEquals(16, frame.region.left)
        assertEquals(16, frame.region.top)
        assertEquals(32, frame.region.right)
        assertEquals(32, frame.region.bottom)
    }

    @Test
    fun loopingAnimationAdvancesAndWraps() {
        val texture = FakeTexture(3, 1)
        val sheet = SpriteSheet(texture, 1, 1)
        val animation = SpriteAnimation(
            listOf(
                AnimationFrame(sheet.sprite(0), 0.1f),
                AnimationFrame(sheet.sprite(1), 0.1f),
                AnimationFrame(sheet.sprite(2), 0.1f)
            ),
            looping = true
        )
        val component = SpriteAnimationComponent(mapOf("walk" to animation), "walk")
        val owner = GameObject(0f, 0f, 20f, 20f)

        component.update(owner, 0.11f)
        assertEquals(1, component.currentSprite.region.left)

        component.update(owner, 0.2f)
        assertEquals(0, component.currentSprite.region.left)
        assertFalse(component.isFinished)
    }

    @Test
    fun nonLoopingAnimationStopsOnLastFrameAndCanRestart() {
        val texture = FakeTexture(2, 1)
        val sheet = SpriteSheet(texture, 1, 1)
        val animation = SpriteAnimation(
            listOf(
                AnimationFrame(sheet.sprite(0), 0.1f),
                AnimationFrame(sheet.sprite(1), 0.1f)
            ),
            looping = false
        )
        val component = SpriteAnimationComponent(mapOf("death" to animation), "death")
        val owner = GameObject(0f, 0f, 16f, 16f)

        component.update(owner, 0.5f)

        assertTrue(component.isFinished)
        assertEquals(1, component.currentSprite.region.left)

        component.play("death")
        assertFalse(component.isFinished)
        assertEquals(0, component.currentSprite.region.left)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptySpriteSheetFrame() {
        val texture = FakeTexture(16, 16)
        SpriteSheet(texture, frameWidth = 0, frameHeight = 16)
    }
}
