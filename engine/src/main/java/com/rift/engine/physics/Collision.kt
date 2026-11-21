package com.rift.engine.physics

data class Aabb(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(left <= right && top <= bottom) {
            "AABB right/bottom edges must not precede left/top edges."
        }
    }
}

object Collision {
    fun intersects(first: Aabb, second: Aabb): Boolean {
        return first.left < second.right &&
            second.left < first.right &&
            first.top < second.bottom &&
            second.top < first.bottom
    }

    fun clamp(
        value: Float,
        minimum: Float,
        maximum: Float
    ): Float {
        require(minimum <= maximum) {
            "minimum must not be greater than maximum."
        }
        return value.coerceIn(minimum, maximum)
    }
}
