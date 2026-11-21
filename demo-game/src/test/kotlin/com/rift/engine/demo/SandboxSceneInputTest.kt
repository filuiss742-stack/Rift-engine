package com.rift.engine.demo

import com.rift.engine.core.SceneManager
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.EngineKey
import com.rift.engine.input.PointerButtons
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import org.junit.Assert.assertEquals
import org.junit.Test

class SandboxSceneInputTest {
    @Test
    fun heldKeyMovesAtDeltaBasedSpeedAndReleaseStopsMovement() {
        val scene = SandboxScene()
        val renderer = RecordingRenderContext()

        val manager = SceneManager()
        manager.setInitialScene(scene)
        manager.onKey(EngineKey.D, true)
        manager.update(0.5f)
        manager.onKey(EngineKey.D, false)
        manager.update(0.5f)
        manager.render(renderer)

        assertEquals(260f, renderer.firstRoundedRectLeft!!, 0.001f)
    }

    @Test
    fun tappingWorldDoesNotTeleportPlayer() {
        val scene = SandboxScene()
        val renderer = RecordingRenderContext()
        val manager = SceneManager()
        manager.setInitialScene(scene)

        manager.onPointer(
            PointerEvent(
                pointerId = 1,
                type = PointerEventType.DOWN,
                x = 700f,
                y = 300f,
                viewportWidth = 960f,
                viewportHeight = 600f,
                button = PointerButtons.TOUCH
            )
        )
        manager.onPointer(
            PointerEvent(
                pointerId = 1,
                type = PointerEventType.UP,
                x = 700f,
                y = 300f,
                viewportWidth = 960f,
                viewportHeight = 600f,
                button = PointerButtons.TOUCH
            )
        )
        manager.update(1f / 60f)
        manager.render(renderer)

        assertEquals(140f, renderer.firstRoundedRectLeft!!, 0.001f)
    }

    private class RecordingRenderContext : RenderContext {
        override val width = 960
        override val height = 600
        var firstRoundedRectLeft: Float? = null
            private set
        private val translationStack = mutableListOf<Float>()
        private var translationX = 0f

        override fun pushState() {
            translationStack.add(translationX)
        }

        override fun popState() {
            translationX = translationStack.removeAt(translationStack.lastIndex)
        }

        override fun translate(x: Float, y: Float) {
            translationX += x
        }

        override fun rotate(degrees: Float) = Unit

        override fun scale(x: Float, y: Float) = Unit

        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) = Unit

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
            if (firstRoundedRectLeft == null && color == 0xFF6996FF.toInt()) {
                firstRoundedRectLeft = translationX + left
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
