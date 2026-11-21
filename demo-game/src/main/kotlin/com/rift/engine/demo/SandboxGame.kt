package com.rift.engine.demo

import com.rift.engine.game.RiftGame
import com.rift.engine.scene.Scene

/**
 * Example game composition used by the Android and Windows demo apps.
 */
class SandboxGame : RiftGame {
    override fun createInitialScene(): Scene = SandboxScene()
}
