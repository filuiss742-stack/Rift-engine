package com.rift.engine.graphics3d

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

/** Eight authored camera-relative views for a 2D character in a 3D world. */
enum class SpriteDirection8 {
    FRONT,
    FRONT_RIGHT,
    RIGHT,
    BACK_RIGHT,
    BACK,
    BACK_LEFT,
    LEFT,
    FRONT_LEFT
}

/**
 * Selects the authored sprite view nearest to the camera's horizontal azimuth.
 *
 * "Front" means the camera is in the direction the character faces. The
 * character's facing vector and camera offset are projected onto the X/Z plane;
 * vertical camera pitch does not change the selected azimuth.
 */
object DirectionalSpriteView {
    private const val HORIZONTAL_EPSILON_SQUARED = 1e-8f

    fun select(
        characterPosition: Vector3,
        facingDirection: Vector3,
        cameraPosition: Vector3
    ): SpriteDirection8 {
        val facing = Vector3(facingDirection.x, 0f, facingDirection.z)
        val cameraOffset = cameraPosition - characterPosition
        val cameraDirection = Vector3(cameraOffset.x, 0f, cameraOffset.z)

        require(facing.lengthSquared() > HORIZONTAL_EPSILON_SQUARED) {
            "Character facing direction must have a non-zero horizontal component."
        }

        if (cameraDirection.lengthSquared() <= HORIZONTAL_EPSILON_SQUARED) {
            // Directly overhead: preserve a deterministic front-view fallback.
            return SpriteDirection8.FRONT
        }

        val forward = facing.normalized()
        val right = forward.cross(Vector3.UP).normalized()
        val direction = cameraDirection.normalized()
        val angle = atan2(direction.dot(right), direction.dot(forward))
        val sector = (angle / (PI / 4.0)).roundToInt().mod(8)
        return SpriteDirection8.entries[sector]
    }
}
