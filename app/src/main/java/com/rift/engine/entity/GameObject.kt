package com.rift.engine.entity

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color

open class GameObject(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var color: Int = Color.WHITE
) {
    var velocityX: Float = 0f
    var velocityY: Float = 0f
    var active: Boolean = true
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    open fun update(deltaSeconds: Float) {
        if (!active) return
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
