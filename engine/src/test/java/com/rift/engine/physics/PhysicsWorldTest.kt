package com.rift.engine.physics

import com.rift.engine.entity.GameObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun highSpeedDynamicBodyDoesNotPassThroughThinStaticWallWithinSubstepBudget() {
        val world = PhysicsWorld(gravityY = 0f, maxSubstepDistance = 12f, maxSubsteps = 16)
        val runner = GameObject(0f, 20f, 10f, 10f).apply { velocityX = 1000f }
        val wall = GameObject(100f, 0f, 8f, 80f)
        world.addBody(runner, gravityScale = 0f)
        world.addBody(wall, PhysicsBodyType.STATIC)

        world.step(0.1f)

        assertTrue(runner.x + runner.width <= wall.x + 0.001f)
        assertEquals(0f, runner.velocityX, 0.001f)
        assertTrue(world.contacts().isNotEmpty())
    }

    @Test
    fun stackedDynamicBodiesSettleOnStaticGround() {
        val world = PhysicsWorld(gravityY = 980f)
        val lower = GameObject(20f, 80f, 20f, 10f)
        val upper = GameObject(20f, 70f, 20f, 10f)
        val ground = GameObject(0f, 100f, 100f, 20f)
        world.addBody(lower)
        world.addBody(upper)
        world.addBody(ground, PhysicsBodyType.STATIC)

        world.step(0.5f)

        assertEquals(90f, lower.y, 0.05f)
        assertEquals(80f, upper.y, 0.05f)
        assertEquals(0f, lower.velocityY, 0.05f)
        assertEquals(0f, upper.velocityY, 0.05f)
        assertTrue(lower.isGrounded)
        assertTrue(upper.isGrounded)
    }



    @Test
    fun dynamicBodyStopsAtInsideCornerWithoutCrossingEitherWall() {
        val world = PhysicsWorld(gravityY = 0f)
        val mover = GameObject(80f, 80f, 10f, 10f).apply {
            velocityX = 100f
            velocityY = 100f
        }
        val verticalWall = GameObject(100f, 0f, 10f, 100f)
        val horizontalWall = GameObject(0f, 100f, 100f, 10f)
        world.addBody(mover, gravityScale = 0f)
        world.addBody(verticalWall, PhysicsBodyType.STATIC)
        world.addBody(horizontalWall, PhysicsBodyType.STATIC)

        world.step(0.2f)

        assertEquals(90f, mover.x, 0.001f)
        assertEquals(90f, mover.y, 0.001f)
        assertEquals(0f, mover.velocityX, 0.001f)
        assertEquals(0f, mover.velocityY, 0.001f)
        assertEquals(2, world.contacts().size)
    }

    @Test
    fun reportsBeginStayAndEndContactLifecycle() {
        val world = PhysicsWorld(gravityY = 100f)
        val player = GameObject(0f, 0f, 10f, 10f)
        val ground = GameObject(0f, 10f, 100f, 10f)
        world.addBody(player)
        world.addBody(ground, PhysicsBodyType.STATIC)

        world.step(0.1f)
        assertEquals(ContactPhase.BEGIN, world.contactEvents().single().phase)

        world.step(0.1f)
        assertEquals(ContactPhase.STAY, world.contactEvents().single().phase)

        world.removeBody(ground)
        world.step(0.1f)
        assertEquals(ContactPhase.END, world.contactEvents().single().phase)
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
    fun boxRaycastStartingInsideReturnsFirstExitSurfaceAndNormal() {
        val world = PhysicsWorld(gravityY = 0f)
        val target = GameObject(10f, 10f, 20f, 20f)
        world.addBody(target, PhysicsBodyType.STATIC)

        val hit = world.raycast(15f, 15f, 40f, 15f)

        assertNotNull(hit)
        assertTrue(hit!!.body.gameObject === target)
        assertEquals(30f, hit.pointX, 0.001f)
        assertEquals(15f, hit.pointY, 0.001f)
        assertEquals(1f, hit.normalX, 0.001f)
        assertEquals(0f, hit.normalY, 0.001f)
        assertEquals(0.6f, hit.fraction, 0.001f)
    }

    @Test
    fun clearReleasesBodiesAndResetsContactState() {
        val world = PhysicsWorld(gravityY = 0f)
        val first = GameObject(0f, 0f, 10f, 10f)
        val second = GameObject(5f, 0f, 10f, 10f)
        world.addBody(first, gravityScale = 0f)
        world.addBody(second, PhysicsBodyType.STATIC)
        world.step(0.016f)
        assertTrue(world.contacts().isNotEmpty())

        world.clear()

        assertTrue(world.bodies().isEmpty())
        assertTrue(world.contacts().isEmpty())
        assertTrue(world.contactEvents().isEmpty())
        assertFalse(first.physicsControlled)
        assertFalse(second.physicsControlled)
        assertEquals(0, world.lastBroadPhaseCandidateCount)
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
    }


    @Test
    fun circleBodyResolvesAgainstStaticBox() {
        val world = PhysicsWorld(gravityY = 0f)
        val ball = GameObject(0f, 0f, 10f, 10f)
        val wall = GameObject(8f, 0f, 10f, 10f)
        world.addBody(
            ball,
            gravityScale = 0f,
            shape = CollisionShape.Circle(radius = 5f)
        )
        world.addBody(wall, PhysicsBodyType.STATIC)

        world.step(0.016f)

        assertEquals(-2f, ball.x, 0.001f)
        assertEquals(1f, world.contacts().single().normalX, 0.001f)
    }

    @Test
    fun circleCircleCollisionIsDetected() {
        val world = PhysicsWorld(gravityY = 0f)
        val first = GameObject(0f, 0f, 10f, 10f)
        val second = GameObject(8f, 0f, 10f, 10f)
        world.addBody(first, gravityScale = 0f, shape = CollisionShape.Circle(5f))
        world.addBody(second, PhysicsBodyType.STATIC, shape = CollisionShape.Circle(5f))

        world.step(0.016f)

        assertEquals(1, world.contacts().size)
        assertEquals(-2f, first.x, 0.001f)
    }

    @Test
    fun circleRaycastReturnsSurfacePointAndNormal() {
        val world = PhysicsWorld(gravityY = 0f)
        val target = GameObject(5f, 0f, 10f, 10f)
        world.addBody(
            target,
            PhysicsBodyType.STATIC,
            shape = CollisionShape.Circle(5f)
        )

        val hit = world.raycast(0f, 5f, 20f, 5f)

        assertNotNull(hit)
        assertTrue(hit!!.body.gameObject === target)
        assertEquals(5f, hit.pointX, 0.001f)
        assertEquals(5f, hit.pointY, 0.001f)
        assertEquals(-1f, hit.normalX, 0.001f)
    }


    @Test
    fun spatialHashSkipsPairsInDistantCells() {
        val world = PhysicsWorld(gravityY = 0f, broadPhaseCellSize = 64f)
        repeat(40) { index ->
            world.addBody(
                GameObject(index * 500f, 0f, 10f, 10f),
                PhysicsBodyType.STATIC
            )
        }

        world.step(0.016f)

        assertEquals(0, world.lastBroadPhaseCandidateCount)
        assertTrue(world.contacts().isEmpty())
    }

    @Test
    fun oversizedBodiesUseSafeFallbackAndRemainCollidable() {
        val world = PhysicsWorld(
            gravityY = 0f,
            broadPhaseCellSize = 1f,
            maxCellsPerBody = 4
        )
        val dynamic = GameObject(0f, 0f, 10f, 10f)
        val ground = GameObject(5f, 0f, 10f, 10f)
        world.addBody(dynamic, gravityScale = 0f)
        world.addBody(ground, PhysicsBodyType.STATIC)

        world.step(0.016f)

        assertEquals(1, world.lastBroadPhaseCandidateCount)
        assertEquals(1, world.contacts().size)
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
        val dynamic = GameObject(0f, 1f, 10f, 10f)
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
