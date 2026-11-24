package com.rift.engine.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class Transform2DTest {
    @Test
    fun computesAxisAlignedBoundsAfterRotation() {
        val transform = Transform2D()
        transform.rotationDegrees = 90f

        val bounds = transform.transformedBounds(
            worldX = 100f,
            worldY = 200f,
            width = 10f,
            height = 20f
        )

        assertEquals(80f, bounds.left, 0.001f)
        assertEquals(200f, bounds.top, 0.001f)
        assertEquals(100f, bounds.right, 0.001f)
        assertEquals(210f, bounds.bottom, 0.001f)
    }

    @Test
    fun negativeScaleCanFlipObjectAndBounds() {
        val transform = Transform2D()
        transform.scaleX = -1f

        val point = transform.transformPoint(10f, 0f, 100f, 50f)
        val bounds = transform.transformedBounds(100f, 50f, 10f, 20f)

        assertEquals(90f, point.x, 0.001f)
        assertEquals(50f, point.y, 0.001f)
        assertEquals(90f, bounds.left, 0.001f)
        assertEquals(100f, bounds.right, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteRotation() {
        Transform2D().rotationDegrees = Float.POSITIVE_INFINITY
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteScale() {
        Transform2D().scaleY = Float.NaN
    }

    @Test
    fun gameObjectBoundsUseItsTransform() {
        val objectUnderTest = GameObject(10f, 20f, 8f, 4f)
        objectUnderTest.rotationDegrees = 90f

        val bounds = objectUnderTest.bounds()

        assertEquals(6f, bounds.left, 0.001f)
        assertEquals(20f, bounds.top, 0.001f)
        assertEquals(10f, bounds.right, 0.001f)
        assertEquals(28f, bounds.bottom, 0.001f)
    }
}
