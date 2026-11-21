package com.rift.engine.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputActionMapTest {
    @Test
    fun tracksPressedHeldAndReleasedActions() {
        val input = InputActionMap()
        input.bindKey("move.left", 1)

        input.onKey(1, true)
        assertTrue(input.isDown("move.left"))
        assertTrue(input.wasPressed("move.left"))
        assertFalse(input.wasReleased("move.left"))

        input.endFrame()
        assertFalse(input.wasPressed("move.left"))
        assertTrue(input.isDown("move.left"))

        input.onKey(1, false)
        assertFalse(input.isDown("move.left"))
        assertTrue(input.wasReleased("move.left"))
    }

    @Test
    fun multipleKeysCanBindToOneActionWithoutFalseRelease() {
        val input = InputActionMap()
        input.bindKey("move.left", 1)
        input.bindKey("move.left", 2)

        input.onKey(1, true)
        assertTrue(input.wasPressed("move.left"))
        input.endFrame()

        input.onKey(2, true)
        assertFalse(input.wasPressed("move.left"))
        input.onKey(1, false)

        assertTrue(input.isDown("move.left"))
        assertFalse(input.wasReleased("move.left"))

        input.onKey(2, false)
        assertFalse(input.isDown("move.left"))
        assertTrue(input.wasReleased("move.left"))
    }

    @Test
    fun repeatedKeyDownDoesNotRepeatPressedEdge() {
        val input = InputActionMap()
        input.bindKey("jump", 3)

        input.onKey(3, true)
        input.onKey(3, true)

        assertTrue(input.wasPressed("jump"))
        input.endFrame()
        input.onKey(3, true)

        assertFalse(input.wasPressed("jump"))
    }


    @Test
    fun bindingAnotherAlreadyHeldKeyDoesNotRepeatActionPress() {
        val input = InputActionMap()
        input.bindKey("move.left", 1)
        input.onKey(1, true)
        input.endFrame()

        input.onKey(2, true)
        input.bindKey("move.left", 2)

        assertTrue(input.isDown("move.left"))
        assertFalse(input.wasPressed("move.left"))
    }

    @Test
    fun unbindingHeldActionProducesReleaseEdge() {
        val input = InputActionMap()
        input.bindKey("interact", 4)
        input.onKey(4, true)
        input.endFrame()

        assertTrue(input.unbindAction("interact"))
        assertFalse(input.isDown("interact"))
        assertTrue(input.wasReleased("interact"))
    }


    @Test
    fun pointerButtonsCanDriveNamedActions() {
        val input = InputActionMap()
        input.bindPointerButton("attack", PointerButtons.PRIMARY)

        input.onPointer(
            PointerEvent(0, PointerEventType.DOWN, 10f, 20f, 100f, 100f, PointerButtons.PRIMARY)
        )
        assertTrue(input.isDown("attack"))
        assertTrue(input.wasPressed("attack"))

        input.endFrame()
        input.onPointer(
            PointerEvent(0, PointerEventType.UP, 10f, 20f, 100f, 100f, PointerButtons.PRIMARY)
        )
        assertFalse(input.isDown("attack"))
        assertTrue(input.wasReleased("attack"))
    }

    @Test
    fun keyboardAndPointerBindingsShareOneActionWithoutFalseRelease() {
        val input = InputActionMap()
        input.bindKey("confirm", 1)
        input.bindPointerButton("confirm", PointerButtons.PRIMARY)

        input.onKey(1, true)
        input.endFrame()
        input.onPointer(
            PointerEvent(0, PointerEventType.DOWN, 1f, 1f, 20f, 20f, PointerButtons.PRIMARY)
        )
        input.onKey(1, false)

        assertTrue(input.isDown("confirm"))
        assertFalse(input.wasReleased("confirm"))
    }

    @Test
    fun multipleTouchPointersKeepActionHeldUntilLastRelease() {
        val input = InputActionMap()
        input.bindPointerButton("touch", PointerButtons.TOUCH)

        input.onPointer(
            PointerEvent(10, PointerEventType.DOWN, 1f, 1f, 100f, 100f, PointerButtons.TOUCH)
        )
        input.endFrame()
        input.onPointer(
            PointerEvent(11, PointerEventType.DOWN, 2f, 2f, 100f, 100f, PointerButtons.TOUCH)
        )
        input.onPointer(
            PointerEvent(10, PointerEventType.UP, 1f, 1f, 100f, 100f, PointerButtons.TOUCH)
        )

        assertTrue(input.isDown("touch"))
        assertFalse(input.wasReleased("touch"))

        input.onPointer(
            PointerEvent(11, PointerEventType.UP, 2f, 2f, 100f, 100f, PointerButtons.TOUCH)
        )
        assertFalse(input.isDown("touch"))
        assertTrue(input.wasReleased("touch"))
    }

    @Test
    fun pointerCancelReleasesHeldButtonsAndTracksMovement() {
        val input = InputActionMap()
        input.bindPointerButton("touch", PointerButtons.TOUCH)
        input.onPointer(
            PointerEvent(3, PointerEventType.DOWN, 4f, 5f, 100f, 100f, PointerButtons.TOUCH)
        )
        input.onPointer(
            PointerEvent(3, PointerEventType.MOVE, 9f, 12f, 100f, 100f)
        )

        assertTrue(input.isDown("touch"))
        assertTrue(input.pointerDeltaX == 5f)
        assertTrue(input.pointerDeltaY == 7f)

        input.onPointer(
            PointerEvent(3, PointerEventType.CANCEL, 9f, 12f, 100f, 100f)
        )
        assertFalse(input.isDown("touch"))
        assertTrue(input.wasReleased("touch"))
    }

    @Test
    fun resetClearsHeldKeysAndEdges() {
        val input = InputActionMap()
        input.bindKey("fire", 5)
        input.onKey(5, true)

        input.reset()

        assertFalse(input.isDown("fire"))
        assertFalse(input.wasPressed("fire"))
        assertFalse(input.wasReleased("fire"))
    }
}
