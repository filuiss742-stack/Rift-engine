package com.rift.engine.input

enum class PointerEventType {
    DOWN,
    MOVE,
    UP,
    CANCEL
}

object PointerButtons {
    const val NONE = 0
    const val PRIMARY = 1
    const val SECONDARY = 2
    const val MIDDLE = 3
    const val TOUCH = 4
}

/**
 * Device-independent pointer event. Pointer IDs distinguish simultaneous
 * fingers; mouse input uses pointer ID 0.
 */
data class PointerEvent(
    val pointerId: Int,
    val type: PointerEventType,
    val x: Float,
    val y: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val button: Int = PointerButtons.NONE
) {
    init {
        require(pointerId >= 0) { "pointerId must not be negative." }
        require(x.isFinite() && y.isFinite()) { "Pointer coordinates must be finite." }
        require(
            viewportWidth.isFinite() && viewportHeight.isFinite() &&
                viewportWidth >= 0f && viewportHeight >= 0f
        ) { "Viewport dimensions must be finite and non-negative." }
        require(button >= 0) { "Pointer button must not be negative." }
        if (type == PointerEventType.DOWN || type == PointerEventType.UP) {
            require(button != PointerButtons.NONE) {
                "Pointer down/up events must specify a button."
            }
        }
    }
}
