package com.rift.engine.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementProfileTest {
    @Test
    fun topDownMovementNormalizesDiagonalInput() {
        val body = MovementBody()
        val profile = TopDownMovementProfile(speed = 100f)

        profile.update(body, MovementInput(horizontal = 1f, vertical = 1f), 1f / 60f)

        assertEquals(70.7107f, body.velocityX, 0.001f)
        assertEquals(70.7107f, body.velocityY, 0.001f)
    }

    @Test
    fun platformerJumpStartsOnlyWhenGrounded() {
        val body = MovementBody().apply { isGrounded = true }
        val profile = PlatformerMovementProfile(jumpSpeed = 400f)

        profile.update(body, MovementInput(jumpPressed = true), 1f / 60f)

        assertEquals(-400f, body.velocityY, 0.001f)
        assertFalse(body.isGrounded)
    }

    @Test
    fun platformerAppliesGravityAndCapsFallingSpeed() {
        val body = MovementBody().apply {
            isGrounded = false
            velocityY = 890f
        }
        val profile = PlatformerMovementProfile(gravity = 980f, maxFallSpeed = 900f)

        profile.update(body, MovementInput(), 1f / 60f)

        assertEquals(900f, body.velocityY, 0.001f)
        assertTrue(body.velocityY <= 900f)
    }

    @Test
    fun groundedPlatformerDoesNotAccumulateGravity() {
        val body = MovementBody().apply {
            isGrounded = true
            velocityY = 120f
        }
        val profile = PlatformerMovementProfile()

        profile.update(body, MovementInput(), 1f / 60f)

        assertEquals(0f, body.velocityY, 0.001f)
    }
}
