package com.rift.engine.desktop

import com.rift.engine.audio.AudioLoader
import com.rift.engine.audio.AudioResource
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

private class DesktopAudioResource(
    private val clip: Clip
) : AudioResource {
    private var closed = false

    override var volume: Float = 1f
        set(value) {
            require(value.isFinite() && value in 0f..1f) {
                "Audio volume must be between zero and one."
            }
            field = value
            applyVolume()
        }

    override val isPlaying: Boolean
        get() = !closed && clip.isRunning

    override fun play(looping: Boolean) {
        check(!closed) { "Audio resource is closed." }
        clip.stop()
        clip.framePosition = 0
        if (looping) clip.loop(Clip.LOOP_CONTINUOUSLY) else clip.start()
    }

    override fun pause() {
        if (!closed) clip.stop()
    }

    override fun resume() {
        check(!closed) { "Audio resource is closed." }
        if (!clip.isRunning) clip.start()
    }

    override fun stop() {
        if (closed) return
        clip.stop()
        clip.framePosition = 0
    }

    private fun applyVolume() {
        if (closed) return
        val control = runCatching { clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl }
            .getOrNull() ?: return
        val decibels = if (volume <= 0f) control.minimum else 20f * log10(volume)
        control.value = min(control.maximum, max(control.minimum, decibels))
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        clip.stop()
        clip.close()
    }
}

/** Loads WAV/PCM files below the configured project audio root. */
class DesktopAudioLoader(root: File = File("assets")) : AudioLoader {
    private val canonicalRoot = root.canonicalFile

    override fun load(path: String, streaming: Boolean): AudioResource? {
        val candidate = File(canonicalRoot, path).canonicalFile
        val rootPrefix = canonicalRoot.path + File.separator
        require(candidate.path.startsWith(rootPrefix) || candidate == canonicalRoot) {
            "Audio path escapes the configured project root."
        }
        if (!candidate.isFile) return null

        val clip = AudioSystem.getClip()
        try {
            AudioSystem.getAudioInputStream(candidate).use { stream ->
                clip.open(stream)
            }
            return DesktopAudioResource(clip)
        } catch (exception: Exception) {
            clip.close()
            throw exception
        }
    }
}
