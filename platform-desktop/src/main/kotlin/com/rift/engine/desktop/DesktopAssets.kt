package com.rift.engine.desktop

import com.rift.engine.graphics.TextureLoader
import com.rift.engine.graphics.TextureResource
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class DesktopTexture internal constructor(
    internal val image: BufferedImage
) : TextureResource {
    @Volatile
    private var disposed = false

    override val width: Int
        get() = image.width

    override val height: Int
        get() = image.height

    override val isDisposed: Boolean
        get() = disposed

    @Synchronized
    override fun close() {
        if (disposed) return
        disposed = true
        image.flush()
    }
}

/** Loads images below one project asset root and rejects paths escaping it. */
class DesktopTextureLoader(root: File = File("assets")) : TextureLoader {
    private val canonicalRoot = root.canonicalFile

    override fun load(path: String): TextureResource? {
        val candidate = File(canonicalRoot, path).canonicalFile
        val rootPrefix = canonicalRoot.path + File.separator
        require(candidate.path.startsWith(rootPrefix) || candidate == canonicalRoot) {
            "Asset path escapes the configured project root."
        }
        if (!candidate.isFile) return null
        return ImageIO.read(candidate)?.let(::DesktopTexture)
    }
}
