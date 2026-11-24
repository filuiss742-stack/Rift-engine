package com.rift.engine.demo

import com.rift.engine.game.RiftGame
import com.rift.engine.scene.Scene

/** Main playable showcase for the current Rift Engine development build. */
class SandboxGame : RiftGame {
    override fun createInitialScene(): Scene = SandboxScene()
}
