package com.rift.engine.movement

/** A composable rule set that converts input into body velocity. */
interface MovementProfile {
    fun update(body: MovementBody, input: MovementInput, deltaSeconds: Float)
}

/**
 * Free movement on a 2D plane. Diagonal input is normalized so it is not faster
 * than movement along one axis.
 */
class TopDownMovementProfile(
    private val speed: Float = 180f
) : MovementProfile {
    init {
        require(speed.isFinite() && speed >= 0f) {
            "speed must be a finite, non-negative value."
        }
    }

    override fun update(body: MovementBody, input: MovementInput, deltaSeconds: Float) {
        val horizontal = input.horizontal.coerceIn(-1f, 1f)
        val vertical = input.vertical.coerceIn(-1f, 1f)
        val lengthSquared = horizontal * horizontal + vertical * vertical
        val scale = if (lengthSquared > 1f) 1f / kotlin.math.sqrt(lengthSquared) else 1f

        body.velocityX = horizontal * scale * speed
        body.velocityY = vertical * scale * speed
    }
}

/**
 * Side-view platform movement. Positive Y points down: gravity is positive and
 * jump velocity is negative. Ground contact is supplied by a collision system
 * or by the game; this profile does not invent floor geometry.
 */
class PlatformerMovementProfile(
    private val horizontalSpeed: Float = 190f,
    private val gravity: Float = 980f,
    private val jumpSpeed: Float = 420f,
    private val maxFallSpeed: Float = 900f
) : MovementProfile {
    init {
        require(horizontalSpeed.isFinite() && horizontalSpeed >= 0f) {
            "horizontalSpeed must be a finite, non-negative value."
        }
        require(gravity.isFinite() && gravity >= 0f) {
            "gravity must be a finite, non-negative value."
        }
        require(jumpSpeed.isFinite() && jumpSpeed >= 0f) {
            "jumpSpeed must be a finite, non-negative value."
        }
        require(maxFallSpeed.isFinite() && maxFallSpeed >= 0f) {
            "maxFallSpeed must be a finite, non-negative value."
        }
    }

    override fun update(body: MovementBody, input: MovementInput, deltaSeconds: Float) {
        body.velocityX = input.horizontal.coerceIn(-1f, 1f) * horizontalSpeed

        if (body.isGrounded) {
            if (input.jumpPressed) {
                body.velocityY = -jumpSpeed
                body.isGrounded = false
            } else {
                body.velocityY = 0f
            }
        } else {
            body.velocityY = (body.velocityY + gravity * deltaSeconds)
                .coerceAtMost(maxFallSpeed)
        }
    }
}
