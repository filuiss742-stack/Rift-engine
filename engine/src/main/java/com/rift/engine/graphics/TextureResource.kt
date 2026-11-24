package com.rift.engine.graphics

/** Platform-owned texture handle exposed through a platform-neutral contract. */
interface TextureResource : AutoCloseable {
    val width: Int
    val height: Int
    val isDisposed: Boolean

    /** Releases the platform bitmap/image exactly once. */
    override fun close()
}

fun interface TextureLoader {
    /** Loads a project-relative texture path, or returns null when it is missing. */
    fun load(path: String): TextureResource?
}
