package com.rift.engine.movement

/**
 * Runtime movement state composed into an entity.
 *
 * Position and velocity use logical world pixels and pixels per second.
 * Collision systems or the game may update [isGrounded].
 */
class MovementBody(
    x: Float = 0f,
    y: Float = 0f
) {
    var x: Float = x
        set(value) {
            require(value.isFinite()) { "x must be finite." }
            field = value
        }

    var y: Float = y
        set(value) {
            require(value.isFinite()) { "y must be finite." }
            field = value
        }

    var velocityX: Float = 0f
        set(value) {
            require(value.isFinite()) { "velocityX must be finite." }
            field = value
        }

    var velocityY: Float = 0f
        set(value) {
            require(value.isFinite()) { "velocityY must be finite." }
            field = value
        }

    var isGrounded: Boolean = false

    /** True while a PhysicsWorld owns position integration and gravity. */
    var isPhysicsControlled: Boolean = false
        internal set

    init {
        require(x.isFinite() && y.isFinite()) { "Position must be finite." }
    }
}
