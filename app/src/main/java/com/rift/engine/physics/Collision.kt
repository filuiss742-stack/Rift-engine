package com.rift.engine.physics

import android.graphics.RectF

object Collision {
    fun intersects(a: RectF, b: RectF): Boolean = RectF.intersects(a, b)

    fun clamp(value: Float, min: Float, max: Float): Float =
        value.coerceIn(min, max)
}
