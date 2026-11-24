package com.rift.engine.graphics

import com.rift.engine.entity.GameObject
import com.rift.engine.physics.Aabb
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraRigTest {
    private val step = 1f / 60f

    @Test
    fun zeroSmoothingCentersOnTarget() {
        val camera = Camera()
        val rig = CameraRig(camera, 100f, 100f, smoothing = 0f)
        rig.update(GameObject(500f, 500f, 20f, 20f), step)
        assertEquals(460f, camera.x, 0.001f)
        assertEquals(460f, camera.y, 0.001f)
    }

    @Test
    fun boundsClampBothEdges() {
        val camera = Camera()
        val rig = CameraRig(
            camera,
            100f,
            100f,
            smoothing = 0f,
            worldBounds = Aabb(0f, 0f, 1000f, 1000f)
        )
        rig.snapTo(GameObject(0f, 0f, 10f, 10f))
        assertEquals(0f, camera.x, 0.001f)
        rig.snapTo(GameObject(990f, 990f, 10f, 10f))
        assertEquals(900f, camera.x, 0.001f)
        assertEquals(900f, camera.y, 0.001f)
    }

    @Test
    fun deadZoneKeepsCameraStill() {
        val camera = Camera(x = 450f, y = 450f)
        val rig = CameraRig(
            camera,
            100f,
            100f,
            smoothing = 0f,
            deadZoneWidth = 40f,
            deadZoneHeight = 40f
        )
        rig.update(GameObject(490f, 490f, 20f, 20f), step)
        assertEquals(450f, camera.x, 0.001f)
        assertEquals(450f, camera.y, 0.001f)
    }

    @Test
    fun shakeReturnsToBasePosition() {
        val camera = Camera()
        val rig = CameraRig(camera, 100f, 100f, smoothing = 0f)
        val target = GameObject(500f, 500f, 20f, 20f)
        rig.shake(10f, 0.1f)
        repeat(10) { rig.update(target, 0.02f) }
        rig.update(target, 0.02f)
        assertEquals(460f, camera.x, 0.001f)
        assertEquals(460f, camera.y, 0.001f)
    }

    @Test
    fun zoomChangesVisibleWorldArea() {
        val camera = Camera(zoom = 2f)
        val rig = CameraRig(camera, 100f, 100f, smoothing = 0f)
        rig.snapTo(GameObject(500f, 500f, 20f, 20f))
        assertEquals(485f, camera.x, 0.001f)
        assertEquals(485f, camera.y, 0.001f)
    }
}
