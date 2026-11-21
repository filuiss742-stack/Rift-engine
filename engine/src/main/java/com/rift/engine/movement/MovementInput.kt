package com.rift.engine.movement

/**
 * Normalized movement intent, independent of keyboard, touch or controller.
 *
 * Horizontal and vertical axes are clamped to -1..1 by movement profiles.
 * Set jumpPressed for one update when a new jump press occurs.
 */
data class MovementInput(
    val horizontal: Float = 0f,
    val vertical: Float = 0f,
    val jumpPressed: Boolean = false
)
