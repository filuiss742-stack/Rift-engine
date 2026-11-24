package com.rift.engine.desktop

import com.rift.engine.audio.AudioLoader
import com.rift.engine.audio.AudioResource
import com.rift.engine.audio.ProceduralMusicAsset
import java.io.BufferedInputStream
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

private class DesktopAudioResource(
    private val clip: Clip,
    private val temporaryFile: File? = null
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
        temporaryFile?.delete()
    }
}

/** Loads WAV assets and creates deterministic PCM music for procedural: paths. */
class DesktopAudioLoader(root: File = File("assets")) : AudioLoader {
    private val canonicalRoot = root.canonicalFile

    override fun load(path: String, streaming: Boolean): AudioResource? {
        var generatedFile: File? = null
        val clip = AudioSystem.getClip()
        try {
            val stream = if (path.startsWith("procedural:")) {
                val world = path.substringAfter("procedural:")
                generatedFile = File.createTempFile("rift-world-music-", ".wav")
                ProceduralMusicAsset.write(world, generatedFile!!)
                AudioSystem.getAudioInputStream(generatedFile!!)
            } else {
                val candidate = File(canonicalRoot, path).canonicalFile
                val rootPrefix = canonicalRoot.path + File.separator
                require(candidate.path.startsWith(rootPrefix) || candidate == canonicalRoot) {
                    "Audio path escapes the configured project root."
                }
                if (candidate.isFile) {
                    AudioSystem.getAudioInputStream(candidate)
                } else {
                    val loader = Thread.currentThread().contextClassLoader ?: javaClass.classLoader
                    val resource = loader.getResourceAsStream(path) ?: return null
                    AudioSystem.getAudioInputStream(BufferedInputStream(resource))
                }
            }
            stream.use { clip.open(it) }
            return DesktopAudioResource(clip, generatedFile)
        } catch (exception: Exception) {
            clip.close()
            generatedFile?.delete()
            throw exception
        }
    }
}
