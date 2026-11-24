package com.rift.engine.movement

import kotlin.math.max
import kotlin.math.sign

/**
 * Side-view platform movement with acceleration, air control, coyote time,
 * jump buffering and optional variable jump height.
 *
 * The MovementBody marks itself as physics-controlled when its GameObject is
 * registered with PhysicsWorld. In that case this profile leaves gravity to
 * the world automatically. Use one profile instance per character because
 * jump timers are stateful.
 */
class ResponsivePlatformerProfile(
    private val maxSpeed: Float = 220f,
    private val groundAcceleration: Float = 1800f,
    private val groundDeceleration: Float = 2200f,
    private val airControl: Float = 0.6f,
    private val jumpSpeed: Float = 440f,
    private val gravity: Float = 980f,
    private val maxFallSpeed: Float = 900f,
    private val coyoteTimeSeconds: Float = 0.1f,
    private val jumpBufferSeconds: Float = 0.12f,
    private val variableJump: Boolean = false,
    private val jumpCutMultiplier: Float = 0.5f
) : MovementProfile {
    private var coyoteRemaining = 0f
    private var bufferRemaining = 0f
    private var jumping = false

    init {
        listOf(
            maxSpeed,
            groundAcceleration,
            groundDeceleration,
            jumpSpeed,
            gravity,
            maxFallSpeed,
            coyoteTimeSeconds,
            jumpBufferSeconds
        ).forEach {
            require(it.isFinite() && it >= 0f) {
                "Profile values must be finite and non-negative."
            }
        }
        require(airControl.isFinite() && airControl in 0f..1f) {
            "airControl must be finite and in 0..1."
        }
        require(jumpCutMultiplier.isFinite() && jumpCutMultiplier in 0f..1f) {
            "jumpCutMultiplier must be finite and in 0..1."
        }
    }

    /** Clears buffered input and jump grace state, for respawns or teleports. */
    fun reset() {
        coyoteRemaining = 0f
        bufferRemaining = 0f
        jumping = false
    }

    override fun update(body: MovementBody, input: MovementInput, deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }

        val grounded = body.isGrounded
        coyoteRemaining = if (grounded) {
            coyoteTimeSeconds
        } else {
            max(0f, coyoteRemaining - deltaSeconds)
        }
        bufferRemaining = if (input.jumpPressed) {
            jumpBufferSeconds
        } else {
            max(0f, bufferRemaining - deltaSeconds)
        }

        val horizontal = input.horizontal.coerceIn(-1f, 1f)
        val accelerating = horizontal != 0f &&
            (body.velocityX == 0f || sign(horizontal) == sign(body.velocityX))
        var acceleration = if (accelerating) groundAcceleration else groundDeceleration
        if (!grounded) acceleration *= airControl
        body.velocityX = moveToward(
            body.velocityX,
            horizontal * maxSpeed,
            acceleration * deltaSeconds
        )

        val wantsJump = input.jumpPressed || bufferRemaining > 0f
        if (wantsJump && (grounded || coyoteRemaining > 0f)) {
            body.velocityY = -jumpSpeed
            body.isGrounded = false
            bufferRemaining = 0f
            coyoteRemaining = 0f
            jumping = true
        } else if (grounded) {
            body.velocityY = 0f
            jumping = false
        } else {
            if (variableJump && jumping && !input.jumpHeld && body.velocityY < 0f) {
                body.velocityY *= jumpCutMultiplier
                jumping = false
            }
            if (!body.isPhysicsControlled) {
                body.velocityY = (body.velocityY + gravity * deltaSeconds)
                    .coerceAtMost(maxFallSpeed)
            }
            if (body.velocityY >= 0f) jumping = false
        }
    }

    private fun moveToward(current: Float, target: Float, maxDelta: Float): Float =
        if (current < target) {
            minOf(current + maxDelta, target)
        } else {
            maxOf(current - maxDelta, target)
        }
}
