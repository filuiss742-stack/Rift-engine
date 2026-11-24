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
        val audio = object : AudioResource {
            override var volume: Float
                get() = 1f
                set(_) {
                    throw IllegalStateException("volume control failed")
                }
            override val isPlaying = false
            override fun play(looping: Boolean) = Unit
            override fun pause() = Unit
            override fun resume() = Unit
            override fun stop() = Unit
            override fun close() {
                throw IllegalStateException("native audio release failed")
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
    fun failedPreviousTrackStopIsDiagnosedAndDoesNotBlockNextTrack() {
        val diagnostics = DiagnosticReporter()
        val previous = object : AudioResource {
            override var volume = 1f
            override val isPlaying = true
            override fun play(looping: Boolean) = Unit
            override fun pause() = Unit
            override fun resume() = Unit
            override fun stop() {
                throw IllegalStateException("stop failed")
            }
            override fun close() = Unit
        }
        val next = FakeAudio()
        val queue = ArrayDeque(listOf(previous, next))
        val manager = AudioManager(AudioLoader { _, _ -> queue.removeFirst() }, diagnostics)
        manager.loadMusic("first", "first.wav")
        manager.loadMusic("second", "second.wav")

        assertTrue(manager.playMusic("first"))
        assertTrue(manager.playMusic("second"))
        assertEquals(1, next.plays)
        assertTrue(diagnostics.snapshot().any { it.code == "RIFT_AUDIO_PLAYBACK_FAILED" })
    }

    @Test
    fun failedStopKeepsCurrentMusicIdSoStopCanBeRetried() {
        val diagnostics = DiagnosticReporter()
        var stopAttempts = 0
        val audio = object : AudioResource {
            override var volume = 1f
            override val isPlaying = true
            override fun play(looping: Boolean) = Unit
            override fun pause() = Unit
            override fun resume() = Unit
            override fun stop() {
                stopAttempts += 1
                if (stopAttempts == 1) error("temporary stop failure")
            }
            override fun close() = Unit
        }
        val manager = AudioManager(AudioLoader { _, _ -> audio }, diagnostics)
        manager.loadMusic("theme", "theme.wav")
        manager.playMusic("theme")

        assertFalse(manager.stopMusic())
        assertTrue(manager.stopMusic())
        assertEquals(2, stopAttempts)
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
