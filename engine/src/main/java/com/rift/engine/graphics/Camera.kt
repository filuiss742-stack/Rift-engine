package com.rift.engine.graphics

/**
 * 2D camera whose x/y coordinates describe the world-space top-left of the
 * visible area. Zoom is uniform and must be finite and greater than zero.
 */
class Camera(
    x: Float = 0f,
    y: Float = 0f,
    zoom: Float = 1f
) {
    var x: Float = x
        set(value) {
            require(value.isFinite()) { "Camera x must be finite." }
            field = value
        }

    var y: Float = y
        set(value) {
            require(value.isFinite()) { "Camera y must be finite." }
            field = value
        }

    var zoom: Float = zoom
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Camera zoom must be finite and greater than zero."
            }
            field = value
        }

    init {
        require(x.isFinite() && y.isFinite()) { "Camera position must be finite." }
        require(zoom.isFinite() && zoom > 0f) {
            "Camera zoom must be finite and greater than zero."
        }
    }

    fun worldToScreenX(worldX: Float): Float = (worldX - x) * zoom

    fun worldToScreenY(worldY: Float): Float = (worldY - y) * zoom

    fun screenToWorldX(screenX: Float): Float = screenX / zoom + x

    fun screenToWorldY(screenY: Float): Float = screenY / zoom + y

    /**
     * Applies this camera only for the duration of draw. The renderer state is
     * restored even if the callback throws.
     */
    fun withTransform(
        context: RenderContext,
        draw: () -> Unit
    ) {
        context.pushState()
        try {
            context.scale(zoom, zoom)
            context.translate(-x, -y)
            draw()
        } finally {
            context.popState()
        }
    }
}
