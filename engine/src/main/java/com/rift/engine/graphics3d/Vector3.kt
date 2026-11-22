package com.rift.engine.graphics3d

import kotlin.math.sqrt

/**
 * Immutable three-dimensional vector used by Rift's platform-neutral 3D APIs.
 *
 * Coordinates use a right-handed world convention: +X right, +Y up, +Z toward
 * the viewer. Render backends may convert coordinates at their boundary.
 */
data class Vector3(val x: Float, val y: Float, val z: Float) {
    init {
        require(x.isFinite() && y.isFinite() && z.isFinite()) {
            "Vector3 components must be finite."
        }
    }

    operator fun plus(other: Vector3) =
        Vector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vector3) =
        Vector3(x - other.x, y - other.y, z - other.z)

    operator fun unaryMinus() = Vector3(-x, -y, -z)

    operator fun times(scale: Float): Vector3 {
        require(scale.isFinite()) { "Vector scale must be finite." }
        return Vector3(x * scale, y * scale, z * scale)
    }

    operator fun div(scale: Float): Vector3 {
        require(scale.isFinite() && scale != 0f) {
            "Vector divisor must be finite and non-zero."
        }
        return Vector3(x / scale, y / scale, z / scale)
    }

    fun dot(other: Vector3): Float =
        x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun lengthSquared(): Float = dot(this)

    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vector3 {
        val magnitude = length()
        require(magnitude > NORMALIZATION_EPSILON) {
            "Cannot normalize a zero-length Vector3."
        }
        return this / magnitude
    }

    companion object {
        private const val NORMALIZATION_EPSILON = 1e-6f

        val ZERO = Vector3(0f, 0f, 0f)
        val RIGHT = Vector3(1f, 0f, 0f)
        val UP = Vector3(0f, 1f, 0f)
        val FORWARD = Vector3(0f, 0f, -1f)
    }
}
