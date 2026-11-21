package com.rift.engine.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GameStateCodecTest {
    @Test
    fun roundTripsAllSupportedPrimitiveTypesAndUnicodeText() {
        val state = GameState().apply {
            putBoolean("quest.done", true)
            putInt("coins", 42)
            putLong("playtime", 9_000_000_000L)
            putFloat("speed", 3.5f)
            putDouble("score", 1234.125)
            putString("chapter", "Лес\n第二章")
        }

        val decoded = GameStateCodec.decode(GameStateCodec.encode(state))

        assertEquals(state.snapshot(), decoded)
    }

    @Test
    fun rejectsDuplicateKeysUnknownTypesAndMalformedBase64() {
        assertThrows(IllegalArgumentException::class.java) {
            GameStateCodec.decode("RIFT_STATE_V1\ns\tYQ\tMQ\ns\tYQ\tMg\n")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GameStateCodec.decode("RIFT_STATE_V1\nx\tYQ\tMQ\n")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GameStateCodec.decode("RIFT_STATE_V1\ns\t%%%\tMQ\n")
        }
    }

    @Test
    fun rejectsUnsupportedHeaderAndOversizedData() {
        assertThrows(IllegalArgumentException::class.java) {
            GameStateCodec.decode("OTHER_FORMAT\n")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GameStateCodec.decode("x".repeat(1_048_577))
        }
    }
}
