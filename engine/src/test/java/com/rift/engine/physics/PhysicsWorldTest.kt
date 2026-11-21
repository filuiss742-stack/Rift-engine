package com.rift.engine.physics

import com.rift.engine.entity.GameObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicsWorldTest {
    @Test
    fun dynamicBodyFallsAndSettlesOnStaticGround() {
        val world = PhysicsWorld(gravityY = 980f)
        val player = GameObject(20f, 80f, 10f, 10f)
        val ground = GameObject(0f, 100f, 100f, 20f)

        world.addBody(player)
        world.addBody(ground, PhysicsBodyType.STATIC)
        world.step(0.5f)

        assertEquals(90f, player.y, 0.001f)
        assertEquals(0f, player.velocityY, 0.001f)
        assertTrue(player.isGrounded)
        assertEquals(1, world.contacts().size)
    }

    @Test
    fun sensorReportsOverlapWithoutResolvingIt() {
        val world = PhysicsWorld(gravityY = 0f)
        val player = GameObject(0f, 0f, 10f, 10f)
        val pickup = GameObject(8f, 0f, 10f, 10f)

        world.addBody(player, gravityScale = 0f)
        world.addBody(pickup, PhysicsBodyType.STATIC, isSensor = true)
        world.step(0.016f)

        assertEquals(0f, player.x, 0.001f)
        assertTrue(world.contacts().single().isSensor)
    }

    @Test
    fun collisionFiltersCanExcludePairs() {
        val world = PhysicsWorld(gravityY = 0f)
        val first = GameObject(0f, 0f, 10f, 10f)
        val second = GameObject(5f, 0f, 10f, 10f)

        world.addBody(
            first,
            gravityScale = 0f,
            collisionFilter = CollisionFilter(categoryBits = 0b01, maskBits = 0b01)
        )
        world.addBody(
            second,
            PhysicsBodyType.STATIC,
            collisionFilter = CollisionFilter(categoryBits = 0b10, maskBits = 0b10)
        )
        world.step(0.016f)

        assertTrue(world.contacts().isEmpty())
        assertEquals(0f, first.x, 0.001f)
    }

    @Test
    fun queryAabbReturnsOnlyOverlappingBodies() {
        val world = PhysicsWorld(gravityY = 0f)
        val near = GameObject(4f, 4f, 5f, 5f)
        val far = GameObject(40f, 40f, 5f, 5f)
        world.addBody(near, PhysicsBodyType.STATIC)
        world.addBody(far, PhysicsBodyType.STATIC)

        val result = world.queryAabb(Aabb(0f, 0f, 10f, 10f))

        assertEquals(listOf(near), result.map { it.gameObject })
    }

    @Test
    fun raycastReturnsNearestBodyAndSurfaceNormal() {
        val world = PhysicsWorld(gravityY = 0f)
        val farther = GameObject(15f, 0f, 5f, 10f)
        val nearer = GameObject(8f, 0f, 5f, 10f)
        world.addBody(farther, PhysicsBodyType.STATIC)
        world.addBody(nearer, PhysicsBodyType.STATIC)

        val hit = world.raycast(0f, 5f, 30f, 5f)

        assertNotNull(hit)
        assertTrue(hit!!.body.gameObject === nearer)
        assertEquals(8f, hit.pointX, 0.001f)
        assertEquals(5f, hit.pointY, 0.001f)
        assertEquals(-1f, hit.normalX, 0.001f)
    }

    @Test
    fun removingBodyRestoresGameObjectOwnedIntegration() {
        val world = PhysicsWorld(gravityY = 0f)
        val objectUnderTest = GameObject(0f, 0f, 10f, 10f)
        objectUnderTest.velocityX = 10f
        world.addBody(objectUnderTest)

        assertTrue(world.removeBody(objectUnderTest))
        objectUnderTest.update(1f)

        assertEquals(10f, objectUnderTest.x, 0.001f)
        assertFalse(objectUnderTest.physicsControlledForTest())
    }

    @Test
    fun staticBodiesDoNotProduceStaticStaticContacts() {
        val world = PhysicsWorld(gravityY = 0f)
        world.addBody(GameObject(0f, 0f, 10f, 10f), PhysicsBodyType.STATIC)
        world.addBody(GameObject(5f, 0f, 10f, 10f), PhysicsBodyType.STATIC)

        world.step(0.016f)

        assertTrue(world.contacts().isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDelta() {
        PhysicsWorld().step(Float.NaN)
    }

    @Test
    fun staticBodyHasNoGravityAndDynamicGroundStateCanClear() {
        val world = PhysicsWorld(gravityY = 0f)
        val dynamic = GameObject(0f, 0f, 10f, 10f)
        val ground = GameObject(0f, 10f, 10f, 10f)
        world.addBody(dynamic, gravityScale = 0f)
        world.addBody(ground, PhysicsBodyType.STATIC)
        world.step(0.016f)
        assertTrue(dynamic.isGrounded)

        ground.x = 100f
        world.step(0.016f)

        assertFalse(dynamic.isGrounded)
    }
}
