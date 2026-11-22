package com.rift.engine.graphics3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Vector3Test {
    @Test
    fun dotAndCrossProductsUseRightHandedCoordinates() {
        assertEquals(0f, Vector3.RIGHT.dot(Vector3.UP), 0.0001f)
        assertEquals(Vector3(0f, 0f, 1f), Vector3.RIGHT.cross(Vector3.UP))
    }

    @Test
    fun normalizationProducesUnitLength() {
        val normalized = Vector3(3f, 0f, 4f).normalized()
        assertEquals(1f, normalized.length(), 0.0001f)
        assertEquals(0.6f, normalized.x, 0.0001f)
        assertEquals(0.8f, normalized.z, 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroVectorCannotBeNormalized() {
        Vector3.ZERO.normalized()
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteComponents() {
        Vector3(Float.NaN, 0f, 0f)
    }
}
