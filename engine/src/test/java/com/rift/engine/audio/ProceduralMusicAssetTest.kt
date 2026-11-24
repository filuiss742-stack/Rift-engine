package com.rift.engine.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProceduralMusicAssetTest {
    @Test
    fun writesValidNonSilentPcmForEveryWorld() {
        val directory = createTempDir(prefix = "rift-music-test-")
        try {
            listOf("earth", "mira", "thea").forEach { world ->
                val output = File(directory, "$world.wav")
                ProceduralMusicAsset.write(world, output, 1)

                assertTrue(output.isFile)
                val bytes = output.readBytes()
                assertTrue(bytes.size > 44)
                assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
                assertEquals("WAVE", String(bytes, 8, 4, Charsets.US_ASCII))
                assertEquals("data", String(bytes, 36, 4, Charsets.US_ASCII))
                assertEquals(44 + ProceduralMusicAsset.SAMPLE_RATE * 2, bytes.size)
                assertTrue("The $world score must not be silent.", bytes.drop(44).any { it.toInt() != 0 })
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownWorldIds() {
        ProceduralMusicAsset.write("unknown", File(createTempDir(), "unknown.wav"), 1)
    }
}
