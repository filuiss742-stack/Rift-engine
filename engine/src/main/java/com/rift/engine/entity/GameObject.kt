package com.rift.engine.entity

import com.rift.engine.graphics.RenderContext
import com.rift.engine.movement.MovementBody
import com.rift.engine.movement.MovementInput
import com.rift.engine.movement.MovementProfile
import com.rift.engine.physics.Aabb

/**
 * Basic composable entity used by the current scene API.
 *
 * Position and dimensions are measured in logical world pixels.
 */
open class GameObject(
    x: Float,
    y: Float,
    var width: Float,
    var height: Float,
    var color: Int = 0xFFFFFFFF.toInt()
) {
    val movementBody = MovementBody(x, y)

    var x: Float
        get() = movementBody.x
        set(value) {
            movementBody.x = value
        }

    var y: Float
        get() = movementBody.y
        set(value) {
            movementBody.y = value
        }

    var velocityX: Float
        get() = movementBody.velocityX
        set(value) {
            movementBody.velocityX = value
        }

    var velocityY: Float
        get() = movementBody.velocityY
        set(value) {
            movementBody.velocityY = value
        }

    var isGrounded: Boolean
        get() = movementBody.isGrounded
        set(value) {
            movementBody.isGrounded = value
        }

    var movementProfile: MovementProfile? = null
    var movementInput: MovementInput = MovementInput()
    var active: Boolean = true

    open fun update(deltaSeconds: Float) {
        if (!active) {
            return
        }

        movementProfile?.update(
            movementBody,
            movementInput,
            deltaSeconds
        )

        x += velocityX * deltaSeconds
        y += velocityY * deltaSeconds
    }

    open fun render(context: RenderContext) {
        if (!active) {
            return
        }

        context.drawRoundRect(
            x,
            y,
            x + width,
            y + height,
            12f,
            color
        )
    }

    fun bounds(): Aabb {
        return Aabb(
            left = x,
            top = y,
            right = x + width,
            bottom = y + height
        )
    }
}
