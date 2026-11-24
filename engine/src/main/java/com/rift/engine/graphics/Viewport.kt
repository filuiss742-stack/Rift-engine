package com.rift.engine.graphics

import kotlin.math.min

/**
 * FIT viewport mapping from a fixed logical game resolution to a physical
 * render surface. Aspect ratio is preserved and unused space becomes bars.
 */
data class ViewportTransform(
    val logicalWidth: Int,
    val logicalHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int
) {
    val scale: Float
    val offsetX: Float
    val offsetY: Float

    init {
        require(logicalWidth > 0 && logicalHeight > 0) {
            "Logical viewport dimensions must be positive."
        }
        require(screenWidth > 0 && screenHeight > 0) {
            "Screen dimensions must be positive."
        }
        scale = min(
            screenWidth.toFloat() / logicalWidth,
            screenHeight.toFloat() / logicalHeight
        )
        offsetX = (screenWidth - logicalWidth * scale) * 0.5f
        offsetY = (screenHeight - logicalHeight * scale) * 0.5f
    }

    fun screenToLogicalX(screenX: Float): Float {
        require(screenX.isFinite()) { "screenX must be finite." }
        return (screenX - offsetX) / scale
    }

    fun screenToLogicalY(screenY: Float): Float {
        require(screenY.isFinite()) { "screenY must be finite." }
        return (screenY - offsetY) / scale
    }

    fun logicalToScreenX(logicalX: Float): Float {
        require(logicalX.isFinite()) { "logicalX must be finite." }
        return offsetX + logicalX * scale
    }

    fun logicalToScreenY(logicalY: Float): Float {
        require(logicalY.isFinite()) { "logicalY must be finite." }
        return offsetY + logicalY * scale
    }
}

/**
 * RenderContext adapter that presents a fixed logical resolution while
 * scaling drawing to the physical host surface. Close it once per frame.
 */
class ViewportRenderContext(
    private val delegate: RenderContext,
    val viewport: ViewportTransform
) : RenderContext by delegate, AutoCloseable {
    override val width: Int get() = viewport.logicalWidth
    override val height: Int get() = viewport.logicalHeight

    private var closed = false
    private var nestedStateDepth = 0

    init {
        delegate.pushState()
        applyViewportTransform()
    }

    override fun clear(color: Int) {
        check(!closed) { "ViewportRenderContext is closed." }
        delegate.clear(color)
        delegate.drawRect(0f, 0f, width.toFloat(), height.toFloat(), color)
    }

    override fun pushState() {
        check(!closed) { "ViewportRenderContext is closed." }
        delegate.pushState()
        nestedStateDepth += 1
    }

    override fun popState() {
        check(!closed) { "ViewportRenderContext is closed." }
        check(nestedStateDepth > 0) { "Viewport state stack is empty." }
        delegate.popState()
        nestedStateDepth -= 1
    }

    override fun close() {
        if (closed) return
        closed = true
        repeat(nestedStateDepth) { delegate.popState() }
        nestedStateDepth = 0
        delegate.popState()
    }

    private fun applyViewportTransform() {
        delegate.translate(viewport.offsetX, viewport.offsetY)
        delegate.scale(viewport.scale, viewport.scale)
    }
}
