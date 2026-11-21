package com.rift.engine.entity

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import com.rift.engine.movement.MovementBody
import com.rift.engine.movement.MovementInput
import com.rift.engine.movement.MovementProfile

open class GameObject(
    x: Float,
    y: Float,
    var width: Float,
    var height: Float,
    var color: Int = Color.WHITE
) {
    val movementBody = MovementBody(x, y)

    /** Compatibility accessors backed by the composed movement body. */
    var x: Float
        get() = movementBody.x
        set(value) { movementBody.x = value }

    var y: Float
        get() = movementBody.y
        set(value) { movementBody.y = value }

    var velocityX: Float
        get() = movementBody.velocityX
        set(value) { movementBody.velocityX = value }

    var velocityY: Float
        get() = movementBody.velocityY
        set(value) { movementBody.velocityY = value }

    var isGrounded: Boolean
        get() = movementBody.isGrounded
        set(value) { movementBody.isGrounded = value }

    /** Optional movement rule set. Null preserves direct velocity-based movement. */
    var movementProfile: MovementProfile? = null

    /** Updated by the game's input layer before the scene update. */
    var movementInput: MovementInput = MovementInput()

    var active: Boolean = true
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    open fun update(deltaSeconds: Float) {
        if (!active) return

        movementProfile?.update(movementBody, movementInput, deltaSeconds)
        x += velocityX * deltaSeconds
        y += velocityY * deltaSeconds
    }

    open fun render(canvas: Canvas) {
        if (!active) return
        paint.color = color
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 12f, 12f, paint)
    }

    fun bounds(): RectF = RectF(x, y, x + width, y + height)
}
