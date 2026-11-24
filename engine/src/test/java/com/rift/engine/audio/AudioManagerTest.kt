package com.rift.engine.audio

import com.rift.engine.diagnostics.DiagnosticReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioManagerTest {
    private class FakeAudio : AudioResource {
        override var volume: Float = 1f
        var plays = 0
        var pauses = 0
        var resumes = 0
        var stops = 0
        var closes = 0
        override val isPlaying: Boolean
            get() = plays > pauses + stops

        override fun play(looping: Boolean) {
            plays += 1
        }

        override fun pause() {
            pauses += 1
        }

        override fun resume() {
            resumes += 1
        }

        override fun stop() {
            stops += 1
        }

        override fun close() {
            closes += 1
        }
    }


    @Test
    fun failedAudioDisposalIsReportedAndVolumeFailuresAreContained() {
        val diagnostics = DiagnosticReporter()
        val audio = object : FakeAudio() {
            override fun close() {
                throw IllegalStateException("native audio release failed")
            }

            override var volume: Float
                get() = super.volume
                set(_) {
                    throw IllegalStateException("volume control failed")
                }
        }
        val manager = AudioManager(AudioLoader { _, _ -> audio }, diagnostics)
        manager.loadSound("click", "click.wav")

        assertFalse(manager.setSoundVolume("click", 0.5f))
        assertTrue(manager.unloadSound("click"))
        assertEquals(2, diagnostics.snapshot().size)
        assertTrue(diagnostics.snapshot().any { it.code == "RIFT_AUDIO_RESOURCE_OPERATION_FAILED" })
        assertTrue(diagnostics.snapshot().any { it.code == "RIFT_AUDIO_RESOURCE_DISPOSE_FAILED" })
    }

    @Test
    fun cachesSoundsAndPlaysById() {
        val audio = FakeAudio()
        var loads = 0
        val manager = AudioManager(AudioLoader { _, _ ->
            loads += 1
            audio
        })

        assertTrue(manager.loadSound("click", "audio/click.wav") === audio)
        assertTrue(manager.loadSound("click", "audio/click.wav") === audio)
        assertTrue(manager.playSound("click"))
        assertEquals(1, loads)
        assertEquals(1, audio.plays)
    }

    @Test
    fun switchingMusicStopsPreviousTrackAndSupportsPauseResume() {
        val first = FakeAudio()
        val second = FakeAudio()
        val queue = ArrayDeque(listOf(first, second))
        val manager = AudioManager(AudioLoader { _, _ -> queue.removeFirst() })
        manager.loadMusic("town", "music/town.wav")
        manager.loadMusic("dungeon", "music/dungeon.wav")

        assertTrue(manager.playMusic("town"))
        assertTrue(manager.pauseMusic())
        assertTrue(manager.resumeMusic())
        assertTrue(manager.playMusic("dungeon"))

        assertEquals(1, first.pauses)
        assertEquals(1, first.resumes)
        assertEquals(1, first.stops)
        assertEquals(1, second.plays)
    }

    @Test
    fun volumeAndUnloadAreApplied() {
        val audio = FakeAudio()
        val manager = AudioManager(AudioLoader { _, _ -> audio })
        manager.loadMusic("theme", "theme.wav")
        manager.setMusicVolume(0.4f)
        assertEquals(0.4f, audio.volume, 0.001f)

        assertTrue(manager.unloadMusic("theme"))
        assertEquals(1, audio.closes)
        assertFalse(manager.stopMusic())
    }

    @Test
    fun rejectsUnsafePathAndReportsMissingAudio() {
        val diagnostics = DiagnosticReporter()
        val manager = AudioManager(AudioLoader { _, _ -> null }, diagnostics)

        assertNull(manager.loadSound("bad", "../outside.wav"))
        assertNull(manager.loadSound("missing", "audio/missing.wav"))
        assertEquals("RIFT_AUDIO_NOT_FOUND", diagnostics.latestError()?.code)
    }


    @Test
    fun rejectsAudioHandleAlreadyOwnedByAnotherId() {
        val audio = FakeAudio()
        val manager = AudioManager(AudioLoader { _, _ -> audio })
        manager.loadSound("first", "first.wav")

        assertNull(manager.loadSound("second", "second.wav"))
        assertEquals(0, audio.closes)

        manager.close()
        assertEquals(1, audio.closes)
    }

    @Test
    fun closeReleasesSoundAndMusicResourcesOnce() {
        val sound = FakeAudio()
        val music = FakeAudio()
        val queue = ArrayDeque(listOf(sound, music))
        val manager = AudioManager(AudioLoader { _, _ -> queue.removeFirst() })
        manager.loadSound("click", "click.wav")
        manager.loadMusic("theme", "theme.wav")

        manager.close()
        manager.close()

        assertEquals(1, sound.closes)
        assertEquals(1, music.closes)
    }
}
