package com.rift.engine.physics3d

import com.rift.engine.graphics3d.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionWorld3DTest {
    @Test
    fun raycastAabbReturnsNearestSurfacePointAndOutwardNormal() {
        val world = CollisionWorld3D()
        world.add(AabbCollider3D("crate", Vector3.ZERO, Vector3(1f, 1f, 1f)))

        val hit = world.raycastFirst(Ray3(Vector3(0f, 0f, 5f), Vector3(0f, 0f, -4f)))

        assertNotNull(hit)
        assertEquals("crate", hit!!.colliderId)
        assertEquals(4f, hit.distance, 0.0001f)
        assertEquals(1f, hit.point.z, 0.0001f)
        assertEquals(1f, hit.normal.z, 0.0001f)
    }

    @Test
    fun raycastSphereFindsEntryPoint() {
        val world = CollisionWorld3D()
        world.add(SphereCollider3D("orb", Vector3.ZERO, 2f))

        val hit = world.raycastFirst(Ray3(Vector3(0f, 0f, 5f), Vector3(0f, 0f, -1f)))

        assertNotNull(hit)
        assertEquals(3f, hit!!.distance, 0.0001f)
        assertEquals(1f, hit.normal.z, 0.0001f)
    }

    @Test
    fun rayStartingInsideAabbReturnsExitSurface() {
        val world = CollisionWorld3D()
        world.add(AabbCollider3D("room", Vector3.ZERO, Vector3(1f, 1f, 1f)))

        val hit = world.raycastFirst(Ray3(Vector3.ZERO, Vector3.RIGHT))

        assertNotNull(hit)
        assertEquals(1f, hit!!.distance, 0.0001f)
        assertEquals(1f, hit.normal.x, 0.0001f)
    }

    @Test
    fun raycastRespectsMaximumDistanceAndLayerMask() {
        val world = CollisionWorld3D()
        world.add(AabbCollider3D("near", Vector3(0f, 0f, 2f), Vector3(0.5f, 0.5f, 0.5f), layerMask = 0b01))
        world.add(AabbCollider3D("far", Vector3(0f, 0f, 0f), Vector3(0.5f, 0.5f, 0.5f), layerMask = 0b10))
        val ray = Ray3(Vector3(0f, 0f, 5f), Vector3(0f, 0f, -1f))

        assertEquals("near", world.raycastFirst(ray, maxDistance = 3f, queryLayerMask = 0b01)?.colliderId)
        assertNull(world.raycastFirst(ray, maxDistance = 1f, queryLayerMask = 0b01))
        assertEquals("far", world.raycastFirst(ray, queryLayerMask = 0b10)?.colliderId)
    }

    @Test
    fun raycastAllIsSortedAndDisabledCollidersAreIgnored() {
        val world = CollisionWorld3D()
        world.add(SphereCollider3D("far", Vector3(0f, 0f, 0f), 0.5f))
        world.add(SphereCollider3D("near", Vector3(0f, 0f, 3f), 0.5f))
        world.add(SphereCollider3D("disabled", Vector3(0f, 0f, 4f), 0.5f, enabled = false))

        val hits = world.raycastAll(Ray3(Vector3(0f, 0f, 6f), Vector3(0f, 0f, -1f)))

        assertEquals(listOf("near", "far"), hits.map { it.colliderId })
    }

    @Test
    fun removeColliderChangesSubsequentQueries() {
        val world = CollisionWorld3D()
        world.add(SphereCollider3D("target", Vector3.ZERO, 1f))
        assertTrue(world.remove("target"))
        assertFalse(world.remove("target"))
        assertNull(world.raycastFirst(Ray3(Vector3(0f, 0f, 3f), Vector3(0f, 0f, -1f))))
    }
}
