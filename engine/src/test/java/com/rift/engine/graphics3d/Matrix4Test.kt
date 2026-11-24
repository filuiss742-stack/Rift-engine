package com.rift.engine.graphics3d

import org.junit.Assert.assertEquals
import org.junit.Test

class Matrix4Test {
    @Test
    fun identityLeavesPointUnchanged() {
        val point = Vector3(2f, -3f, 4f)
        assertEquals(point, Matrix4.identity().transformPoint(point))
    }

    @Test
    fun lookAtMapsEyeToOriginAndTargetToNegativeZ() {
        val eye = Vector3(0f, 2f, 5f)
        val target = Vector3.ZERO
        val view = Matrix4.lookAt(eye, target, Vector3.UP)

        assertEquals(Vector3.ZERO, view.transformPoint(eye))
        val targetInView = view.transformPoint(target)
        assertEquals(0f, targetInView.x, 0.0001f)
        assertEquals(0f, targetInView.y, 0.0001f)
        assertTrueNegative(targetInView.z)
    }

    @Test
    fun perspectiveMatrixHasOpenGlDepthConvention() {
        val projection = Matrix4.perspective(
            Math.toRadians(60.0).toFloat(), 16f / 9f, 0.1f, 100f
        )
        assertEquals(1f, projection[3, 2] * -1f, 0.0001f)
        assertEquals(0f, projection[3, 3], 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidPerspectivePlanes() {
        Matrix4.perspective(1f, 1f, 10f, 1f)
    }

    private fun assertTrueNegative(value: Float) {
        org.junit.Assert.assertTrue("Expected a point in front of camera to have negative view-space Z.", value < 0f)
    }
}
