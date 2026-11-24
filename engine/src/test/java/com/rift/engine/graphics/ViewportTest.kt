package com.rift.engine.graphics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportTest {
    @Test
    fun fitViewportPreservesAspectRatioAndMapsCoordinatesBothWays() {
        val viewport = ViewportTransform(960, 600, 873, 393)

        assertEquals(0.655f, viewport.scale, 0.001f)
        assertTrue(viewport.offsetX > 0f)
        assertEquals(0f, viewport.offsetY, 0.001f)
        assertEquals(
            480f,
            viewport.screenToLogicalX(viewport.logicalToScreenX(480f)),
            0.001f
        )
        assertEquals(
            300f,
            viewport.screenToLogicalY(viewport.logicalToScreenY(300f)),
            0.001f
        )
    }

    @Test
    fun viewportContextExposesLogicalDimensionsAndRestoresUnbalancedStateOnClose() {
        val delegate = RecordingContext()
        val viewport = ViewportTransform(960, 600, 1920, 1200)
        val context = ViewportRenderContext(delegate, viewport)

        assertEquals(960, context.width)
        assertEquals(600, context.height)
        assertEquals(1, delegate.stateDepth)

        context.pushState()
        context.translate(12f, 8f)
        context.pushState()
        context.close()

        assertEquals(0, delegate.stateDepth)
    }

    private class RecordingContext : RenderContext {
        override val width = 1920
        override val height = 1200
        var stateDepth = 0
        override fun clear(color: Int) = Unit
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, color: Int) = Unit
        override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Int) = Unit
        override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float) = Unit
        override fun drawText(text: String, x: Float, y: Float, color: Int, size: Float, bold: Boolean) = Unit
        override fun measureText(text: String, size: Float, bold: Boolean): Float = 0f
        override fun pushState() { stateDepth += 1 }
        override fun popState() { stateDepth -= 1 }
        override fun translate(x: Float, y: Float) = Unit
        override fun rotate(degrees: Float) = Unit
        override fun scale(x: Float, y: Float) = Unit
        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) = Unit
    }
}
