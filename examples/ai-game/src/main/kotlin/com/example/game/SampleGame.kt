package com.example.game

import com.rift.engine.game.RiftGame
import com.rift.engine.scene.Scene

class SampleGame : RiftGame {
    override fun createInitialScene(): Scene = WorldScene()
}
