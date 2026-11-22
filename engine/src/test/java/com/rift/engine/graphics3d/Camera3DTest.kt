package com.rift.engine.graphics3d

import org.junit.Assert.assertEquals
import org.junit.Test

class Camera3DTest {
    @Test
    fun cameraProducesViewProjectionMatrix() {
        val camera = Camera3D(
            position = Vector3(0f, 2f, 5f),
            target = Vector3.ZERO,
            aspectRatio = 2f
        )

        assertEquals(16, camera.viewProjectionMatrix.toFloatArray().size)
        assertEquals(2f, camera.aspectRatio, 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun cameraRejectsCoincidentPositionAndTarget() {
        Camera3D(position = Vector3.ZERO, target = Vector3.ZERO)
    }

    @Test(expected = IllegalArgumentException::class)
    fun cameraRejectsParallelUpAndViewDirection() {
        Camera3D(
            position = Vector3(0f, 0f, 5f),
            target = Vector3.ZERO,
            up = Vector3(0f, 0f, 1f)
        )
    }
}
