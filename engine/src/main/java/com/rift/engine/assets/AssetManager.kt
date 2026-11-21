package com.rift.engine.assets

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity
import com.rift.engine.graphics.TextureLoader
import com.rift.engine.graphics.TextureResource

/**
 * Synchronous, ID-based texture cache. Platform decoding is supplied through
 * TextureLoader so the engine module never imports Android or AWT classes.
 */
class AssetManager(
    private val textureLoader: TextureLoader = TextureLoader { null },
    private val diagnostics: DiagnosticReporter = DiagnosticReporter()
) : AutoCloseable {
    private data class LoadedTexture(
        val path: String,
        val resource: TextureResource
    )

    private val textures = linkedMapOf<String, LoadedTexture>()
    private var closed = false

    @Synchronized
    fun loadTexture(id: String, path: String): TextureResource? {
        check(!closed) { "AssetManager is closed." }
        if (!validId(id)) {
            report("RIFT_ASSET_ID_INVALID", "Texture ID must be a stable lowercase identifier.", id)
            return null
        }
        if (!validRelativePath(path)) {
            report("RIFT_ASSET_PATH_UNSAFE", "Texture path must be a safe project-relative path.", path)
            return null
        }

        val existing = textures[id]
        if (existing != null) {
            if (existing.path != path) {
                report(
                    "RIFT_ASSET_ID_CONFLICT",
                    "Texture ID '$id' is already loaded from '${existing.path}'.",
                    id
                )
                return null
            }
            return existing.resource
        }

        return try {
            val resource = textureLoader.load(path)
            if (resource == null) {
                report("RIFT_ASSET_TEXTURE_NOT_FOUND", "Texture '$path' could not be loaded.", path)
                null
            } else if (resource.width <= 0 || resource.height <= 0 || resource.isDisposed) {
                resource.close()
                report("RIFT_ASSET_TEXTURE_INVALID", "Texture '$path' returned invalid dimensions or was already disposed.", path)
                null
            } else if (textures.values.any { it.resource === resource }) {
                report(
                    "RIFT_ASSET_RESOURCE_REUSED",
                    "Texture loader returned a resource already owned by another asset ID.",
                    path
                )
                null
            } else {
                textures[id] = LoadedTexture(path, resource)
                resource
            }
        } catch (exception: Exception) {
            diagnostics.report(
                DiagnosticSeverity.ERROR,
                "RIFT_ASSET_TEXTURE_LOAD_FAILED",
                "AssetManager",
                "Texture '$path' could not be decoded.",
                exception.toString(),
                path
            )
            null
        }
    }

    @Synchronized
    fun texture(id: String): TextureResource? = textures[id]?.resource

    @Synchronized
    fun unloadTexture(id: String): Boolean {
        val loaded = textures.remove(id) ?: return false
        loaded.resource.close()
        return true
    }

    @Synchronized
    fun loadedTextureIds(): Set<String> = textures.keys.toSet()

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        val disposed = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<TextureResource, Boolean>()
        )
        textures.values.forEach { loaded ->
            if (disposed.add(loaded.resource)) {
                try {
                    loaded.resource.close()
                } catch (exception: Exception) {
                    diagnostics.reportException("AssetManager.dispose", exception)
                }
            }
        }
        textures.clear()
    }

    private fun report(code: String, message: String, location: String) {
        diagnostics.report(
            DiagnosticSeverity.ERROR,
            code,
            "AssetManager",
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
        val segments = normalized.split('/')
        return segments.none { it.isBlank() || it == "." || it == ".." }
    }
}
