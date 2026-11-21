package com.rift.engine.ui

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
