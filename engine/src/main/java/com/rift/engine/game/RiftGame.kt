package com.rift.engine.game

import com.rift.engine.scene.Scene

/**
 * Entry point for a game built with Rift Engine.
 *
 * A platform host owns the runtime and asks the game for its first scene.
 * Game-specific startup and scene construction stay in the game project.
 */
interface RiftGame {
    /** Logical game resolution, independent of the physical device/window size. */
    val viewportWidth: Int get() = 960
    val viewportHeight: Int get() = 600

    fun createInitialScene(): Scene
}
