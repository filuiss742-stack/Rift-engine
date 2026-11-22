package com.rift.engine.demo

import com.rift.engine.game.RiftGame
import com.rift.engine.scene.Scene

/** Android game's opening chapter. */
class PrologueGame : RiftGame {
    override fun createInitialScene(): Scene = ModernPrologueScene()
}