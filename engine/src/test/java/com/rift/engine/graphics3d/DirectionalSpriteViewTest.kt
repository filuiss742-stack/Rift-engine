package com.rift.engine.graphics3d

import org.junit.Assert.assertEquals
import org.junit.Test

class DirectionalSpriteViewTest {
    private val origin = Vector3.ZERO
    private val facing = Vector3.FORWARD

    @Test
    fun selectsAllFourCardinalViews() {
        assertEquals(
            SpriteDirection8.FRONT,
            DirectionalSpriteView.select(origin, facing, Vector3(0f, 0f, -5f))
        )
        assertEquals(
            SpriteDirection8.RIGHT,
            DirectionalSpriteView.select(origin, facing, Vector3(5f, 0f, 0f))
        )
        assertEquals(
            SpriteDirection8.BACK,
            DirectionalSpriteView.select(origin, facing, Vector3(0f, 0f, 5f))
        )
        assertEquals(
            SpriteDirection8.LEFT,
            DirectionalSpriteView.select(origin, facing, Vector3(-5f, 0f, 0f))
        )
    }

    @Test
    fun selectsDiagonalViews() {
        assertEquals(
            SpriteDirection8.FRONT_RIGHT,
            DirectionalSpriteView.select(origin, facing, Vector3(2f, 0f, -2f))
        )
        assertEquals(
            SpriteDirection8.BACK_RIGHT,
            DirectionalSpriteView.select(origin, facing, Vector3(2f, 0f, 2f))
        )
        assertEquals(
            SpriteDirection8.BACK_LEFT,
            DirectionalSpriteView.select(origin, facing, Vector3(-2f, 0f, 2f))
        )
        assertEquals(
            SpriteDirection8.FRONT_LEFT,
            DirectionalSpriteView.select(origin, facing, Vector3(-2f, 0f, -2f))
        )
    }

    @Test
    fun cameraElevationDoesNotChangeHorizontalViewSelection() {
        val ground = DirectionalSpriteView.select(
            origin, facing, Vector3(3f, 0f, -4f)
        )
        val elevated = DirectionalSpriteView.select(
            origin, facing, Vector3(3f, 20f, -4f)
        )
        assertEquals(ground, elevated)
    }

    @Test
    fun directlyOverheadCameraUsesDeterministicFrontFallback() {
        assertEquals(
            SpriteDirection8.FRONT,
            DirectionalSpriteView.select(origin, facing, Vector3(0f, 10f, 0f))
        )
    }
}
