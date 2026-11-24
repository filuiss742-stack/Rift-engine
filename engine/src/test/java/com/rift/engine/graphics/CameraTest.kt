package com.rift.engine.graphics

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraTest {
    @Test
    fun worldAndScreenCoordinatesRoundTripWithZoom() {
        val camera = Camera(x = 120f, y = 80f, zoom = 2f)

        assertEquals(60f, camera.worldToScreenX(150f), 0.001f)
        assertEquals(40f, camera.worldToScreenY(100f), 0.001f)
        assertEquals(150f, camera.screenToWorldX(60f), 0.001f)
        assertEquals(100f, camera.screenToWorldY(40f), 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroZoom() {
        Camera(zoom = 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteCameraPosition() {
        Camera(x = Float.NaN)
    }

    @Test
    fun zoomCanBeChangedAfterConstruction() {
        val camera = Camera()
        camera.zoom = 1.5f

        assertEquals(15f, camera.worldToScreenX(10f), 0.001f)
    }
}
