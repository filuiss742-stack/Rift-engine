package com.rift.engine.ui

import com.rift.engine.input.PointerButtons
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualControlsTest {
    private fun event(
        pointerId: Int,
        type: PointerEventType,
        x: Float,
        y: Float
    ) = PointerEvent(
        pointerId = pointerId,
        type = type,
        x = x,
        y = y,
        viewportWidth = 320f,
        viewportHeight = 240f,
        button = PointerButtons.TOUCH
    )

    @Test
    fun joystickMapsPointerDistanceToNormalizedAxesAndResetsOnRelease() {
        val joystick = VirtualJoystick(0f, 0f, 100f)
        assertTrue(joystick.onPointer(event(7, PointerEventType.DOWN, 50f, 50f)))
        assertTrue(joystick.onPointer(event(7, PointerEventType.MOVE, 100f, 50f)))
        assertEquals(1f, joystick.axisX, 0.001f)
        assertEquals(0f, joystick.axisY, 0.001f)

        assertFalse(joystick.onPointer(event(8, PointerEventType.MOVE, 50f, 100f)))
        assertTrue(joystick.onPointer(event(7, PointerEventType.UP, 100f, 50f)))
        assertEquals(0f, joystick.axisX, 0.001f)
        assertEquals(0f, joystick.axisY, 0.001f)
    }


    @Test
    fun additionalFingerOnJoystickIsConsumedWithoutTakingControl() {
        val canvas = UiCanvas()
        val joystick = VirtualJoystick(0f, 0f, 100f)
        canvas.add(joystick)

        assertTrue(canvas.dispatchPointer(event(1, PointerEventType.DOWN, 50f, 50f)))
        assertTrue(canvas.dispatchPointer(event(1, PointerEventType.MOVE, 100f, 50f)))
        assertTrue(canvas.dispatchPointer(event(2, PointerEventType.DOWN, 50f, 50f)))
        assertTrue(canvas.dispatchPointer(event(2, PointerEventType.UP, 50f, 50f)))
        assertEquals(1f, joystick.axisX, 0.001f)

        canvas.dispatchPointer(event(1, PointerEventType.UP, 100f, 50f))
        assertEquals(0f, joystick.axisX, 0.001f)
    }

    @Test
    fun joystickAppliesDeadZone() {
        val joystick = VirtualJoystick(0f, 0f, 100f, deadZone = 0.2f)
        joystick.onPointer(event(1, PointerEventType.DOWN, 50f, 50f))
        joystick.onPointer(event(1, PointerEventType.MOVE, 55f, 50f))

        assertEquals(0f, joystick.axisX, 0.001f)
        assertEquals(0f, joystick.axisY, 0.001f)
    }

    @Test
    fun virtualButtonStaysPressedUntilAllPointersRelease() {
        var presses = 0
        var releases = 0
        val button = VirtualButton(
            0f, 0f, 80f, 60f, "Jump",
            onPressed = { presses += 1 },
            onReleased = { releases += 1 }
        )

        assertTrue(button.onPointer(event(1, PointerEventType.DOWN, 20f, 20f)))
        assertTrue(button.onPointer(event(2, PointerEventType.DOWN, 20f, 20f)))
        assertEquals(1, presses)
        assertTrue(button.isPressed)

        button.onPointer(event(1, PointerEventType.UP, 20f, 20f))
        assertTrue(button.isPressed)
        assertEquals(0, releases)

        button.onPointer(event(2, PointerEventType.CANCEL, 20f, 20f))
        assertFalse(button.isPressed)
        assertEquals(1, releases)
    }

    @Test
    fun removingPressedVirtualButtonReleasesIt() {
        var releases = 0
        val canvas = UiCanvas()
        val button = VirtualButton(
            0f, 0f, 80f, 60f, "Fire",
            onPressed = {},
            onReleased = { releases += 1 }
        )
        canvas.add(button)
        canvas.dispatchPointer(event(1, PointerEventType.DOWN, 20f, 20f))

        canvas.remove(button)

        assertFalse(button.isPressed)
        assertEquals(1, releases)
    }
}
