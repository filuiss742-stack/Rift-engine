package com.rift.engine.android

import android.content.Context
import android.media.MediaPlayer
import com.rift.engine.audio.AudioLoader
import com.rift.engine.audio.AudioResource
import java.io.File
import java.io.FileOutputStream

private class AndroidAudioResource(
    context: Context,
    path: String
) : AudioResource {
    private val temporaryFile: File
    private val player: MediaPlayer = MediaPlayer()
    private var closed = false

    override var volume: Float = 1f
        set(value) {
            require(value.isFinite() && value in 0f..1f) {
                "Audio volume must be between zero and one."
            }
            field = value
            if (!closed) player.setVolume(value, value)
        }

    init {
        temporaryFile = File.createTempFile("rift-audio-", ".media", context.cacheDir)
        try {
            context.assets.open(path).use { input ->
                FileOutputStream(temporaryFile).use { output -> input.copyTo(output) }
            }
            player.setDataSource(temporaryFile.absolutePath)
            player.prepare()
            player.setVolume(volume, volume)
        } catch (exception: Exception) {
            player.release()
            temporaryFile.delete()
            throw exception
        }
    }

    override val isPlaying: Boolean
        get() = !closed && runCatching { player.isPlaying }.getOrDefault(false)

    override fun play(looping: Boolean) {
        check(!closed) { "Audio resource is closed." }
        player.isLooping = looping
        player.seekTo(0)
        player.start()
    }

    override fun pause() {
        if (isPlaying) player.pause()
    }

    override fun resume() {
        check(!closed) { "Audio resource is closed." }
        if (!player.isPlaying) player.start()
    }

    override fun stop() {
        if (closed) return
        if (player.isPlaying) player.pause()
        player.seekTo(0)
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        player.release()
        temporaryFile.delete()
    }
}

/** Loads packaged Android audio assets through a temporary cache file. */
class AndroidAudioLoader(context: Context) : AudioLoader {
    private val applicationContext = context.applicationContext

    override fun load(path: String, streaming: Boolean): AudioResource {
        // MediaPlayer handles both short effects and music in this first backend.
        return AndroidAudioResource(applicationContext, path)
    }
}
