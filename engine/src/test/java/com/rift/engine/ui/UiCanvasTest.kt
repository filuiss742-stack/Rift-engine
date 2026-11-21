package com.rift.engine.ui

import com.rift.engine.input.EngineKey
import com.rift.engine.input.PointerButtons
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiCanvasTest {
    private fun event(
        type: PointerEventType,
        x: Float = 20f,
        y: Float = 20f,
        pointerId: Int = 0
    ) = PointerEvent(
        pointerId = pointerId,
        type = type,
        x = x,
        y = y,
        viewportWidth = 200f,
        viewportHeight = 200f,
        button = if (type == PointerEventType.DOWN || type == PointerEventType.UP) {
            PointerButtons.TOUCH
        } else {
            PointerButtons.NONE
        }
    )


    @Test
    fun failingUiElementIsQuarantinedWithoutStoppingOtherElements() {
        val diagnostics = com.rift.engine.diagnostics.DiagnosticReporter()
        val canvas = UiCanvas().apply { this.diagnostics = diagnostics }
        var shouldFail = true
        var failingUpdates = 0
        var healthyUpdates = 0
        val failing = object : UiElement(0f, 0f, 10f, 10f) {
            override fun update(deltaSeconds: Float) {
                failingUpdates += 1
                if (shouldFail) error("broken UI element")
            }
            override fun render(context: com.rift.engine.graphics.RenderContext) = Unit
        }
        val healthy = object : UiElement(20f, 0f, 10f, 10f) {
            override fun update(deltaSeconds: Float) {
                healthyUpdates += 1
            }
            override fun render(context: com.rift.engine.graphics.RenderContext) = Unit
        }
        canvas.add(failing)
        canvas.add(healthy)

        canvas.update(1f / 60f)
        canvas.update(1f / 60f)

        assertEquals(1, failingUpdates)
        assertEquals(2, healthyUpdates)
        assertTrue(canvas.isElementQuarantined(failing))
        assertEquals("RIFT_RUNTIME_EXCEPTION", diagnostics.latestError()?.code)

        shouldFail = false
        assertTrue(canvas.retryElement(failing))
        canvas.update(1f / 60f)

        assertEquals(2, failingUpdates)
        assertEquals(3, healthyUpdates)
        assertFalse(canvas.isElementQuarantined(failing))
    }


    @Test
    fun panelVerticalLayoutPositionsChildrenInLocalCoordinates() {
        val panel = UiPanel(100f, 50f, 200f, 140f).apply {
            layout = UiLayout.VERTICAL
            padding = 10f
            gap = 5f
        }
        val first = UiButton(0f, 0f, 60f, 20f, "First", {})
        val second = UiLabel(0f, 0f, 80f, 15f, "Second")
        panel.add(first)
        panel.add(second)

        assertEquals(10f, first.x, 0.001f)
        assertEquals(10f, first.y, 0.001f)
        assertEquals(10f, second.x, 0.001f)
        assertEquals(35f, second.y, 0.001f)
    }

    @Test
    fun nestedPanelRoutesPointerToLocalChildCoordinates() {
        val canvas = UiCanvas()
        var clicks = 0
        val panel = UiPanel(100f, 100f, 120f, 80f)
        panel.add(UiButton(10f, 10f, 60f, 30f, "Nested", { clicks += 1 }))
        canvas.add(panel)

        assertTrue(canvas.dispatchPointer(event(PointerEventType.DOWN, x = 115f, y = 115f)))
        assertTrue(canvas.dispatchPointer(event(PointerEventType.UP, x = 115f, y = 115f)))
        assertEquals(1, clicks)
    }

    @Test
    fun tabCyclesFocusAndEnterActivatesFocusedButton() {
        val canvas = UiCanvas()
        var firstClicks = 0
        var secondClicks = 0
        val first = UiButton(0f, 0f, 60f, 30f, "First", { firstClicks += 1 })
        val second = UiButton(0f, 40f, 60f, 30f, "Second", { secondClicks += 1 })
        canvas.add(first)
        canvas.add(second)

        assertTrue(canvas.dispatchKey(EngineKey.TAB, true))
        assertTrue(first.isFocused)
        canvas.dispatchKey(EngineKey.TAB, false)
        assertTrue(canvas.dispatchKey(EngineKey.ENTER, true))
        canvas.dispatchKey(EngineKey.ENTER, false)
        assertEquals(1, firstClicks)

        assertTrue(canvas.dispatchKey(EngineKey.TAB, true))
        assertTrue(second.isFocused)
        canvas.dispatchKey(EngineKey.TAB, false)
        canvas.dispatchKey(EngineKey.ENTER, true)
        assertEquals(1, secondClicks)
    }

    @Test
    fun buttonFiresOnlyAfterPressAndReleaseInside() {
        val canvas = UiCanvas()
        var clicks = 0
        val button = UiButton(10f, 10f, 80f, 40f, "Play", { clicks += 1 })
        canvas.add(button)

        assertTrue(canvas.dispatchPointer(event(PointerEventType.DOWN)))
        assertTrue(button.isPressed)
        assertTrue(canvas.dispatchPointer(event(PointerEventType.UP)))
        assertFalse(button.isPressed)
        assertEquals(1, clicks)
    }

    @Test
    fun releaseOutsideCancelsClick() {
        val canvas = UiCanvas()
        var clicks = 0
        canvas.add(UiButton(10f, 10f, 80f, 40f, "Play", { clicks += 1 }))

        canvas.dispatchPointer(event(PointerEventType.DOWN))
        canvas.dispatchPointer(event(PointerEventType.UP, x = 150f, y = 150f))

        assertEquals(0, clicks)
    }

    @Test
    fun cancelAndRemovalClearPressedState() {
        val canvas = UiCanvas()
        val button = UiButton(10f, 10f, 80f, 40f, "Play", {})
        canvas.add(button)
        canvas.dispatchPointer(event(PointerEventType.DOWN))
        assertTrue(button.isPressed)

        canvas.remove(button)

        assertFalse(button.isPressed)
        assertFalse(canvas.dispatchPointer(event(PointerEventType.UP)))
    }

    @Test
    fun topmostZIndexReceivesPointerBeforeLowerWidgets() {
        val canvas = UiCanvas()
        var lowerClicks = 0
        var upperClicks = 0
        canvas.add(UiButton(10f, 10f, 80f, 40f, "Lower", { lowerClicks += 1 }).apply {
            zIndex = 1
        })
        canvas.add(UiButton(10f, 10f, 80f, 40f, "Upper", { upperClicks += 1 }).apply {
            zIndex = 5
        })

        canvas.dispatchPointer(event(PointerEventType.DOWN))
        canvas.dispatchPointer(event(PointerEventType.UP))

        assertEquals(0, lowerClicks)
        assertEquals(1, upperClicks)
    }

    @Test
    fun multiplePointersCanPressSameButton() {
        val canvas = UiCanvas()
        var clicks = 0
        val button = UiButton(10f, 10f, 80f, 40f, "Tap", { clicks += 1 })
        canvas.add(button)

        canvas.dispatchPointer(event(PointerEventType.DOWN, pointerId = 1))
        canvas.dispatchPointer(event(PointerEventType.DOWN, pointerId = 2))
        canvas.dispatchPointer(event(PointerEventType.UP, pointerId = 1))
        assertTrue(button.isPressed)
        canvas.dispatchPointer(event(PointerEventType.UP, pointerId = 2))

        assertFalse(button.isPressed)
        assertEquals(2, clicks)
    }
}
