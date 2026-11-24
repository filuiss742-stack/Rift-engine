package com.example.game

import com.rift.engine.game.RiftGame
import com.rift.engine.scene.Scene

class TemplateGame : RiftGame {
    override fun createInitialScene(): Scene = WorldScene()
}
