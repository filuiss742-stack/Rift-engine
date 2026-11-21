package com.rift.engine.movement

/**
 * Runtime movement state composed into an entity.
 *
 * Position and velocity use logical world pixels and pixels per second.
 * Collision systems or the game may update [isGrounded].
 */
class MovementBody(
    var x: Float = 0f,
    var y: Float = 0f
) {
    var velocityX: Float = 0f
    var velocityY: Float = 0f
    var isGrounded: Boolean = false
}
