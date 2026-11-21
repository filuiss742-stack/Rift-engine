package com.rift.engine.audio

/** Platform-owned audio playback handle. Volume is normalized to 0..1. */
interface AudioResource : AutoCloseable {
    val isPlaying: Boolean
    var volume: Float

    fun play(looping: Boolean = false)
    fun pause()
    fun resume()
    fun stop()

    override fun close()
}

fun interface AudioLoader {
    /** Loads a project-relative audio file. streaming is a hint for long-form music. */
    fun load(path: String, streaming: Boolean): AudioResource?
}
