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
