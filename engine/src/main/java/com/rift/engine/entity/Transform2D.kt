package com.rift.engine.entity

import com.rift.engine.physics.Aabb
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** A point in logical world coordinates. */
data class Point2D(
    val x: Float,
    val y: Float
)

/**
 * Local 2D rotation and scale for a GameObject.
 *
 * Position remains owned by the GameObject's MovementBody. Rotation is in
 * degrees; scale may be negative to support horizontal/vertical flipping.
 */
class Transform2D {
    var rotationDegrees: Float = 0f
        set(value) {
            require(value.isFinite()) { "rotationDegrees must be finite." }
            field = value
        }

    var scaleX: Float = 1f
        set(value) {
            require(value.isFinite()) { "scaleX must be finite." }
            field = value
        }

    var scaleY: Float = 1f
        set(value) {
            require(value.isFinite()) { "scaleY must be finite." }
            field = value
        }

    fun transformPoint(
        localX: Float,
        localY: Float,
        worldX: Float,
        worldY: Float
    ): Point2D {
        val scaledX = localX * scaleX
        val scaledY = localY * scaleY
        val radians = Math.toRadians(rotationDegrees.toDouble())
        val cosine = cos(radians).toFloat()
        val sine = sin(radians).toFloat()

        return Point2D(
            x = worldX + scaledX * cosine - scaledY * sine,
            y = worldY + scaledX * sine + scaledY * cosine
        )
    }

    fun transformedBounds(
        worldX: Float,
        worldY: Float,
        width: Float,
        height: Float
    ): Aabb {
        require(width.isFinite() && height.isFinite()) {
            "width and height must be finite."
        }

        val corners = listOf(
            transformPoint(0f, 0f, worldX, worldY),
            transformPoint(width, 0f, worldX, worldY),
            transformPoint(width, height, worldX, worldY),
            transformPoint(0f, height, worldX, worldY)
        )

        return Aabb(
            left = corners.minOf { it.x },
            top = corners.minOf { it.y },
            right = corners.maxOf { it.x },
            bottom = corners.maxOf { it.y }
        )
    }
}
