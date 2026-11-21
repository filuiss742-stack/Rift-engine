package com.rift.engine.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsivePlatformerProfileTest {
    private val dt = 1f / 60f

    @Test
    fun coyoteTimeAllowsJumpJustAfterLeavingGround() {
        val profile = ResponsivePlatformerProfile()
        val body = MovementBody()
        body.isGrounded = true
        profile.update(body, MovementInput(), dt)
        body.isGrounded = false
        profile.update(body, MovementInput(jumpPressed = true), dt)
        assertTrue(body.velocityY < 0f)
    }

    @Test
    fun jumpBufferFiresOnLanding() {
        val profile = ResponsivePlatformerProfile()
        val body = MovementBody()
        profile.update(body, MovementInput(jumpPressed = true), dt)
        assertTrue(body.velocityY > 0f)
        body.isGrounded = true
        profile.update(body, MovementInput(), dt)
        assertTrue(body.velocityY < 0f)
    }

    @Test
    fun noJumpAfterCoyoteWindowExpires() {
        val profile = ResponsivePlatformerProfile(coyoteTimeSeconds = 0.05f)
        val body = MovementBody()
        body.isGrounded = true
        profile.update(body, MovementInput(), dt)
        body.isGrounded = false
        repeat(10) { profile.update(body, MovementInput(), dt) }
        profile.update(body, MovementInput(jumpPressed = true), dt)
        assertTrue(body.velocityY >= 0f)
    }

    @Test
    fun releasingJumpCutsHeightWhenVariableJumpEnabled() {
        val profile = ResponsivePlatformerProfile(
            jumpSpeed = 400f,
            gravity = 0f,
            variableJump = true,
            jumpCutMultiplier = 0.5f
        )
        val body = MovementBody()
        body.isGrounded = true
        profile.update(body, MovementInput(jumpPressed = true, jumpHeld = true), dt)
        profile.update(body, MovementInput(jumpHeld = false), dt)
        assertEquals(-200f, body.velocityY, 0.001f)
    }

    @Test
    fun accelerationIsGradual() {
        val profile = ResponsivePlatformerProfile(maxSpeed = 200f, groundAcceleration = 600f)
        val body = MovementBody()
        body.isGrounded = true
        profile.update(body, MovementInput(horizontal = 1f), dt)
        assertEquals(10f, body.velocityX, 0.001f)
    }

    @Test
    fun defaultGravityProducesFallingVelocity() {
        val profile = ResponsivePlatformerProfile()
        val body = MovementBody()
        profile.update(body, MovementInput(), dt)
        assertTrue(body.velocityY > 0f)
    }
}
