package com.rift.engine.audio

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity

/**
 * ID-based audio resource manager. Playback is delegated to the active host,
 * while game code uses the same sound/music API on Android and Windows.
 */
class AudioManager(
    private val audioLoader: AudioLoader = AudioLoader { _, _ -> null },
    private val diagnostics: DiagnosticReporter = DiagnosticReporter()
) : AutoCloseable {
    private data class LoadedAudio(
        val path: String,
        val resource: AudioResource
    )

    private val sounds = linkedMapOf<String, LoadedAudio>()
    private val music = linkedMapOf<String, LoadedAudio>()
    private var currentMusicId: String? = null
    private var closed = false

    @Synchronized
    fun loadSound(id: String, path: String): AudioResource? =
        load(id, path, streaming = false, destination = sounds)

    @Synchronized
    fun loadMusic(id: String, path: String): AudioResource? =
        load(id, path, streaming = true, destination = music)

    @Synchronized
    fun playSound(id: String): Boolean {
        check(!closed) { "AudioManager is closed." }
        val resource = sounds[id]?.resource ?: return false
        return safePlayback("sound.play", id) {
            resource.play(looping = false)
        }
    }

    @Synchronized
    fun playMusic(id: String, looping: Boolean = true): Boolean {
        check(!closed) { "AudioManager is closed." }
        val resource = music[id]?.resource ?: return false
        if (currentMusicId != id) {
            currentMusicId?.let { music[it]?.resource?.stop() }
        }
        val played = safePlayback("music.play", id) {
            resource.play(looping)
        }
        if (played) currentMusicId = id
        return played
    }

    @Synchronized
    fun pauseMusic(): Boolean {
        val id = currentMusicId ?: return false
        val resource = music[id]?.resource ?: return false
        return safePlayback("music.pause", id) { resource.pause() }
    }

    @Synchronized
    fun resumeMusic(): Boolean {
        val id = currentMusicId ?: return false
        val resource = music[id]?.resource ?: return false
        return safePlayback("music.resume", id) { resource.resume() }
    }

    @Synchronized
    fun stopMusic(): Boolean {
        val id = currentMusicId ?: return false
        val resource = music[id]?.resource ?: return false
        val stopped = safePlayback("music.stop", id) { resource.stop() }
        currentMusicId = null
        return stopped
    }

    @Synchronized
    fun setMusicVolume(volume: Float) {
        require(volume.isFinite() && volume in 0f..1f) {
            "Music volume must be between zero and one."
        }
        music.values.forEach { it.resource.volume = volume }
    }

    @Synchronized
    fun setSoundVolume(id: String, volume: Float): Boolean {
        require(volume.isFinite() && volume in 0f..1f) {
            "Sound volume must be between zero and one."
        }
        val resource = sounds[id]?.resource ?: return false
        resource.volume = volume
        return true
    }

    @Synchronized
    fun unloadSound(id: String): Boolean = unload(sounds, id)

    @Synchronized
    fun unloadMusic(id: String): Boolean {
        if (currentMusicId == id) currentMusicId = null
        return unload(music, id)
    }

    @Synchronized
    fun loadedSoundIds(): Set<String> = sounds.keys.toSet()

    @Synchronized
    fun loadedMusicIds(): Set<String> = music.keys.toSet()

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        currentMusicId = null
        (sounds.values + music.values).forEach { loaded ->
            try {
                loaded.resource.close()
            } catch (exception: Exception) {
                diagnostics.reportException("AudioManager.dispose", exception)
            }
        }
        sounds.clear()
        music.clear()
    }

    private fun load(
        id: String,
        path: String,
        streaming: Boolean,
        destination: MutableMap<String, LoadedAudio>
    ): AudioResource? {
        check(!closed) { "AudioManager is closed." }
        if (!validId(id)) {
            report("RIFT_AUDIO_ID_INVALID", "Audio ID must be a stable lowercase identifier.", id)
            return null
        }
        if (!validRelativePath(path)) {
            report("RIFT_AUDIO_PATH_UNSAFE", "Audio path must be a safe project-relative path.", path)
            return null
        }

        val existing = destination[id]
        if (existing != null) {
            if (existing.path != path) {
                report(
                    "RIFT_AUDIO_ID_CONFLICT",
                    "Audio ID '$id' is already loaded from '${existing.path}'.",
                    id
                )
                return null
            }
            return existing.resource
        }

        return try {
            val resource = audioLoader.load(path, streaming)
            if (resource == null) {
                report("RIFT_AUDIO_NOT_FOUND", "Audio '$path' could not be loaded.", path)
                null
            } else {
                destination[id] = LoadedAudio(path, resource)
                resource
            }
        } catch (exception: Exception) {
            diagnostics.report(
                DiagnosticSeverity.ERROR,
                "RIFT_AUDIO_LOAD_FAILED",
                "AudioManager",
                "Audio '$path' could not be decoded.",
                exception.toString(),
                path
            )
            null
        }
    }

    private fun unload(
        destination: MutableMap<String, LoadedAudio>,
        id: String
    ): Boolean {
        val loaded = destination.remove(id) ?: return false
        loaded.resource.close()
        return true
    }

    private inline fun safePlayback(
        subsystem: String,
        id: String,
        block: () -> Unit
    ): Boolean {
        return try {
            block()
            true
        } catch (exception: Exception) {
            diagnostics.report(
                DiagnosticSeverity.ERROR,
                "RIFT_AUDIO_PLAYBACK_FAILED",
                "AudioManager",
                "Audio '$id' failed during $subsystem.",
                exception.toString(),
                id
            )
            false
        }
    }

    private fun report(code: String, message: String, location: String) {
        diagnostics.report(
            DiagnosticSeverity.ERROR,
            code,
            "AudioManager",
            message,
            location = location
        )
    }

    private fun validId(id: String): Boolean =
        id.matches(Regex("^[a-z][a-z0-9._-]{0,127}$"))

    private fun validRelativePath(path: String): Boolean {
        if (path.isBlank()) return false
        val normalized = path.replace('\\', '/')
        if (normalized.startsWith("/") || Regex("^[A-Za-z]:").containsMatchIn(normalized)) {
            return false
        }
        return normalized.split('/').none { it.isBlank() || it == "." || it == ".." }
    }
}
