package com.rift.engine.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {
    @Test
    fun storesTypedValuesAndReturnsDefaultsForMismatchedTypes() {
        val state = GameState()
        state.putBoolean("quest.done", true)
        state.putInt("coins", 12)
        state.putFloat("player.speed", 125.5f)
        state.putString("chapter", "forest")

        assertTrue(state.boolean("quest.done"))
        assertEquals(12, state.int("coins"))
        assertEquals(125.5f, state.float("player.speed"), 0.001f)
        assertEquals("forest", state.string("chapter"))
        assertEquals(0, state.int("quest.done"))
    }

    @Test
    fun snapshotIsIndependentAndRemoveWorks() {
        val state = GameState()
        state.putInt("coins", 5)
        val snapshot = state.snapshot()
        state.putInt("coins", 8)

        assertEquals(5, snapshot["coins"])
        assertTrue(state.remove("coins"))
        assertFalse(state.contains("coins"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsKeysThatTheSaveCodecWouldRefuse() {
        GameState().putInt("k".repeat(4097), 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMoreEntriesThanTheSaveCodecCanLoad() {
        val state = GameState()
        repeat(10_001) { index ->
            state.putInt("entry.$index", index)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteFloatValues() {
        GameState().putFloat("bad", Float.NaN)
    }
}
