package com.rift.engine.demo

import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.EngineKey
import org.junit.Assert.assertEquals
import org.junit.Test

class SandboxSceneInputTest {
    @Test
    fun heldKeyMovesAtDeltaBasedSpeedAndReleaseStopsMovement() {
        val scene = SandboxScene()
        val renderer = RecordingRenderContext()

        scene.onKey(EngineKey.D, true)
        scene.update(0.5f)
        scene.onKey(EngineKey.D, false)
        scene.update(0.5f)
        scene.render(renderer)

        assertEquals(260f, renderer.firstRoundedRectLeft!!, 0.001f)
    }

    private class RecordingRenderContext : RenderContext {
        override val width = 960
        override val height = 600
        var firstRoundedRectLeft: Float? = null
            private set

        override fun clear(color: Int) = Unit

        override fun drawRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            color: Int
        ) = Unit

        override fun drawRoundRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            radius: Float,
            color: Int
        ) {
            if (firstRoundedRectLeft == null) {
                firstRoundedRectLeft = left
            }
        }

        override fun drawLine(
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float,
            color: Int,
            strokeWidth: Float
        ) = Unit

        override fun drawText(
            text: String,
            x: Float,
            y: Float,
            color: Int,
            size: Float,
            bold: Boolean
        ) = Unit

        override fun measureText(
            text: String,
            size: Float,
            bold: Boolean
        ): Float = text.length * size * 0.5f
    }
}
